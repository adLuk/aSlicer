package cz.ad.print3d.aslicer.logic.model.serializer.stl;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import cz.ad.print3d.aslicer.logic.model.serializer.ModelSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

/**
 * Implementation of ModelSerializer for binary STL files.
 */
public class BinaryStlSerializer implements ModelSerializer<StlModel> {

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

        // Write 4-byte triangle count (Little Endian)
        final ByteBuffer countBuffer = ByteBuffer.allocate(TRIANGLE_COUNT_SIZE);
        countBuffer.order(ByteOrder.LITTLE_ENDIAN);
        countBuffer.putInt(model.facetCount());
        countBuffer.flip();
        writeFully(channel, countBuffer);

        // Write facets
        final ByteBuffer facetBuffer = ByteBuffer.allocate(FACET_SIZE);
        facetBuffer.order(ByteOrder.LITTLE_ENDIAN);

        for (final StlFacet facet : model.facets()) {
            facetBuffer.clear();
            writeVector(facetBuffer, facet.normal());
            writeVector(facetBuffer, facet.v1());
            writeVector(facetBuffer, facet.v2());
            writeVector(facetBuffer, facet.v3());
            facetBuffer.putShort((short) facet.attributeByteCount());
            facetBuffer.flip();
            writeFully(channel, facetBuffer);
        }
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
