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
package cz.ad.print3d.aslicer.logic.model.serializer.stl;

import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import cz.ad.print3d.aslicer.logic.model.serializer.ModelSerializer;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * STL Serializer dispatcher that supports both binary and ASCII formats.
 * By default, it uses the binary format.
 */
public class StlSerializer implements ModelSerializer<StlModel> {

    /**
     * Enumeration of supported STL formats.
     */
    public enum Format {
        BINARY,
        ASCII
    }

    /**
     * The format to use for serialization (BINARY or ASCII).
     */
    private Format format;

    /**
     * Creates a new StlSerializer with the default BINARY format.
     */
    public StlSerializer() {
        this(Format.BINARY);
    }

    /**
     * Creates a new StlSerializer with the specified format.
     *
     * @param format the format to use for serialization
     */
    public StlSerializer(final Format format) {
        this.format = format;
    }

    /**
     * Gets the current serialization format.
     *
     * @return the current format
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Sets the serialization format.
     *
     * @param format the format to use
     */
    public void setFormat(final Format format) {
        this.format = format;
    }

    /**
     * Serializes the model using the configured format.
     *
     * @param model the model to serialize
     * @param channel the output channel
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serialize(final StlModel model, final WritableByteChannel channel) throws IOException {
        final ModelSerializer<StlModel> serializer;
        if (format == Format.ASCII) {
            serializer = new AsciiStlSerializer();
        } else {
            serializer = new BinaryStlSerializer();
        }
        serializer.serialize(model, channel);
    }
}
