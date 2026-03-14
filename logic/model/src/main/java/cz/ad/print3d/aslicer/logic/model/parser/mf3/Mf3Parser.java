package cz.ad.print3d.aslicer.logic.model.parser.mf3;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.mf3.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.Mf3Object;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import cz.ad.print3d.aslicer.logic.model.format.mf3.Mf3Triangle;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationship;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParser;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Implementation of {@link ModelParser} for 3MF (3D Manufacturing Format) files.
 * This parser is responsible for reading and extracting 3D model data from the ZIP-based 3MF structure.
 */
public class Mf3Parser implements ModelParser<Mf3Model> {

    private static final String DEFAULT_MODEL_ENTRY = "3D/3dmodel.model";
    private static final String ROOT_RELS_ENTRY = "_rels/.rels";
    private static final String MAIN_MODEL_REL_TYPE = "http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel";

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

        final Mf3Relationships relationships = new Mf3Relationships();
        String modelPath = DEFAULT_MODEL_ENTRY;

        // 1. Parse root relationships to find the main model part
        final byte[] rootRelsContent = entries.get(ROOT_RELS_ENTRY);
        if (rootRelsContent != null) {
            try (InputStream is = new ByteArrayInputStream(rootRelsContent)) {
                final Mf3Relationships rootRels = parseRelsXml(is);
                if (rootRels.getRelationships() != null) {
                    relationships.getRelationships().addAll(rootRels.getRelationships());
                    for (final Mf3Relationship rel : rootRels.getRelationships()) {
                        if (MAIN_MODEL_REL_TYPE.equals(rel.getType())) {
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

        // 3. Parse model-specific relationships if they exist
        final String modelRelsPath = getRelsPathFor(modelPath);
        final byte[] modelRelsContent = entries.get(modelRelsPath);
        if (modelRelsContent != null) {
            try (InputStream is = new ByteArrayInputStream(modelRelsContent)) {
                final Mf3Relationships modelRels = parseRelsXml(is);
                if (modelRels.getRelationships() != null) {
                    relationships.getRelationships().addAll(modelRels.getRelationships());
                }
            }
        }

        try (InputStream is = new ByteArrayInputStream(modelContent)) {
            return parseModelXml(is, relationships);
        } catch (XMLStreamException e) {
            throw new IOException("Failed to parse 3MF XML content", e);
        }
    }

    /**
     * Parses the model XML from the given input stream.
     *
     * @param is            the input stream containing the model XML
     * @param relationships the list of discovered relationships
     * @return the parsed Mf3Model
     * @throws XMLStreamException if an error occurs during XML parsing
     */
    private Mf3Model parseModelXml(final InputStream is, final Mf3Relationships relationships) throws XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        // Disable external entities for security
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        final XMLStreamReader reader = factory.createXMLStreamReader(is);
        try {
            final Map<String, String> metadata = new HashMap<>();
            final List<Mf3Object> objects = new ArrayList<>();
            Unit unit = Unit.MILLIMETER;

            while (reader.hasNext()) {
                final int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if ("model".equals(localName)) {
                        final String unitValue = reader.getAttributeValue(null, "unit");
                        if (unitValue != null) {
                            final Unit parsedUnit = Unit.fromString(unitValue);
                            if (parsedUnit != null) {
                                unit = parsedUnit;
                            }
                        }
                    } else if ("metadata".equals(localName)) {
                        final String name = reader.getAttributeValue(null, "name");
                        final String value = reader.getElementText();
                        if (name != null) {
                            metadata.put(name, value);
                        }
                    } else if ("object".equals(localName)) {
                        objects.add(parseObject(reader));
                    }
                }
            }

            return new Mf3Model(metadata, objects, unit, relationships);
        } catch (NumberFormatException e) {
            throw new XMLStreamException("Invalid numeric value in 3MF XML", e);
        } finally {
            reader.close();
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
            return (Mf3Relationships) unmarshaller.unmarshal(is);
        } catch (final JAXBException e) {
            throw new IOException("Failed to parse relationships XML", e);
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
     * Parses an object element from the XML.
     *
     * @param reader the XML stream reader
     * @return the parsed Mf3Object
     * @throws XMLStreamException if an error occurs during XML parsing
     */
    private Mf3Object parseObject(XMLStreamReader reader) throws XMLStreamException {
        int id = Integer.parseInt(reader.getAttributeValue(null, "id"));
        String name = reader.getAttributeValue(null, "name");
        
        List<Vector3f> vertices = new ArrayList<>();
        List<Mf3Triangle> triangles = new ArrayList<>();
        
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("vertex".equals(localName)) {
                    float x = Float.parseFloat(reader.getAttributeValue(null, "x"));
                    float y = Float.parseFloat(reader.getAttributeValue(null, "y"));
                    float z = Float.parseFloat(reader.getAttributeValue(null, "z"));
                    vertices.add(new Vector3f(x, y, z));
                } else if ("triangle".equals(localName)) {
                    int v1 = Integer.parseInt(reader.getAttributeValue(null, "v1"));
                    int v2 = Integer.parseInt(reader.getAttributeValue(null, "v2"));
                    int v3 = Integer.parseInt(reader.getAttributeValue(null, "v3"));
                    triangles.add(new Mf3Triangle(v1, v2, v3));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "object".equals(reader.getLocalName())) {
                break;
            }
        }
        
        return new Mf3Object(id, name, vertices, triangles);
    }
}
