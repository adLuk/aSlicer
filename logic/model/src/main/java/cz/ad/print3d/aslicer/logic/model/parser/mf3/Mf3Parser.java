package cz.ad.print3d.aslicer.logic.model.parser.mf3;

import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3ContentTypes;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationship;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParser;
import cz.ad.print3d.aslicer.logic.model.storage.FileStorage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
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

    private static final String DEFAULT_MODEL_ENTRY = "3D/3dmodel.model";
    private static final String ROOT_RELS_ENTRY = "_rels/.rels";
    private static final String CONTENT_TYPES_ENTRY = "[Content_Types].xml";
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
        final Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(Channels.newInputStream(channel))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    zis.transferTo(baos);
                    entries.put(entry.getName(), baos.toByteArray());
                }
                zis.closeEntry();
            }
        }

        final Set<String> parsedEntries = new HashSet<>();
        final Mf3Relationships relationships = new Mf3Relationships();
        final Map<String, Mf3Relationships> relationshipParts = new HashMap<>();
        String modelPath = DEFAULT_MODEL_ENTRY;

        // 1. Parse content types if present
        Mf3ContentTypes contentTypes = null;
        final byte[] contentTypesContent = entries.get(CONTENT_TYPES_ENTRY);
        if (contentTypesContent != null) {
            parsedEntries.add(CONTENT_TYPES_ENTRY);
            try (InputStream is = new ByteArrayInputStream(contentTypesContent)) {
                contentTypes = parseContentTypesXml(is);
            }
        }

        // 2. Parse root relationships to find the main model part
        final byte[] rootRelsContent = entries.get(ROOT_RELS_ENTRY);
        if (rootRelsContent != null) {
            parsedEntries.add(ROOT_RELS_ENTRY);
            try (InputStream is = new ByteArrayInputStream(rootRelsContent)) {
                final Mf3Relationships rootRels = parseRelsXml(is);
                if (rootRels.getRelationships() != null) {
                    relationshipParts.put(ROOT_RELS_ENTRY, rootRels);
                    for (final Mf3Relationship rel : rootRels.getRelationships()) {
                        final String type = rel.getType();
                        if (MAIN_MODEL_REL_TYPE_01.equals(type) || MAIN_MODEL_REL_TYPE_11.equals(type) || MAIN_MODEL_REL_TYPE_CORE.equals(type)) {
                            modelPath = rel.getTarget();
                            if (modelPath.startsWith("/")) {
                                modelPath = modelPath.substring(1);
                            }
                        }
                    }
                }
            }
        }

        // 2. Parse model part
        final byte[] modelContent = entries.get(modelPath);
        if (modelContent == null) {
            throw new IOException("Invalid 3MF file: missing main model part at " + modelPath);
        }
        parsedEntries.add(modelPath);

        // 3. Parse model-specific relationships if they exist
        final String modelRelsPath = getRelsPathFor(modelPath);
        final byte[] modelRelsContent = entries.get(modelRelsPath);
        if (modelRelsContent != null) {
            parsedEntries.add(modelRelsPath);
            try (InputStream is = new ByteArrayInputStream(modelRelsContent)) {
                final Mf3Relationships modelRels = parseRelsXml(is);
                if (modelRels.getRelationships() != null) {
                    relationshipParts.put(modelRelsPath, modelRels);
                }
            }
        }

        try (InputStream is = new ByteArrayInputStream(modelContent)) {
            // Combine all relationships for the model unmarshaller (it needs them to resolve references if any)
            final Mf3Relationships allRels = new Mf3Relationships();
            relationshipParts.values().forEach(rels -> allRels.getRelationships().addAll(rels.getRelationships()));

            final Mf3Model model = parseModelXml(is, allRels);
            if (model != null) {
                model.setContentTypes(contentTypes);
                relationshipParts.forEach(model::setRelationshipPart);

                // Extract all non-parsed files into storage
                final Path storagePath = storage.createRandomDirectory();
                model.setStoragePath(storagePath);
                for (final Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    final String entryName = entry.getKey();
                    if (!parsedEntries.contains(entryName)) {
                        try (InputStream entryStream = new ByteArrayInputStream(entry.getValue())) {
                            storage.storeFile(entryStream, entryName, storagePath);
                        }
                    }
                }
            }
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
