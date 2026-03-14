package cz.ad.print3d.aslicer.logic.model.parser.stl;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StlParser and its specialized implementations.
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
        
        BinaryStlParser parser = new BinaryStlParser(Unit.MILLIMETER);
        StlModel model = parser.parse(channel);
        
        assertArrayEquals(header, model.header());
        assertEquals(0, model.facetCount());
        assertTrue(model.facets().isEmpty());
        assertEquals(Unit.MILLIMETER, model.unit());
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
        
        BinaryStlParser parser = new BinaryStlParser(Unit.MILLIMETER);
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

    /**
     * Verifies that the dispatcher correctly delegates to BinaryStlParser for binary content.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testDispatcherBinary() throws IOException {
        byte[] header = new byte[80];
        ByteBuffer buffer = ByteBuffer.allocate(84);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(header);
        buffer.putInt(0);
        
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(buffer.array()));
        StlParser parser = new StlParser(Unit.MILLIMETER);
        StlModel model = parser.parse(channel);
        
        assertEquals(0, model.facetCount());
        assertEquals(Unit.MILLIMETER, model.unit());
    }

    /**
     * Verifies that the dispatcher correctly delegates to AsciiStlParser for ASCII content.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testDispatcherAscii() throws IOException {
        String ascii = "solid test\nfacet normal 0 0 1\nouter loop\nvertex 0 0 0\nvertex 1 0 0\nvertex 0 1 0\nendloop\nendfacet\nendsolid test";
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(ascii.getBytes(StandardCharsets.US_ASCII)));
        
        StlParser parser = new StlParser(Unit.INCH);
        StlModel model = parser.parse(channel);
        
        assertEquals(1, model.facetCount());
        assertEquals("test", new String(model.header(), StandardCharsets.US_ASCII).trim());
        assertEquals(Unit.INCH, model.unit());
    }

    /**
     * Verifies that the parser can load an ASCII STL file from resources.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testParseAsciiResource() throws IOException {
        String resourcePath = "/stl/test-ascii.stl";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            ReadableByteChannel channel = Channels.newChannel(is);
            StlParser parser = new StlParser(Unit.CENTIMETER);
            StlModel model = parser.parse(channel);
            
            assertEquals(1, model.facetCount());
            assertEquals("simple", new String(model.header(), StandardCharsets.US_ASCII).trim());
            assertEquals(Unit.CENTIMETER, model.unit());
            
            var facet = model.facets().get(0);
            assertEquals(1.0f, facet.normal().z());
        }
    }
}
