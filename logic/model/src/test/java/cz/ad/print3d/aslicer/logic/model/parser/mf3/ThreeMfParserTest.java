package cz.ad.print3d.aslicer.logic.model.parser.mf3;

import cz.ad.print3d.aslicer.logic.model.format.mf3.ThreeMfModel;
import cz.ad.print3d.aslicer.logic.model.format.mf3.ThreeMfObject;
import cz.ad.print3d.aslicer.logic.model.format.mf3.ThreeMfTriangle;
import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ThreeMfParser}.
 */
public class ThreeMfParserTest {

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
        
        ThreeMfParser parser = new ThreeMfParser();
        ThreeMfModel model = parser.parse(channel);
        
        assertNotNull(model);
        assertEquals(Unit.MILLIMETER, model.unit());
        assertEquals("Test Cube", model.metadata().get("Title"));
        assertEquals(1, model.objects().size());

        ThreeMfObject object = model.objects().get(0);
        assertEquals(1, object.id());
        assertEquals("Cube", object.name());
        assertEquals(3, object.vertices().size());
        assertEquals(1, object.triangles().size());

        ThreeMfTriangle triangle = object.triangles().get(0);
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
        ThreeMfParser parser = new ThreeMfParser();
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
        
        ThreeMfParser parser = new ThreeMfParser();
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
        
        ThreeMfParser parser = new ThreeMfParser();
        ThreeMfModel model = parser.parse(channel);

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

        ThreeMfParser parser = new ThreeMfParser();

        for (Unit unit : Unit.values()) {
            String xmlContent = String.format(xmlTemplate, unit.getValue());
            byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
            ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
            
            ThreeMfModel model = parser.parse(channel);
            assertEquals(unit, model.unit(), "Failed to parse unit: " + unit.getValue());
        }
    }

    private byte[] createZipWithFile(String path, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(path);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
