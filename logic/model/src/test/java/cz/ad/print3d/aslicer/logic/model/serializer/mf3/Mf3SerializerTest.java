package cz.ad.print3d.aslicer.logic.model.serializer.mf3;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import cz.ad.print3d.aslicer.logic.model.parser.mf3.Mf3Parser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Mf3Serializer}.
 */
public class Mf3SerializerTest {

    /**
     * Verifies that the serializer can handle a Mf3Model.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testSerialize() throws IOException {
        Mf3Serializer serializer = new Mf3Serializer();
        Mf3Model model = new Mf3Model(Collections.emptyMap(), Collections.emptyList(), Unit.MILLIMETER, new Mf3Relationships());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel(outputStream);
        
        serializer.serialize(model, channel);
        
        byte[] bytes = outputStream.toByteArray();
        assertTrue(bytes.length > 0);
        
        Map<String, byte[]> entries = getZipEntries(bytes);
        assertTrue(entries.containsKey("[Content_Types].xml"));
        assertTrue(entries.containsKey("_rels/.rels"));
        assertTrue(entries.containsKey("3D/3dmodel.model"));
    }

    @Test
    public void testRoundTripWithSimpleModel() throws IOException {
        Mf3Parser parser = new Mf3Parser();
        Mf3Serializer serializer = new Mf3Serializer();

        // 1. Parse the original file
        InputStream is = getClass().getResourceAsStream("/3mf/test-simple.3mf");
        assertNotNull(is, "Resource test-simple.3mf not found");
        byte[] originalBytes = is.readAllBytes();
        Mf3Model model = parser.parse(Channels.newChannel(new ByteArrayInputStream(originalBytes)));
        assertNotNull(model);

        // 2. Serialize back to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(model, Channels.newChannel(baos));
        byte[] serializedBytes = baos.toByteArray();

        // 3. Parse the serialized bytes
        Mf3Model reParsedModel = parser.parse(Channels.newChannel(new ByteArrayInputStream(serializedBytes)));
        assertNotNull(reParsedModel);

        // 4. Compare logical models
        assertEquals(model.unit(), reParsedModel.unit());
        assertEquals(model.metadata(), reParsedModel.metadata());
        assertEquals(model.getResources(), reParsedModel.getResources());
        assertEquals(model.getBuild(), reParsedModel.getBuild());
        
        // Relationships should also match logically
        assertEquals(model.relationshipParts(), reParsedModel.relationshipParts());
        
        // 5. Compare ZIP entries to ensure all files from storage are included
        Map<String, byte[]> originalEntries = getZipEntries(originalBytes);
        Map<String, byte[]> serializedEntries = getZipEntries(serializedBytes);

        for (String entryName : originalEntries.keySet()) {
            if (entryName.endsWith(".model") || entryName.endsWith(".rels") || entryName.equals("[Content_Types].xml")) {
                continue;
            }
            if (entryName.endsWith("/")) {
                continue;
            }
            assertTrue(serializedEntries.containsKey(entryName), "Serialized ZIP missing entry: " + entryName);
        }
    }

    @Test
    public void testRoundTripWithComplexRelationships() throws IOException {
        Mf3Parser parser = new Mf3Parser();
        Mf3Serializer serializer = new Mf3Serializer();

        String rootRelsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rel1\" Type=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/mainmodel\" Target=\"/3D/model.model\"/>\n" +
                "  <Relationship Id=\"thumb\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/thumbnail\" Target=\"/Metadata/thumbnail.png\"/>\n" +
                "</Relationships>";

        String modelPath = "3D/model.model";
        String modelRelsPath = "3D/_rels/model.model.rels";

        String modelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\" unit=\"millimeter\">\n" +
                "  <resources />\n" +
                "  <build />\n" +
                "</model>";

        String modelRelsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rel2\" Type=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/texture\" Target=\"/textures/tex1.png\"/>\n" +
                "</Relationships>";

        byte[] thumbnailData = new byte[]{1, 2, 3, 4};
        byte[] textureData = new byte[]{5, 6, 7, 8};

        // Create initial ZIP
        ByteArrayOutputStream baosInitial = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baosInitial)) {
            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            zos.write(rootRelsContent.getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(modelPath));
            zos.write(modelContent.getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(modelRelsPath));
            zos.write(modelRelsContent.getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("Metadata/thumbnail.png"));
            zos.write(thumbnailData);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("textures/tex1.png"));
            zos.write(textureData);
            zos.closeEntry();
        }
        byte[] originalZipData = baosInitial.toByteArray();

        // 1. Parse
        Mf3Model model = parser.parse(Channels.newChannel(new ByteArrayInputStream(originalZipData)));
        assertNotNull(model);
        assertEquals(2, model.relationshipParts().size());
        assertTrue(model.relationshipParts().containsKey("_rels/.rels"));
        assertTrue(model.relationshipParts().containsKey(modelRelsPath));

        // 2. Serialize
        ByteArrayOutputStream baosFinal = new ByteArrayOutputStream();
        serializer.serialize(model, Channels.newChannel(baosFinal));
        byte[] finalZipData = baosFinal.toByteArray();

        // 3. Compare ZIP contents
        Map<String, byte[]> finalEntries = getZipEntries(finalZipData);
        assertTrue(finalEntries.containsKey("_rels/.rels"));
        assertTrue(finalEntries.containsKey(modelPath));
        assertTrue(finalEntries.containsKey(modelRelsPath));
        assertTrue(finalEntries.containsKey("Metadata/thumbnail.png"));
        assertTrue(finalEntries.containsKey("textures/tex1.png"));

        assertArrayEquals(thumbnailData, finalEntries.get("Metadata/thumbnail.png"));
        assertArrayEquals(textureData, finalEntries.get("textures/tex1.png"));

        // Verify root rels contain both relationships
        String finalRootRels = new String(finalEntries.get("_rels/.rels"));
        assertTrue(finalRootRels.contains("rel1"));
        assertTrue(finalRootRels.contains("thumb"));

        // 4. Re-parse and compare
        Mf3Model reParsedModel = parser.parse(Channels.newChannel(new ByteArrayInputStream(finalZipData)));
        assertEquals(model.relationshipParts(), reParsedModel.relationshipParts());
    }

    private Map<String, byte[]> getZipEntries(byte[] data) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
        return entries;
    }
}
