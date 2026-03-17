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
package cz.ad.print3d.aslicer.logic.model.parser.gcode;

import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.flavor.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.flavor.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.flavor.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GCodeModelParser}.
 */
public class GCodeModelParserTest {

    @Test
    public void testParseExampleFile() throws IOException {
        GCodeModelParser parser = new GCodeModelParser();
        try (InputStream is = getClass().getResourceAsStream("/gcode/example.gcode")) {
            assertNotNull(is, "Example G-code file not found in resources");
            try (ReadableByteChannel channel = Channels.newChannel(is)) {
                GCode gcode = parser.parse(channel);

                assertNotNull(gcode);
                assertFalse(gcode.getCommands().isEmpty());

                // Verify metadata extraction
                Map<String, String> metadata = gcode.getMetadata();
                assertEquals("aSlicer - free G-code example", metadata.get("slicer"));
                assertEquals("Marlin", metadata.get("flavor"));
                assertEquals("0.2", metadata.get("layer_height"));
                assertEquals("0.3", metadata.get("initial_layer_height"));

                // Verify some commands
                List<GCodeBlock> commands = gcode.getCommands();
                
                // G21 ; set units to millimeters
                GCodeBlock g21 = commands.get(4); // Indices: 0-3 are comments
                assertEquals(new GCodeWord('G', 21), g21.words().get(0));
                assertEquals("set units to millimeters", g21.comment());

                // N1 G1 Z5 F5000 ; lift nozzle
                GCodeBlock n1 = commands.stream()
                        .filter(b -> b.getSequenceNumber().orElse(-1L) == 1L)
                        .findFirst()
                        .orElseThrow();
                assertEquals(3, n1.words().size());
                assertEquals(new GCodeWord('G', 1), n1.words().get(0));
                assertEquals(new GCodeWord('Z', 5), n1.words().get(1));
                assertEquals(new GCodeWord('F', 5000), n1.words().get(2));
                assertEquals("lift nozzle", n1.comment());
            }
        }
    }

    @Test
    public void testParseComplexEdgeCases() throws IOException {
        GCodeModelParser parser = new GCodeModelParser();
        try (InputStream is = getClass().getResourceAsStream("/gcode/complex_edge_cases.gcode")) {
            assertNotNull(is, "Complex G-code file not found in resources");
            try (ReadableByteChannel channel = Channels.newChannel(is)) {
                GCode gcode = parser.parse(channel);

                assertNotNull(gcode);
                assertFalse(gcode.getCommands().isEmpty());

                // Verify metadata
                assertEquals("PrusaSlicer 2.6.0 on 2026-03-17", gcode.getMetadata().get("slicer"));
                assertEquals("0.2", gcode.getMetadata().get("layer_height"));
                assertEquals("0.3", gcode.getMetadata().get("first_layer_height"));

                // Verify specific blocks
                List<GCodeBlock> commands = gcode.getCommands();

                // N6 G1 X-10.5 Y+20.5 Z.5 E.001 ; signed values and omitted leading zero
                GCodeBlock n6 = commands.stream()
                        .filter(b -> b.getSequenceNumber().orElse(-1L) == 6L)
                        .findFirst()
                        .orElseThrow();
                assertEquals(new GCodeWord('G', 1), n6.words().get(0));
                assertEquals(new GCodeWord('X', -10.5), n6.words().get(1));
                assertEquals(new GCodeWord('Y', 20.5), n6.words().get(2));
                assertEquals(new GCodeWord('Z', 0.5), n6.words().get(3));
                assertEquals(new GCodeWord('E', 0.001), n6.words().get(4));
            }
        }
    }

    @Test
    public void testEmptyChannel() throws IOException {
        GCodeModelParser parser = new GCodeModelParser();
        try (InputStream is = new java.io.ByteArrayInputStream(new byte[0]);
             ReadableByteChannel channel = Channels.newChannel(is)) {
            GCode gcode = parser.parse(channel);
            assertNotNull(gcode);
            assertTrue(gcode.getCommands().isEmpty());
        }
    }
}
