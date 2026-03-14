package cz.ad.print3d.aslicer.logic.model.parser.mf3;

import cz.ad.print3d.aslicer.logic.model.format.mf3.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.Mf3Object;
import cz.ad.print3d.aslicer.logic.model.format.mf3.Mf3Triangle;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationship;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Mf3Parser}.
 */
public class Mf3ParserTest {

    /**
     * Verifies that the parser correctly parses a 3MF file with metadata, objects, vertices, and triangles.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseValid3Mf() throws IOException {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model unit=\"millimeter\" xml:lang=\"en-US\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n" +
                "  <metadata name=\"Title\">Test Cube</metadata>\n" +
                "  <resources>\n" +
                "    <object id=\"1\" name=\"Cube\">\n" +
                "      <mesh>\n" +
                "        <vertices>\n" +
                "          <vertex x=\"0.0\" y=\"0.0\" z=\"0.0\" />\n" +
                "          <vertex x=\"10.0\" y=\"0.0\" z=\"0.0\" />\n" +
                "          <vertex x=\"10.0\" y=\"10.0\" z=\"0.0\" />\n" +
                "        </vertices>\n" +
                "        <triangles>\n" +
                "          <triangle v1=\"0\" v2=\"1\" v3=\"2\" />\n" +
                "        </triangles>\n" +
                "      </mesh>\n" +
                "    </object>\n" +
                "  </resources>\n" +
                "</model>";

        byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
        
        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);
        
        assertNotNull(model);
        assertEquals(Unit.MILLIMETER, model.unit());
        assertEquals("Test Cube", model.metadata().get("Title"));
        assertEquals(1, model.objects().size());

        Mf3Object object = model.objects().get(0);
        assertEquals(1, object.id());
        assertEquals("Cube", object.name());
        assertEquals(3, object.vertices().size());
        assertEquals(1, object.triangles().size());

        Mf3Triangle triangle = object.triangles().get(0);
        assertEquals(0, triangle.v1());
        assertEquals(1, triangle.v2());
        assertEquals(2, triangle.v3());
        
        Vector3f v1 = object.vertices().get(0);
        assertEquals(0.0f, v1.x());
        assertEquals(0.0f, v1.y());
        assertEquals(0.0f, v1.z());
    }

    /**
     * Verifies that the parser correctly handles empty input.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseEmpty() throws IOException {
        Mf3Parser parser = new Mf3Parser();
        byte[] data = new byte[0];
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(data));
        
        assertThrows(IOException.class, () -> parser.parse(channel));
    }

    /**
     * Verifies that the parser throws an exception when the core model file is missing.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseMissingModelFile() throws IOException {
        byte[] zipData = createZipWithFile("not-a-model.txt", "some content");
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
        
        Mf3Parser parser = new Mf3Parser();
        assertThrows(IOException.class, () -> parser.parse(channel), "Invalid 3MF file: missing 3D/3dmodel.model");
    }

    /**
     * Verifies that the parser correctly handles multiple objects.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseMultipleObjects() throws IOException {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n" +
                "  <resources>\n" +
                "    <object id=\"1\" name=\"Obj1\"><mesh><vertices><vertex x=\"0\" y=\"0\" z=\"0\"/></vertices><triangles/></mesh></object>\n" +
                "    <object id=\"2\" name=\"Obj2\"><mesh><vertices><vertex x=\"1\" y=\"1\" z=\"1\"/></vertices><triangles/></mesh></object>\n" +
                "  </resources>\n" +
                "</model>";

        byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
        
        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);

        assertEquals(Unit.MILLIMETER, model.unit());
        assertEquals(2, model.objects().size());
        assertEquals("Obj1", model.objects().get(0).name());
        assertEquals("Obj2", model.objects().get(1).name());
    }

    /**
     * Verifies that the parser correctly handles different measurement units.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseUnits() throws IOException {
        String xmlTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model unit=\"%s\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n" +
                "  <resources />\n" +
                "</model>";

        Mf3Parser parser = new Mf3Parser();

        for (Unit unit : Unit.values()) {
            String xmlContent = String.format(xmlTemplate, unit.getValue());
            byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
            ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
            
            Mf3Model model = parser.parse(channel);
            assertEquals(unit, model.unit(), "Failed to parse unit: " + unit.getValue());
        }
    }

    /**
     * Verifies that the parser correctly handles 3MF files with root relationships.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseWithRels() throws IOException {
        String relsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rel1\" Type=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel\" Target=\"/models/my-model.model\"/>\n" +
                "  <Relationship Id=\"rel2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata\" Target=\"/metadata.xml\"/>\n" +
                "</Relationships>";
        
        String modelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n" +
                "  <resources />\n" +
                "</model>";

        Map<String, String> files = new HashMap<>();
        files.put("_rels/.rels", relsContent);
        files.put("models/my-model.model", modelContent);

        byte[] zipData = createZipWithFiles(files);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
        
        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);
        
        assertNotNull(model);
        assertEquals(2, model.relationships().getRelationships().size());
        
        Mf3Relationship rel1 = model.relationships().getRelationships().stream()
                .filter(r -> "rel1".equals(r.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel", rel1.getType());
        assertEquals("/models/my-model.model", rel1.getTarget());
    }

    /**
     * Verifies that the parser correctly handles model-specific relationships.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseWithModelRels() throws IOException {
        String rootRelsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rel1\" Type=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel\" Target=\"/3D/model.model\"/>\n" +
                "</Relationships>";
        
        String modelPath = "3D/model.model";
        String modelRelsPath = "3D/_rels/model.model.rels";
        
        String modelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n" +
                "  <resources />\n" +
                "</model>";
        
        String modelRelsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rel2\" Type=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02/texture\" Target=\"/textures/tex1.png\"/>\n" +
                "</Relationships>";

        Map<String, String> files = new HashMap<>();
        files.put("_rels/.rels", rootRelsContent);
        files.put(modelPath, modelContent);
        files.put(modelRelsPath, modelRelsContent);

        byte[] zipData = createZipWithFiles(files);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
        
        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);
        
        assertNotNull(model);
        assertEquals(2, model.relationships().getRelationships().size());
        assertTrue(model.relationships().getRelationships().stream().anyMatch(r -> "rel1".equals(r.getId())));
        assertTrue(model.relationships().getRelationships().stream().anyMatch(r -> "rel2".equals(r.getId())));
    }

    /**
     * Verifies that the parser correctly parses a 3MF file from resources.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseFromResource() throws IOException {
        String resourcePath = "/3mf/test-simple.3mf";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            ReadableByteChannel channel = Channels.newChannel(is);
            
            Mf3Parser parser = new Mf3Parser();
            Mf3Model model = parser.parse(channel);
            
            assertNotNull(model);
            assertEquals(Unit.MILLIMETER, model.unit());
            assertEquals(1, model.objects().size());
            
            Mf3Object object = model.objects().get(0);
            assertEquals(1, object.id());
            assertFalse(object.vertices().isEmpty());
            
            // Relationships
            assertFalse(model.relationships().getRelationships().isEmpty());
            assertTrue(model.relationships().getRelationships().stream().anyMatch(r -> "rel0".equals(r.getId())));
        }
    }

    private byte[] createZipWithFile(String path, String content) throws IOException {
        Map<String, String> files = new HashMap<>();
        files.put(path, content);
        return createZipWithFiles(files);
    }

    private byte[] createZipWithFiles(Map<String, String> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                zos.putNextEntry(entry);
                zos.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
