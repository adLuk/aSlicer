package cz.ad.print3d.aslicer.logic.model.parser.mf3;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.mf3.ThreeMfModel;
import cz.ad.print3d.aslicer.logic.model.format.mf3.ThreeMfObject;
import cz.ad.print3d.aslicer.logic.model.format.mf3.ThreeMfTriangle;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParser;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
public class ThreeMfParser implements ModelParser<ThreeMfModel> {

    private static final String MODEL_ENTRY = "3D/3dmodel.model";

    /**
     * Parses the 3MF content from the given channel.
     * Since 3MF is a ZIP-based format, this method wraps the channel in a ZipInputStream
     * to extract and parse the core model XML data.
     *
     * @param channel the input 3MF binary channel
     * @return the parsed ThreeMfModel
     * @throws IOException if an I/O error occurs during parsing
     */
    @Override
    public ThreeMfModel parse(ReadableByteChannel channel) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Channels.newInputStream(channel))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (MODEL_ENTRY.equalsIgnoreCase(entry.getName())) {
                    return parseModelXml(zis);
                }
                zis.closeEntry();
            }
        } catch (XMLStreamException e) {
            throw new IOException("Failed to parse 3MF XML content", e);
        }
        throw new IOException("Invalid 3MF file: missing " + MODEL_ENTRY);
    }

    /**
     * Parses the model XML from the given input stream.
     *
     * @param is the input stream containing the model XML
     * @return the parsed ThreeMfModel
     * @throws XMLStreamException if an error occurs during XML parsing
     */
    private ThreeMfModel parseModelXml(InputStream is) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Disable external entities for security
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        
        XMLStreamReader reader = factory.createXMLStreamReader(is);
        try {
            Map<String, String> metadata = new HashMap<>();
            List<ThreeMfObject> objects = new ArrayList<>();
            Unit unit = Unit.MILLIMETER;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    if ("model".equals(localName)) {
                        String unitValue = reader.getAttributeValue(null, "unit");
                        if (unitValue != null) {
                            Unit parsedUnit = Unit.fromString(unitValue);
                            if (parsedUnit != null) {
                                unit = parsedUnit;
                            }
                        }
                    } else if ("metadata".equals(localName)) {
                        String name = reader.getAttributeValue(null, "name");
                        String value = reader.getElementText();
                        if (name != null) {
                            metadata.put(name, value);
                        }
                    } else if ("object".equals(localName)) {
                        objects.add(parseObject(reader));
                    }
                }
            }

            return new ThreeMfModel(metadata, objects, unit);
        } catch (NumberFormatException e) {
            throw new XMLStreamException("Invalid numeric value in 3MF XML", e);
        } finally {
            reader.close();
        }
    }

    /**
     * Parses an object element from the XML.
     *
     * @param reader the XML stream reader
     * @return the parsed ThreeMfObject
     * @throws XMLStreamException if an error occurs during XML parsing
     */
    private ThreeMfObject parseObject(XMLStreamReader reader) throws XMLStreamException {
        int id = Integer.parseInt(reader.getAttributeValue(null, "id"));
        String name = reader.getAttributeValue(null, "name");
        
        List<Vector3f> vertices = new ArrayList<>();
        List<ThreeMfTriangle> triangles = new ArrayList<>();
        
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
                    triangles.add(new ThreeMfTriangle(v1, v2, v3));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "object".equals(reader.getLocalName())) {
                break;
            }
        }
        
        return new ThreeMfObject(id, name, vertices, triangles);
    }
}
