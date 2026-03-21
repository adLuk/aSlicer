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

import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3BaseMaterials;
import cz.ad.print3d.aslicer.logic.model.format.mf3.resource.Mf3Object;
import cz.ad.print3d.aslicer.logic.model.format.mf3.geometry.Mf3Triangle;
import cz.ad.print3d.aslicer.logic.model.format.mf3.contenttype.Mf3ContentTypes;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationship;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import cz.ad.print3d.aslicer.logic.model.format.mf3.prusa.Mf3PrusaSettings;
import cz.ad.print3d.aslicer.logic.model.format.mf3.prusa.Mf3PrusaSlicerModelConfig;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
                "<model unit=\"millimeter\" xml:lang=\"en-US\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
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
        assertEquals(LengthUnit.MILLIMETER, model.lengthUnit());
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
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources>\n" +
                "    <object id=\"1\" name=\"Obj1\"><mesh><vertices><vertex x=\"0\" y=\"0\" z=\"0\"/></vertices><triangles/></mesh></object>\n" +
                "    <object id=\"2\" name=\"Obj2\"><mesh><vertices><vertex x=\"1\" y=\"1\" z=\"1\"/></vertices><triangles/></mesh></object>\n" +
                "  </resources>\n" +
                "</model>";

        byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
        
        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);

        assertEquals(LengthUnit.MILLIMETER, model.lengthUnit());
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
                "<model unit=\"%s\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources />\n" +
                "</model>";

        Mf3Parser parser = new Mf3Parser();

        for (LengthUnit lengthUnit : LengthUnit.values()) {
            String xmlContent = String.format(xmlTemplate, lengthUnit.getValue());
            byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
            ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));
            
            Mf3Model model = parser.parse(channel);
            assertEquals(lengthUnit, model.lengthUnit(), "Failed to parse unit: " + lengthUnit.getValue());
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
                "  <Relationship Id=\"rel1\" Type=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/mainmodel\" Target=\"/models/my-model.model\"/>\n" +
                "  <Relationship Id=\"rel2\" Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata\" Target=\"/metadata.xml\"/>\n" +
                "</Relationships>";
        
        String modelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
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
        assertEquals("http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/mainmodel", rel1.getType());
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
                "  <Relationship Id=\"rel1\" Type=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/mainmodel\" Target=\"/3D/model.model\"/>\n" +
                "</Relationships>";
        
        String modelPath = "3D/model.model";
        String modelRelsPath = "3D/_rels/model.model.rels";
        
        String modelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources />\n" +
                "</model>";
        
        String modelRelsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rel2\" Type=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel/texture\" Target=\"/textures/tex1.png\"/>\n" +
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
        assertEquals(1, model.relationshipParts().get("_rels/.rels").getRelationships().size());
        assertEquals(1, model.relationshipParts().get(modelRelsPath).getRelationships().size());
        assertTrue(model.relationshipParts().get("_rels/.rels").getRelationships().stream().anyMatch(r -> "rel1".equals(r.getId())));
        assertTrue(model.relationshipParts().get(modelRelsPath).getRelationships().stream().anyMatch(r -> "rel2".equals(r.getId())));
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
            assertEquals(LengthUnit.MILLIMETER, model.lengthUnit());
            assertEquals(1, model.objects().size());
            
            Mf3Object object = model.objects().get(0);
            assertEquals(1, object.id());
            assertFalse(object.vertices().isEmpty());
            
            // Relationships
            assertFalse(model.relationshipParts().isEmpty());
            Mf3Relationships rootRels = model.relationshipParts().get("_rels/.rels");
            assertNotNull(rootRels);
            assertTrue(rootRels.getRelationships().stream().anyMatch(r -> "rel0".equals(r.getId())));
        }
    }

    /**
     * Verifies that the parser correctly handles [Content_Types].xml.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseWithContentTypes() throws IOException {
        String contentTypesContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                "  <Default Extension=\"model\" ContentType=\"application/vnd.ms-package.3dmanufacturing-3dmodel+xml\"/>\n" +
                "  <Override PartName=\"/3D/3dmodel.model\" ContentType=\"application/vnd.ms-package.3dmanufacturing-3dmodel+xml\"/>\n" +
                "</Types>";

        String modelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources />\n" +
                "</model>";

        Map<String, String> files = new HashMap<>();
        files.put("[Content_Types].xml", contentTypesContent);
        files.put("3D/3dmodel.model", modelContent);

        byte[] zipData = createZipWithFiles(files);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));

        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);

        assertNotNull(model);
        Mf3ContentTypes contentTypes = model.contentTypes();
        assertNotNull(contentTypes);
        assertEquals(2, contentTypes.getDefaults().size());
        assertEquals(1, contentTypes.getOverrides().size());
        assertEquals("rels", contentTypes.getDefaults().get(0).getExtension());
        assertEquals("/3D/3dmodel.model", contentTypes.getOverrides().get(0).getPartName());
    }

    /**
     * Verifies that the parser correctly handles invalid [Content_Types].xml (schema validation).
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseWithInvalidContentTypes() throws IOException {
        String invalidContentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "  <InvalidElement />\n" +
                "</Types>";

        String modelContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources />\n" +
                "</model>";

        Map<String, String> files = new HashMap<>();
        files.put("[Content_Types].xml", invalidContentTypes);
        files.put("3D/3dmodel.model", modelContent);

        byte[] zipData = createZipWithFiles(files);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));

        Mf3Parser parser = new Mf3Parser();
        // Should throw IOException due to validation failure if XSD is found
        assertThrows(IOException.class, () -> parser.parse(channel));
    }

    @Test
    public void testParsePrusaMetadata() throws IOException {
        Mf3Parser parser = new Mf3Parser();
        InputStream is = getClass().getResourceAsStream("/3mf/test-prusa.3mf");
        assertNotNull(is, "Resource test-prusa.3mf not found");

        Mf3Model model = parser.parse(Channels.newChannel(is));
        assertNotNull(model);

        // Check main model Prusa metadata
        assertNotNull(model.getPrusaMainMetadata());
        assertEquals("1", model.getPrusaMainMetadata().getVersion3mf());

        // Check Slic3r_PE_model.config
        Mf3PrusaSlicerModelConfig config = model.getPrusaSlicerModelConfig();
        assertNotNull(config);
        assertFalse(config.getObjects().isEmpty());
        assertEquals(1, config.getObjects().size());
        assertEquals(1, config.getObjects().get(0).getId());

        // Check Slic3r_PE.config settings
        Mf3PrusaSettings settings = model.getPrusaSettings();
        assertNotNull(settings);
        assertEquals("60", settings.getBedTemperature());
        assertNotNull(settings.get("arc_fitting"));
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

    /**
     * Verifies that the parser correctly handles the 2013/11 namespace.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParse2013_11_Namespace() throws IOException {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model unit=\"millimeter\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/11/3dmodel\">\n" +
                "  <resources>\n" +
                "    <object id=\"1\" name=\"Test\" type=\"model\" partnumber=\"PN-001\">\n" +
                "      <mesh>\n" +
                "        <vertices><vertex x=\"0\" y=\"0\" z=\"0\"/></vertices>\n" +
                "        <triangles/>\n" +
                "      </mesh>\n" +
                "    </object>\n" +
                "  </resources>\n" +
                "  <build>\n" +
                "    <item objectid=\"1\" transform=\"1 0 0 0 1 0 0 0 1 0 0 0\"/>\n" +
                "  </build>\n" +
                "</model>";

        String relsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"r1\" Type=\"http://schemas.microsoft.com/3dmanufacturing/2013/11/3dmodel/mainmodel\" Target=\"/3D/model.model\"/>\n" +
                "</Relationships>";

        Map<String, String> files = new HashMap<>();
        files.put("_rels/.rels", relsContent);
        files.put("3D/model.model", xmlContent);

        byte[] zipData = createZipWithFiles(files);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));

        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);

        assertNotNull(model);
        assertEquals(1, model.objects().size());
        assertEquals("Test", model.objects().get(0).name());
        assertEquals("model", model.objects().get(0).getType());
        assertEquals("PN-001", model.objects().get(0).getPartNumber());

        assertNotNull(model.getBuild());
        assertEquals(1, model.getBuild().getItems().size());
        assertEquals(1, model.getBuild().getItems().get(0).getObjectId());
        assertEquals("1 0 0 0 1 0 0 0 1 0 0 0", model.getBuild().getItems().get(0).getTransform());
    }

    /**
     * Verifies that the parser correctly handles the core 2015/02 namespace.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseCore2015Namespace() throws IOException {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model unit=\"millimeter\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n" +
                "  <resources>\n" +
                "    <object id=\"1\" name=\"CoreTest\">\n" +
                "      <mesh>\n" +
                "        <vertices><vertex x=\"0\" y=\"0\" z=\"0\"/></vertices>\n" +
                "        <triangles/>\n" +
                "      </mesh>\n" +
                "    </object>\n" +
                "  </resources>\n" +
                "</model>";

        String relsContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"r1\" Type=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel\" Target=\"/3D/model.model\"/>\n" +
                "</Relationships>";

        Map<String, String> files = new HashMap<>();
        files.put("_rels/.rels", relsContent);
        files.put("3D/model.model", xmlContent);

        byte[] zipData = createZipWithFiles(files);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));

        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);

        assertNotNull(model);
        assertEquals(1, model.objects().size());
        assertEquals("CoreTest", model.objects().get(0).name());
    }

    /**
     * Verifies that the parser correctly handles components in an object.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseComponents() throws IOException {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources>\n" +
                "    <object id=\"1\" name=\"Part\"><mesh><vertices><vertex x=\"0\" y=\"0\" z=\"0\"/></vertices><triangles/></mesh></object>\n" +
                "    <object id=\"2\" name=\"Assembly\">\n" +
                "      <components>\n" +
                "        <component objectid=\"1\" transform=\"1 0 0 0 1 0 0 0 1 10 0 0\"/>\n" +
                "      </components>\n" +
                "    </object>\n" +
                "  </resources>\n" +
                "</model>";

        byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(zipData));

        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(channel);

        assertNotNull(model);
        assertEquals(2, model.objects().size());

        Mf3Object assembly = model.objects().get(1);
        assertEquals("Assembly", assembly.name());
        assertNotNull(assembly.getComponents());
        assertEquals(1, assembly.getComponents().getComponents().size());
        assertEquals(1, assembly.getComponents().getComponents().get(0).getObjectId());
        assertEquals("1 0 0 0 1 0 0 0 1 10 0 0", assembly.getComponents().getComponents().get(0).getTransform());
    }

    /**
     * Verifies that non-parsed files in the 3MF package are extracted to the storage directory.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testExtractionOfNonParsedFiles() throws IOException {
        final Map<String, String> files = new HashMap<>();
        files.put("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "  <Default Extension=\"model\" ContentType=\"application/vnd.ms-package.3dmanufacturing-3dmodel+xml\"/>\n" +
                "  <Default Extension=\"png\" ContentType=\"image/png\"/>\n" +
                "</Types>");
        files.put("3D/3dmodel.model", "<model unit=\"millimeter\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources></resources><build></build></model>");
        files.put("3D/Textures/texture1.png", "fake-png-content");
        files.put("Metadata/custom.xml", "<custom>data</custom>");

        final byte[] zipData = createZipWithFiles(files);
        final Mf3Parser parser = new Mf3Parser();
        final Mf3Model model = parser.parse(Channels.newChannel(new ByteArrayInputStream(zipData)));

        assertNotNull(model);
        final Path storagePath = model.storagePath();
        assertNotNull(storagePath);
        assertTrue(Files.exists(storagePath));
        assertTrue(Files.isDirectory(storagePath));

        // Check extracted files
        assertTrue(Files.exists(storagePath.resolve("3D/Textures/texture1.png")));
        assertTrue(Files.exists(storagePath.resolve("Metadata/custom.xml")));
        assertEquals("fake-png-content", Files.readString(storagePath.resolve("3D/Textures/texture1.png")));
        assertEquals("<custom>data</custom>", Files.readString(storagePath.resolve("Metadata/custom.xml")));

        // Core files should NOT be in storage
        assertFalse(Files.exists(storagePath.resolve("3D/3dmodel.model")));
        assertFalse(Files.exists(storagePath.resolve("[Content_Types].xml")));
    }

    /**
     * Verifies that the parser correctly handles materials and property references.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseMaterials() throws IOException {
        final String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model unit=\"millimeter\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel\">\n" +
                "  <resources>\n" +
                "    <basematerials id=\"5\">\n" +
                "      <base name=\"Red Material\" displaycolor=\"#FF0000\" />\n" +
                "      <base name=\"Green Material\" displaycolor=\"#00FF00\" />\n" +
                "    </basematerials>\n" +
                "    <object id=\"1\" name=\"Cube\" pid=\"5\" pindex=\"0\">\n" +
                "      <mesh>\n" +
                "        <vertices>\n" +
                "          <vertex x=\"0\" y=\"0\" z=\"0\" />\n" +
                "          <vertex x=\"1\" y=\"0\" z=\"0\" />\n" +
                "          <vertex x=\"0\" y=\"1\" z=\"0\" />\n" +
                "        </vertices>\n" +
                "        <triangles>\n" +
                "          <triangle v1=\"0\" v2=\"1\" v3=\"2\" pid=\"5\" pindex=\"1\" />\n" +
                "          <triangle v1=\"0\" v2=\"1\" v3=\"2\" />\n" +
                "        </triangles>\n" +
                "      </mesh>\n" +
                "    </object>\n" +
                "  </resources>\n" +
                "</model>";

        final byte[] zipData = createZipWithFile("3D/3dmodel.model", xmlContent);
        final Mf3Parser parser = new Mf3Parser();
        final Mf3Model model = parser.parse(Channels.newChannel(new ByteArrayInputStream(zipData)));

        assertNotNull(model);
        assertNotNull(model.getResources());
        assertEquals(1, model.getResources().getBaseMaterials().size());

        final Mf3BaseMaterials bm = model.getResources().getBaseMaterials().get(0);
        assertEquals(5, bm.getId());
        assertEquals(2, bm.getBases().size());
        assertEquals("#FF0000", bm.getBases().get(0).getDisplayColor());

        final Mf3Object object = model.objects().get(0);
        assertEquals(Integer.valueOf(5), object.getPid());
        assertEquals(Integer.valueOf(0), object.getPindex());

        final Mf3Triangle tri1 = object.triangles().get(0);
        assertEquals(Integer.valueOf(5), tri1.getPid());
        assertEquals(Integer.valueOf(1), tri1.getPindex());

        final Mf3Triangle tri2 = object.triangles().get(1);
        assertNull(tri2.getPid());
        assertNull(tri2.getPindex());
    }
}
