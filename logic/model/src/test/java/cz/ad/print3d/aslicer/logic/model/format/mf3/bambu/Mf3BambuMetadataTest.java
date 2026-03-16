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
package cz.ad.print3d.aslicer.logic.model.format.mf3.bambu;

import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.parser.mf3.Mf3Parser;
import cz.ad.print3d.aslicer.logic.model.serializer.mf3.Mf3Serializer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class Mf3BambuMetadataTest {

    @Test
    public void testBambuMetadataRoundTrip() throws IOException {
        // 1. Create a mock 3MF with Bambu metadata
        Map<String, byte[]> mock3mf = new HashMap<>();
        
        String modelXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<model unit=\"millimeter\" xml:lang=\"en-US\" xmlns=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02\">\n" +
                "  <metadata name=\"BambuStudio:version\">1.7.4.52</metadata>\n" +
                "  <resources>\n" +
                "    <object id=\"1\" type=\"model\">\n" +
                "      <mesh>\n" +
                "        <vertices>\n" +
                "          <vertex x=\"0\" y=\"0\" z=\"0\" />\n" +
                "          <vertex x=\"10\" y=\"0\" z=\"0\" />\n" +
                "          <vertex x=\"10\" y=\"10\" z=\"0\" />\n" +
                "        </vertices>\n" +
                "        <triangles>\n" +
                "          <triangle v1=\"0\" v2=\"1\" v3=\"2\" />\n" +
                "        </triangles>\n" +
                "      </mesh>\n" +
                "    </object>\n" +
                "  </resources>\n" +
                "  <build>\n" +
                "    <item objectid=\"1\" />\n" +
                "  </build>\n" +
                "</model>";
        mock3mf.put("3D/3dmodel.model", modelXml.getBytes(StandardCharsets.UTF_8));

        String relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "  <Relationship Id=\"rel1\" Target=\"/3D/3dmodel.model\" Type=\"http://schemas.microsoft.com/3dmanufacturing/core/2015/02/mainmodel\" />\n" +
                "</Relationships>";
        mock3mf.put("_rels/.rels", relsXml.getBytes(StandardCharsets.UTF_8));

        String bambuConfigXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<config>\n" +
                "    <plate id=\"1\"/>\n" +
                "    <plate id=\"2\"/>\n" +
                "</config>";
        mock3mf.put("Metadata/model_settings.config", bambuConfigXml.getBytes(StandardCharsets.UTF_8));

        String bambuGCodeJson = "{\"custom_gcode_per_plate\":[\"G28 ; Home\",\"M117 Printing plate 2\"]}";
        mock3mf.put("Metadata/Bambu_Custom_GCode", bambuGCodeJson.getBytes(StandardCharsets.UTF_8));

        byte[] zipData = createZip(mock3mf);

        // 2. Parse the mock 3MF
        Mf3Parser parser = new Mf3Parser();
        Mf3Model model = parser.parse(Channels.newChannel(new java.io.ByteArrayInputStream(zipData)));

        assertNotNull(model.getBambuConfig());
        assertEquals(2, model.getBambuConfig().getPlate().size());
        assertEquals("1", model.getBambuConfig().getPlate().get(0).getId());
        assertEquals("2", model.getBambuConfig().getPlate().get(1).getId());

        assertNotNull(model.getBambuCustomGCode());
        assertArrayEquals(new String[]{"G28 ; Home", "M117 Printing plate 2"}, model.getBambuCustomGCode().getCustomGCodePerPlate());

        // 3. Serialize back
        Mf3Serializer serializer = new Mf3Serializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(model, Channels.newChannel(baos));
        byte[] serializedZip = baos.toByteArray();

        // 4. Parse the serialized 3MF and compare
        Mf3Model reParsedModel = parser.parse(Channels.newChannel(new java.io.ByteArrayInputStream(serializedZip)));

        assertEquals(model.getBambuConfig(), reParsedModel.getBambuConfig());
        assertEquals(model.getBambuCustomGCode(), reParsedModel.getBambuCustomGCode());

        // 5. Compare the files in the zip for identity
        Map<String, byte[]> resultFiles = getZipEntries(serializedZip);
        // Compare model settings
        String originalConfig = new String(mock3mf.get("Metadata/model_settings.config"), StandardCharsets.UTF_8).trim();
        String serializedConfig = new String(resultFiles.get("Metadata/model_settings.config"), StandardCharsets.UTF_8).trim();
        assertEquals(originalConfig, serializedConfig);

        // Compare custom g-code
        String originalGCode = new String(mock3mf.get("Metadata/Bambu_Custom_GCode"), StandardCharsets.UTF_8).trim();
        String serializedGCode = new String(resultFiles.get("Metadata/Bambu_Custom_GCode"), StandardCharsets.UTF_8).trim();
        assertEquals(originalGCode, serializedGCode);
    }

    private Map<String, byte[]> getZipEntries(byte[] data) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                entries.put(entry.getName(), baos.toByteArray());
            }
        }
        return entries;
    }

    private byte[] createZip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
