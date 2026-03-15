package cz.ad.print3d.aslicer.logic.model.parser.stl;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ModelParser for binary STL files.
 */
public class BinaryStlParser implements ModelParser<StlModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryStlParser.class);

    /**
     * The unit used for coordinate values in the parsed model.
     */
    private final Unit unit;

    /**
     * Creates a new binary STL parser with the specified unit.
     *
     * @param unit the measurement unit to associate with the model
     */
    public BinaryStlParser(final Unit unit) {
        this.unit = unit;
    }

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
    public StlModel parse(final ReadableByteChannel channel) throws IOException {
        LOGGER.info("Starting binary STL parsing");
        // Read 80-byte header
        final ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE);
        readFully(channel, headerBuffer);
        final byte[] header = headerBuffer.array();
        LOGGER.debug("Read 80-byte STL header");

        // Read 4-byte triangle count (Little Endian)
        final ByteBuffer countBuffer = ByteBuffer.allocate(TRIANGLE_COUNT_SIZE);
        countBuffer.order(ByteOrder.LITTLE_ENDIAN);
        readFully(channel, countBuffer);
        countBuffer.flip();
        final int triangleCount = countBuffer.getInt();
        LOGGER.info("Expecting {} facets from binary STL", triangleCount);

        // Read facets
        final List<StlFacet> facets = new ArrayList<>(triangleCount);
        final ByteBuffer facetBuffer = ByteBuffer.allocate(FACET_SIZE);
        facetBuffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < triangleCount; i++) {
            facetBuffer.clear();
            readFully(channel, facetBuffer);
            facetBuffer.flip();

            final Vector3f normal = readVector(facetBuffer);
            final Vector3f v1 = readVector(facetBuffer);
            final Vector3f v2 = readVector(facetBuffer);
            final Vector3f v3 = readVector(facetBuffer);
            final int attributeByteCount = facetBuffer.getShort() & 0xFFFF;

            LOGGER.trace("Read facet {}: normal={}, v1={}, v2={}, v3={}, attrs={}", i, normal, v1, v2, v3, attributeByteCount);
            facets.add(new StlFacet(normal, v1, v2, v3, attributeByteCount));
        }

        LOGGER.info("Finished binary STL parsing successfully");
        return new StlModel(header, facets, unit);
    }

    /**
     * Reads bytes from the channel into the buffer until the buffer is full.
     *
     * @param channel the channel to read from
     * @param buffer the buffer to fill
     * @throws IOException if an I/O error occurs or the end of the stream is reached before the buffer is full
     */
    private void readFully(final ReadableByteChannel channel, final ByteBuffer buffer) throws IOException {
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
    private Vector3f readVector(final ByteBuffer buffer) {
        final float x = buffer.getFloat();
        final float y = buffer.getFloat();
        final float z = buffer.getFloat();
        return new Vector3f(x, y, z);
    }
}
