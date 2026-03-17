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
import cz.ad.print3d.aslicer.logic.model.serializer.ModelSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * Serializer for G-code files that implements {@link ModelSerializer}.
 * This serializer writes a {@link GCode} object into a {@link WritableByteChannel}
 * as UTF-8 encoded text.
 *
 * <p>It relies on {@link GCodeBlock#toString()} to correctly format each line
 * according to ISO 6983 standards and common 3D printing conventions.</p>
 */
public class GCodeModelSerializer implements ModelSerializer<GCode> {

    /**
     * Serializes the given G-code DTO into the specified binary output channel.
     * Each {@link GCodeBlock} is written as a single line, terminated by a platform-independent
     * newline character ('\n').
     *
     * @param model   the G-code model to serialize
     * @param channel the output binary channel
     * @throws IOException if an I/O error occurs during serialization
     */
    @Override
    public void serialize(GCode model, WritableByteChannel channel) throws IOException {
        if (model == null || model.getCommands() == null) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(Channels.newWriter(channel, StandardCharsets.UTF_8))) {
            for (GCodeBlock block : model.getCommands()) {
                writer.write(block.toString());
                writer.write('\n');
            }
            writer.flush();
        }
    }
}
