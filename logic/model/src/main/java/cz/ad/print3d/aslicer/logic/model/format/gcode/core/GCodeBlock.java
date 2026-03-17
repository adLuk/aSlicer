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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a single G-code block (typically a line of G-code).
 * According to ISO 6983-1, a block consists of a sequence of words and is terminated by a block end character.
 * A block may optionally contain a sequence number and comments.
 *
 * Typical structure:
 * N123 G01 X10.5 Y20.0 F1200 ; this is a comment
 *
 * @param sequenceNumber the optional sequence number (e.g., N123)
 * @param words          the list of words forming the command part of the block
 * @param comment        the optional comment associated with the block
 */
public record GCodeBlock(Long sequenceNumber, List<GCodeWord> words, String comment) {

    /**
     * Creates a new G-code block.
     *
     * @param sequenceNumber the optional sequence number (N)
     * @param words          the list of G-code words
     * @param comment        the optional comment
     */
    public GCodeBlock {
        words = words != null ? new ArrayList<>(words) : new ArrayList<>();
    }

    /**
     * Gets an unmodifiable list of words in this block.
     *
     * @return the list of G-code words
     */
    @Override
    public List<GCodeWord> words() {
        return Collections.unmodifiableList(words);
    }

    /**
     * Gets an optional sequence number.
     *
     * @return an Optional containing the sequence number
     */
    public Optional<Long> getSequenceNumber() {
        return Optional.ofNullable(sequenceNumber);
    }

    /**
     * Gets an optional comment.
     *
     * @return an Optional containing the comment
     */
    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    /**
     * Returns a string representation of the G-code block.
     *
     * @return the string representation of the block
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        getSequenceNumber().ifPresent(n -> sb.append("N").append(n).append(" "));
        sb.append(words.stream().map(GCodeWord::toString).collect(Collectors.joining(" ")));
        getComment().ifPresent(c -> {
            if (!sb.isEmpty() && !sb.toString().endsWith(" ")) {
                sb.append(" ");
            }
            sb.append(";").append(c);
        });
        return sb.toString().trim();
    }
}
