/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.logic.model.parser.mf3;

import cz.ad.print3d.aslicer.logic.model.format.mf3.bambu.Mf3BambuConfig;
import cz.ad.print3d.aslicer.logic.model.format.mf3.bambu.Mf3BambuCustomGCode;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3ContentTypes;
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.prusa.Mf3PrusaSettings;
import cz.ad.print3d.aslicer.logic.model.format.mf3.prusa.Mf3PrusaSlicerModelConfig;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationship;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParser;
import cz.ad.print3d.aslicer.logic.model.storage.FileStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Implementation of {@link ModelParser} for 3MF (3D Manufacturing Format) files.
 * This parser is responsible for reading and extracting 3D model data from the ZIP-based 3MF structure.
 */
public class Mf3Parser implements ModelParser<Mf3Model> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mf3Parser.class);

    private static final String DEFAULT_MODEL_ENTRY = "3D/3dmodel.model";
    private static final String ROOT_RELS_ENTRY = "_rels/.rels";
    private static final String CONTENT_TYPES_ENTRY = "[Content_Types].xml";
    private static final String PRUSA_MODEL_CONFIG_ENTRY = "Metadata/Slic3r_PE_model.config";
    private static final String PRUSA_SETTINGS_ENTRY = "Metadata/Slic3r_PE.config";
    private static final String BAMBU_MODEL_CONFIG_ENTRY = "Metadata/model_settings.config";
    private static final String BAMBU_CUSTOM_GCODE_ENTRY = "Metadata/Bambu_Custom_GCode";
    private static final String MAIN_MODEL_REL_TYPE_01 = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/mainmodel";
    private static final String MAIN_MODEL_REL_TYPE_11 = "http://schemas.microsoft.com/3dmanufacturing/2013/11/3dmodel/mainmodel";
    private static final String MAIN_MODEL_REL_TYPE_CORE = "http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel";

    private final FileStorage storage;

    /**
     * Creates a new Mf3Parser with the default file storage.
     */
    public Mf3Parser() {
        this(new FileStorage());
    }

    /**
     * Creates a new Mf3Parser with the specified file storage.
     *
     * @param storage the file storage to use for extracting non-parsed files
     */
    public Mf3Parser(final FileStorage storage) {
        this.storage = storage;
    }

    /**
     * Parses the 3MF content from the given channel.
     * Since 3MF is a ZIP-based format, this method reads all entries from the ZipInputStream.
     * It discovers the main model part through the root relationship file (_rels/.rels).
     *
     * @param channel the input 3MF binary channel
     * @return the parsed Mf3Model
     * @throws IOException if an I/O error occurs during parsing
     */
    @Override
    public Mf3Model parse(final ReadableByteChannel channel) throws IOException {
        LOGGER.info("Starting 3MF package parsing");
        final Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(Channels.newInputStream(channel))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    LOGGER.trace("Reading ZIP entry: {}", entry.getName());
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    zis.transferTo(baos);
                    entries.put(entry.getName(), baos.toByteArray());
                }
                zis.closeEntry();
            }
        }
        LOGGER.debug("Read {} entries from 3MF package", entries.size());

        final Set<String> parsedEntries = new HashSet<>();
        final Mf3Relationships relationships = new Mf3Relationships();
        final Map<String, Mf3Relationships> relationshipParts = new HashMap<>();
        String modelPath = DEFAULT_MODEL_ENTRY;

        // 1. Parse content types if present
        Mf3ContentTypes contentTypes = null;
        final byte[] contentTypesContent = entries.get(CONTENT_TYPES_ENTRY);
        if (contentTypesContent != null) {
            LOGGER.debug("Parsing content types from {}", CONTENT_TYPES_ENTRY);
            parsedEntries.add(CONTENT_TYPES_ENTRY);
            try (InputStream is = new ByteArrayInputStream(contentTypesContent)) {
                contentTypes = parseContentTypesXml(is);
            }
        }

        // 2. Parse all relationship files
        for (final Map.Entry<String, byte[]> entry : entries.entrySet()) {
            final String entryName = entry.getKey();
            if (entryName.endsWith(".rels") && (entryName.equals(ROOT_RELS_ENTRY) || entryName.contains("/_rels/"))) {
                LOGGER.debug("Parsing relationships from {}", entryName);
                parsedEntries.add(entryName);
                try (InputStream is = new ByteArrayInputStream(entry.getValue())) {
                    final Mf3Relationships rels = parseRelsXml(is);
                    if (rels.getRelationships() != null) {
                        relationshipParts.put(entryName, rels);
                        // If it's the root relationships, look for the main model
                        if (ROOT_RELS_ENTRY.equals(entryName)) {
                            for (final Mf3Relationship rel : rels.getRelationships()) {
                                final String type = rel.getType();
                                if (MAIN_MODEL_REL_TYPE_01.equals(type) || MAIN_MODEL_REL_TYPE_11.equals(type) || MAIN_MODEL_REL_TYPE_CORE.equals(type)) {
                                    modelPath = rel.getTarget();
                                    if (modelPath.startsWith("/")) {
                                        modelPath = modelPath.substring(1);
                                    }
                                    LOGGER.debug("Discovered main model path from relationships: {}", modelPath);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Parse model part
        final byte[] modelContent = entries.get(modelPath);
        if (modelContent == null) {
            LOGGER.error("Main model part not found at path: {}", modelPath);
            throw new IOException("Invalid 3MF file: missing main model part at " + modelPath);
        }
        LOGGER.info("Parsing main model part: {}", modelPath);
        parsedEntries.add(modelPath);

        try (InputStream is = new ByteArrayInputStream(modelContent)) {
            // Combine all relationships for the model unmarshaller (it needs them to resolve references if any)
            final Mf3Relationships allRels = new Mf3Relationships();
            relationshipParts.values().forEach(rels -> allRels.getRelationships().addAll(rels.getRelationships()));

            final Mf3Model model = parseModelXml(is, allRels);
            if (model != null) {
                model.setContentTypes(contentTypes);
                relationshipParts.forEach(model::setRelationshipPart);

                // 4. Parse Prusa-specific configuration if present
                final byte[] prusaModelConfigContent = entries.get(PRUSA_MODEL_CONFIG_ENTRY);
                if (prusaModelConfigContent != null) {
                    LOGGER.debug("Parsing Prusa model configuration from {}", PRUSA_MODEL_CONFIG_ENTRY);
                    parsedEntries.add(PRUSA_MODEL_CONFIG_ENTRY);
                    try (InputStream prusaIs = new ByteArrayInputStream(prusaModelConfigContent)) {
                        model.setPrusaSlicerModelConfig(parsePrusaModelConfigXml(prusaIs));
                    }
                }

                final byte[] prusaSettingsContent = entries.get(PRUSA_SETTINGS_ENTRY);
                if (prusaSettingsContent != null) {
                    LOGGER.debug("Parsing Prusa slicer settings from {}", PRUSA_SETTINGS_ENTRY);
                    parsedEntries.add(PRUSA_SETTINGS_ENTRY);
                    final String content = new String(prusaSettingsContent, java.nio.charset.StandardCharsets.UTF_8);
                    model.setPrusaSettings(Mf3PrusaSettings.parse(content));
                }

                // 6. Parse Bambu-specific configuration if present
                final byte[] bambuModelConfigContent = entries.get(BAMBU_MODEL_CONFIG_ENTRY);
                if (bambuModelConfigContent != null) {
                    LOGGER.debug("Parsing Bambu model configuration from {}", BAMBU_MODEL_CONFIG_ENTRY);
                    parsedEntries.add(BAMBU_MODEL_CONFIG_ENTRY);
                    try (InputStream bambuConfigIs = new ByteArrayInputStream(bambuModelConfigContent)) {
                        model.setBambuConfig(parseBambuModelConfigXml(bambuConfigIs));
                    }
                }

                final byte[] bambuCustomGCodeContent = entries.get(BAMBU_CUSTOM_GCODE_ENTRY);
                if (bambuCustomGCodeContent != null) {
                    LOGGER.debug("Parsing Bambu custom G-code from {}", BAMBU_CUSTOM_GCODE_ENTRY);
                    parsedEntries.add(BAMBU_CUSTOM_GCODE_ENTRY);
                    try (InputStream bambuGCodeIs = new ByteArrayInputStream(bambuCustomGCodeContent)) {
                        model.setBambuCustomGCode(parseBambuCustomGCodeJson(bambuGCodeIs));
                    }
                }

                // Extract all non-parsed files into storage
                final Path storagePath = storage.createRandomDirectory();
                LOGGER.info("Extracting non-parsed files to temporary storage: {}", storagePath);
                model.setStoragePath(storagePath);
                for (final Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    final String entryName = entry.getKey();
                    if (!parsedEntries.contains(entryName)) {
                        LOGGER.trace("Extracting non-parsed entry: {}", entryName);
                        try (InputStream entryStream = new ByteArrayInputStream(entry.getValue())) {
                            storage.storeFile(entryStream, entryName, storagePath);
                        }
                    }
                }
            }
            LOGGER.info("Finished 3MF package parsing successfully");
            return model;
        }
    }

    /**
     * Parses the model XML from the given input stream.
     *
     * @param is            the input stream containing the model XML
     * @param relationships the list of discovered relationships
     * @return the parsed Mf3Model
     * @throws IOException if an error occurs during JAXB unmarshalling
     */
    private Mf3Model parseModelXml(final InputStream is, final Mf3Relationships relationships) throws IOException {
        try {
            final JAXBContext context = JAXBContext.newInstance(Mf3Model.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            final SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            final XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            final Mf3NamespaceFilter filter = new Mf3NamespaceFilter(xmlReader);

            final SAXSource source = new SAXSource(filter, new InputSource(is));
            final Mf3Model model = (Mf3Model) unmarshaller.unmarshal(source);
            if (model != null) {
                model.setRelationships(relationships);
            }
            return model;
        } catch (final JAXBException | SAXException | ParserConfigurationException e) {
            throw new IOException("Failed to parse 3MF model XML content", e);
        }
    }

    /**
     * Parses a relationship XML part.
     *
     * @param is the input stream containing the .rels XML
     * @return a list of parsed relationships
     * @throws IOException if an error occurs during JAXB unmarshalling
     */
    private Mf3Relationships parseRelsXml(final InputStream is) throws IOException {
        try {
            final JAXBContext context = JAXBContext.newInstance(Mf3Relationships.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            final SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            final XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            final Mf3NamespaceFilter filter = new Mf3NamespaceFilter(xmlReader);

            final SAXSource source = new SAXSource(filter, new InputSource(is));
            return (Mf3Relationships) unmarshaller.unmarshal(source);
        } catch (final JAXBException | SAXException | ParserConfigurationException e) {
            throw new IOException("Failed to parse relationships XML", e);
        }
    }

    /**
     * Parses the [Content_Types].xml file.
     *
     * @param is the input stream containing the [Content_Types].xml
     * @return the parsed Mf3ContentTypes
     * @throws IOException if an error occurs during JAXB unmarshalling or validation
     */
    private Mf3ContentTypes parseContentTypesXml(final InputStream is) throws IOException {
        try {
            final JAXBContext context = JAXBContext.newInstance(Mf3ContentTypes.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final URL schemaUrl = getClass().getResource("/xsd/opc-contentTypes.xsd");
            if (schemaUrl != null) {
                final Schema schema = sf.newSchema(schemaUrl);
                unmarshaller.setSchema(schema);
            }

            final SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            final XMLReader xmlReader = spf.newSAXParser().getXMLReader();
            final Mf3NamespaceFilter filter = new Mf3NamespaceFilter(xmlReader);

            final SAXSource source = new SAXSource(filter, new InputSource(is));
            return (Mf3ContentTypes) unmarshaller.unmarshal(source);
        } catch (final JAXBException | SAXException | ParserConfigurationException e) {
            throw new IOException("Failed to parse [Content_Types].xml", e);
        }
    }

    /**
     * Parses the Prusa-specific model configuration XML.
     *
     * @param is the input stream containing the XML
     * @return the parsed Mf3PrusaSlicerModelConfig
     * @throws IOException if an I/O error occurs
     */
    private Mf3PrusaSlicerModelConfig parsePrusaModelConfigXml(final InputStream is) throws IOException {
        try {
            final JAXBContext context = JAXBContext.newInstance(Mf3PrusaSlicerModelConfig.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Mf3PrusaSlicerModelConfig) unmarshaller.unmarshal(is);
        } catch (final JAXBException e) {
            throw new IOException("Error parsing Prusa model config XML: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the Bambu model configuration from the XML content.
     *
     * @param is the input stream containing the XML
     * @return the parsed Mf3BambuConfig
     * @throws IOException if an I/O error occurs
     */
    private Mf3BambuConfig parseBambuModelConfigXml(final InputStream is) throws IOException {
        try {
            final JAXBContext context = JAXBContext.newInstance(Mf3BambuConfig.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Mf3BambuConfig) unmarshaller.unmarshal(is);
        } catch (final JAXBException e) {
            throw new IOException("Error parsing Bambu model config XML: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the Bambu custom G-code from the JSON content.
     *
     * @param is the input stream containing the JSON
     * @return the parsed Mf3BambuCustomGCode
     * @throws IOException if an I/O error occurs
     */
    private Mf3BambuCustomGCode parseBambuCustomGCodeJson(final InputStream is) throws IOException {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, Mf3BambuCustomGCode.class);
        } catch (final IOException e) {
            throw new IOException("Error parsing Bambu custom G-code JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Computes the relationship part path for a given part path.
     *
     * @param partPath the path to the part
     * @return the path to the corresponding relationship part
     */
    private String getRelsPathFor(final String partPath) {
        final int lastSlash = partPath.lastIndexOf('/');
        if (lastSlash == -1) {
            return "_rels/" + partPath + ".rels";
        }
        return partPath.substring(0, lastSlash) + "/_rels" + partPath.substring(lastSlash) + ".rels";
    }

    /**
     * XML filter to normalize 3MF and OPC namespaces to single target namespaces.
     * This allows the parser to be independent of the specific schema version used in the file.
     */
    private static class Mf3NamespaceFilter extends XMLFilterImpl {
        private static final String MODEL_TARGET_NAMESPACE = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel";
        private static final String CONTENT_TYPES_TARGET_NAMESPACE = "http://schemas.openxmlformats.org/package/2006/content-types";
        private static final String RELS_TARGET_NAMESPACE = "http://schemas.openxmlformats.org/package/2006/relationships";

        public Mf3NamespaceFilter(final XMLReader parent) {
            super(parent);
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
            super.startElement(normalizeNamespace(uri), localName, qName, atts);
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            super.endElement(normalizeNamespace(uri), localName, qName);
        }

        private String normalizeNamespace(final String uri) {
            if (uri != null) {
                if (uri.contains("3dmanufacturing")) {
                    return MODEL_TARGET_NAMESPACE;
                }
                if (uri.contains("content-types")) {
                    return CONTENT_TYPES_TARGET_NAMESPACE;
                }
                if (uri.contains("relationships")) {
                    return RELS_TARGET_NAMESPACE;
                }
            }
            return uri;
        }
    }
}
