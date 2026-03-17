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
package cz.ad.print3d.aslicer.logic.model.serializer.gcode;

import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.flavor.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.flavor.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.flavor.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;
import cz.ad.print3d.aslicer.logic.model.format.gcode.flavor.*;
import cz.ad.print3d.aslicer.logic.model.parser.gcode.GCodeModelParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GCodeModelSerializerTest {

    @Test
    public void testBasicSerialization() throws IOException {
        GCode gcode = new MarlinGCode();
        gcode.addCommand(new GCodeBlock(1L, List.of(new GCodeWord('G', 21)), "set units to millimeters"));
        gcode.addCommand(new GCodeBlock(null, List.of(new GCodeWord('G', 1), new GCodeWord('X', 10), new GCodeWord('Y', 20.5)), "move"));

        GCodeModelSerializer serializer = new GCodeModelSerializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (WritableByteChannel channel = Channels.newChannel(baos)) {
            serializer.serialize(gcode, channel);
        }

        String result = baos.toString(StandardCharsets.UTF_8);
        String expected = "N1 G21 ;set units to millimeters\nG1 X10 Y20.5 ;move\n";
        
        // Normalize line endings for cross-platform compatibility if necessary, 
        // but here we used '\n' explicitly in serializer.
        assertEquals(expected, result.replace("\r\n", "\n"));
    }

    @Test
    public void testRoundTripComplexFile() throws IOException {
        String resourcePath = "/gcode/complex_edge_cases.gcode";
        InputStream is = getClass().getResourceAsStream(resourcePath);
        assertNotNull(is, "Resource not found: " + resourcePath);

        GCodeModelParser parser = new GCodeModelParser();
        GCode originalGCode;
        try (ReadableByteChannel channel = Channels.newChannel(is)) {
            originalGCode = parser.parse(channel);
        }

        // Serialize the parsed G-code
        GCodeModelSerializer serializer = new GCodeModelSerializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (WritableByteChannel channel = Channels.newChannel(baos)) {
            serializer.serialize(originalGCode, channel);
        }

        byte[] serializedData = baos.toByteArray();

        // Parse the serialized data back
        GCode reParsedGCode;
        try (ReadableByteChannel channel = Channels.newChannel(new java.io.ByteArrayInputStream(serializedData))) {
            reParsedGCode = parser.parse(channel);
        }

        // Compare original DTO with re-parsed DTO
        // Note: metadata is also compared in GCode.equals()
        assertEquals(originalGCode, reParsedGCode, "DTO content should be identical after round-trip");
        
        // Additional check: number of commands
        assertEquals(originalGCode.getCommands().size(), reParsedGCode.getCommands().size());
    }
}
