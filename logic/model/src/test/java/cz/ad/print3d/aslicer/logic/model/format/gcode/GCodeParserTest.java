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
package cz.ad.print3d.aslicer.logic.model.format.gcode;
import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GCodeParserTest {

    @Test
    public void testParseLineWithSequenceAndComment() {
        String line = "N123 G01 X10.5 Y20.0 F1200 ; this is a comment";
        GCodeBlock block = GCodeParser.parseLine(line);

        assertNotNull(block);
        assertEquals(123L, block.sequenceNumber());
        assertEquals("this is a comment", block.comment());
        assertEquals(4, block.words().size());

        assertEquals(new GCodeWord('G', 1), block.words().get(0));
        assertEquals(new GCodeWord('X', 10.5), block.words().get(1));
        assertEquals(new GCodeWord('Y', 20.0), block.words().get(2));
        assertEquals(new GCodeWord('F', 1200), block.words().get(3));
    }

    @Test
    public void testParseLineWithoutSequence() {
        String line = "G28 X0 Y0 ; Home";
        GCodeBlock block = GCodeParser.parseLine(line);

        assertNotNull(block);
        assertNull(block.sequenceNumber());
        assertEquals("Home", block.comment());
        assertEquals(3, block.words().size());
        assertEquals(new GCodeWord('G', 28), block.words().get(0));
    }

    @Test
    public void testParseOnlyComment() {
        String line = "; Just a comment";
        GCodeBlock block = GCodeParser.parseLine(line);

        assertNotNull(block);
        assertNull(block.sequenceNumber());
        assertEquals("Just a comment", block.comment());
        assertTrue(block.words().isEmpty());
    }

    @Test
    public void testParseMCode() {
        String line = "M117 Printing...";
        GCodeBlock block = GCodeParser.parseLine(line);

        assertNotNull(block);
        assertEquals(new GCodeWord('M', 117), block.words().get(0));
        // Note: Currently GCodeParser ignores text after M117 if it's not a word pattern.
        // ISO 6983 words are address + value. "Printing..." is not a word.
    }

    @Test
    public void testToString() {
        String line = "N123 G1 X10 Y20 ; move";
        GCodeBlock block = GCodeParser.parseLine(line);
        assertNotNull(block);
        assertEquals("N123 G1 X10 Y20 ;move", block.toString());
    }
}
