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
package cz.ad.print3d.aslicer.logic.model.format.gcode.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing G-code strings into structured objects based on ISO 6983.
 * ISO 6983-1 defines the block format for numerical control programs.
 */
public final class GCodeParser {

    private static final Pattern WORD_PATTERN = Pattern.compile("([A-Z])([-+]?\\d+\\.?\\d*|[-+]?\\.\\d+|[-+]?\\d*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("^N(\\d+)", Pattern.CASE_INSENSITIVE);

    private GCodeParser() {
        // Prevent instantiation
    }

    /**
     * Parses a single line of G-code into a {@link GCodeBlock}.
     *
     * @param line the raw G-code line
     * @return the structured G-code block, or null if the line is empty
     */
    public static GCodeBlock parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String comment = null;
        String content = line;

        // Extract comment (following ISO 6983, comments are often in parentheses or following a semicolon)
        // Most 3D printers use semicolon for comments.
        int commentIndex = line.indexOf(';');
        if (commentIndex != -1) {
            comment = line.substring(commentIndex + 1).trim();
            content = line.substring(0, commentIndex);
        }

        content = content.trim();
        if (content.isEmpty() && comment != null) {
            return new GCodeBlock(null, List.of(), comment);
        } else if (content.isEmpty()) {
            return null;
        }

        Long sequenceNumber = null;
        Matcher seqMatcher = SEQUENCE_PATTERN.matcher(content);
        if (seqMatcher.find()) {
            sequenceNumber = Long.parseLong(seqMatcher.group(1));
            content = content.substring(seqMatcher.end()).trim();
        }

        List<GCodeWord> words = new ArrayList<>();
        Matcher wordMatcher = WORD_PATTERN.matcher(content);
        while (wordMatcher.find()) {
            char address = wordMatcher.group(1).toUpperCase().charAt(0);
            String valStr = wordMatcher.group(2);
            double value = valStr.isEmpty() || valStr.equals("-") || valStr.equals("+") ? 0.0 : Double.parseDouble(valStr);
            words.add(new GCodeWord(address, value));
        }

        return new GCodeBlock(sequenceNumber, words, comment);
    }

    /**
     * Parses multiple lines of G-code into a list of {@link GCodeBlock}s.
     *
     * @param content the raw G-code content
     * @return the list of structured G-code blocks
     */
    public static List<GCodeBlock> parse(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<GCodeBlock> blocks = new ArrayList<>();
        String[] lines = content.split("\\R");
        for (String line : lines) {
            GCodeBlock block = parseLine(line);
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }
}
