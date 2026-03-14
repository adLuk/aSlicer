package cz.ad.print3d.aslicer.logic.model.parser;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.stl.StlModel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ModelParser for binary STL files.
 */
public class StlParser implements ModelParser {

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
    private static final int FACET_SIZE = 50; // 12 * 4 + 2 bytes

    /**
     * Parses the binary STL content from the given channel.
     *
     * @param channel the input binary channel
     * @return the parsed StlModel
     * @throws IOException if an I/O error occurs or the stream ends prematurely
     */
    @Override
    public StlModel parse(ReadableByteChannel channel) throws IOException {
        // Read 80-byte header
        ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE);
        readFully(channel, headerBuffer);
        byte[] header = headerBuffer.array();

        // Read 4-byte triangle count (Little Endian)
        ByteBuffer countBuffer = ByteBuffer.allocate(TRIANGLE_COUNT_SIZE);
        countBuffer.order(ByteOrder.LITTLE_ENDIAN);
        readFully(channel, countBuffer);
        countBuffer.flip();
        int triangleCount = countBuffer.getInt();

        // Read facets
        List<StlFacet> facets = new ArrayList<>(triangleCount);
        ByteBuffer facetBuffer = ByteBuffer.allocate(FACET_SIZE);
        facetBuffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < triangleCount; i++) {
            facetBuffer.clear();
            readFully(channel, facetBuffer);
            facetBuffer.flip();

            Vector3f normal = readVector(facetBuffer);
            Vector3f v1 = readVector(facetBuffer);
            Vector3f v2 = readVector(facetBuffer);
            Vector3f v3 = readVector(facetBuffer);
            int attributeByteCount = facetBuffer.getShort() & 0xFFFF;

            facets.add(new StlFacet(normal, v1, v2, v3, attributeByteCount));
        }

        return new StlModel(header, facets);
    }

    /**
     * Reads bytes from the channel into the buffer until the buffer is full.
     *
     * @param channel the channel to read from
     * @param buffer the buffer to fill
     * @throws IOException if an I/O error occurs or the end of the stream is reached before the buffer is full
     */
    private void readFully(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) == -1) {
                throw new IOException("Unexpected end of stream");
            }
        }
    }

    /**
     * Reads a 3D vector from the buffer.
     *
     * @param buffer the buffer containing three float values
     * @return a new Vector3f instance
     */
    private Vector3f readVector(ByteBuffer buffer) {
        float x = buffer.getFloat();
        float y = buffer.getFloat();
        float z = buffer.getFloat();
        return new Vector3f(x, y, z);
    }
}
