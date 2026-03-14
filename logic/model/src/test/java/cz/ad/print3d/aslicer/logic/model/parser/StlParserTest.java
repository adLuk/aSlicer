package cz.ad.print3d.aslicer.logic.model.parser;

import cz.ad.print3d.aslicer.logic.model.stl.StlModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StlParser.
 */
public class StlParserTest {

    /**
     * Verifies that the parser handles an STL file with zero facets.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseEmptyStl() throws IOException {
        byte[] header = new byte[80];
        for (int i = 0; i < 80; i++) header[i] = (byte) i;
        
        ByteBuffer buffer = ByteBuffer.allocate(84);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(header);
        buffer.putInt(0); // 0 triangles
        
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(buffer.array()));
        
        StlParser parser = new StlParser();
        StlModel model = parser.parse(channel);
        
        assertArrayEquals(header, model.header());
        assertEquals(0, model.facetCount());
        assertTrue(model.facets().isEmpty());
    }

    /**
     * Verifies that the parser correctly reads a single facet from an STL file.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseSingleFacet() throws IOException {
        byte[] header = new byte[80];
        
        ByteBuffer buffer = ByteBuffer.allocate(84 + 50);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(header);
        buffer.putInt(1); // 1 triangle
        
        // Normal
        buffer.putFloat(0.0f); buffer.putFloat(0.0f); buffer.putFloat(1.0f);
        // V1
        buffer.putFloat(0.0f); buffer.putFloat(0.0f); buffer.putFloat(0.0f);
        // V2
        buffer.putFloat(1.0f); buffer.putFloat(0.0f); buffer.putFloat(0.0f);
        // V3
        buffer.putFloat(0.0f); buffer.putFloat(1.0f); buffer.putFloat(0.0f);
        // Attribute byte count
        buffer.putShort((short) 0);
        
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(buffer.array()));
        
        StlParser parser = new StlParser();
        StlModel model = parser.parse(channel);
        
        assertEquals(1, model.facetCount());
        var facet = model.facets().get(0);
        assertEquals(0.0f, facet.normal().x());
        assertEquals(0.0f, facet.normal().y());
        assertEquals(1.0f, facet.normal().z());
        
        assertEquals(0.0f, facet.v1().x());
        assertEquals(1.0f, facet.v2().x());
        assertEquals(1.0f, facet.v3().y());
    }
}
