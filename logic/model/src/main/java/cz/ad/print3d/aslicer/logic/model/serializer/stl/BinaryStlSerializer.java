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

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import cz.ad.print3d.aslicer.logic.model.serializer.ModelSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

/**
 * Implementation of ModelSerializer for binary STL files.
 */
public class BinaryStlSerializer implements ModelSerializer<StlModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryStlSerializer.class);

    /**
     * Size of the binary STL header in bytes.
     */
    private static final int HEADER_SIZE = 80;

    /**
     * Size of the triangle count field in bytes.
     */
    private static final int TRIANGLE_COUNT_SIZE = 4;

    /**
     * Size of a single facet entry in bytes (50 bytes total: 4*3*4 for vertices/normal + 2 for attributes).
     */
    private static final int FACET_SIZE = 50;

    /**
     * Serializes the binary STL content to the given channel.
     *
     * @param model the model to serialize
     * @param channel the output binary channel
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serialize(final StlModel model, final WritableByteChannel channel) throws IOException {
        LOGGER.info("Starting binary STL serialization");
        // Write 80-byte header
        final ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE);
        if (model.header() != null) {
            final int len = Math.min(model.header().length, HEADER_SIZE);
            headerBuffer.put(model.header(), 0, len);
        }
        // Fill remaining header space with zeros if necessary
        while (headerBuffer.hasRemaining()) {
            headerBuffer.put((byte) 0);
        }
        headerBuffer.flip();
        writeFully(channel, headerBuffer);
        LOGGER.debug("Wrote 80-byte STL header");

        // Write 4-byte triangle count (Little Endian)
        final ByteBuffer countBuffer = ByteBuffer.allocate(TRIANGLE_COUNT_SIZE);
        countBuffer.order(ByteOrder.LITTLE_ENDIAN);
        final int facetCount = model.facetCount();
        countBuffer.putInt(facetCount);
        countBuffer.flip();
        writeFully(channel, countBuffer);
        LOGGER.info("Writing {} facets to binary STL", facetCount);

        // Write facets
        final ByteBuffer facetBuffer = ByteBuffer.allocate(FACET_SIZE);
        facetBuffer.order(ByteOrder.LITTLE_ENDIAN);

        int i = 0;
        for (final StlFacet facet : model.facets()) {
            LOGGER.trace("Writing facet {}: normal={}, v1={}, v2={}, v3={}, attrs={}", i++, facet.normal(), facet.v1(), facet.v2(), facet.v3(), facet.attributeByteCount());
            facetBuffer.clear();
            writeVector(facetBuffer, facet.normal());
            writeVector(facetBuffer, facet.v1());
            writeVector(facetBuffer, facet.v2());
            writeVector(facetBuffer, facet.v3());
            facetBuffer.putShort((short) facet.attributeByteCount());
            facetBuffer.flip();
            writeFully(channel, facetBuffer);
        }
        LOGGER.info("Finished binary STL serialization successfully");
    }

    /**
     * Writes all bytes from the buffer to the channel.
     *
     * @param channel the channel to write to
     * @param buffer the buffer to write from
     * @throws IOException if an I/O error occurs
     */
    private void writeFully(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    /**
     * Writes a 3D vector to the buffer.
     *
     * @param buffer the buffer to write to
     * @param vector the vector to write
     */
    private void writeVector(final ByteBuffer buffer, final Vector3f vector) {
        buffer.putFloat(vector.x());
        buffer.putFloat(vector.y());
        buffer.putFloat(vector.z());
    }
}
