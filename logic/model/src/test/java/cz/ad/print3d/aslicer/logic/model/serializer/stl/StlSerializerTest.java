package cz.ad.print3d.aslicer.logic.model.serializer.stl;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.parser.stl.AsciiStlParser;
import cz.ad.print3d.aslicer.logic.model.parser.stl.BinaryStlParser;
import cz.ad.print3d.aslicer.logic.model.parser.stl.StlParser;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for STL serializers.
 */
public class StlSerializerTest {

    /**
     * Verifies that an StlModel can be serialized to a channel and then correctly deserialized
     * back to an equivalent StlModel using BinaryStlParser.
     *
     * @throws IOException if an I/O error occurs during serialization or deserialization
     */
    @Test
    public void testSerializeAndDeserialize() throws IOException {
        // Prepare a model
        byte[] header = new byte[80];
        for (int i = 0; i < 80; i++) {
            header[i] = (byte) i;
        }

        Vector3f normal = new Vector3f(0, 0, 1);
        Vector3f v1 = new Vector3f(0, 0, 0);
        Vector3f v2 = new Vector3f(1, 0, 0);
        Vector3f v3 = new Vector3f(0, 1, 0);
        StlFacet facet = new StlFacet(normal, v1, v2, v3, 0);
        StlModel originalModel = new StlModel(header, List.of(facet), Unit.MILLIMETER);

        // Serialize
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel writeChannel = Channels.newChannel(outputStream);
        BinaryStlSerializer serializer = new BinaryStlSerializer();
        serializer.serialize(originalModel, writeChannel);

        // Deserialize
        byte[] serializedData = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
        ReadableByteChannel readChannel = Channels.newChannel(inputStream);
        BinaryStlParser parser = new BinaryStlParser(Unit.MILLIMETER);
        StlModel deserializedModel = parser.parse(readChannel);

        // Verify
        assertArrayEquals(originalModel.header(), deserializedModel.header());
        assertEquals(originalModel.facetCount(), deserializedModel.facetCount());
        
        StlFacet f1 = originalModel.facets().get(0);
        StlFacet f2 = deserializedModel.facets().get(0);
        
        assertEquals(f1.normal(), f2.normal());
        assertEquals(f1.v1(), f2.v1());
        assertEquals(f1.v2(), f2.v2());
        assertEquals(f1.v3(), f2.v3());
        assertEquals(f1.attributeByteCount(), f2.attributeByteCount());
    }

    /**
     * Verifies that serializing a model with a null header results in an 80-byte header
     * filled with zeros in the output.
     *
     * @throws IOException if an I/O error occurs during serialization
     */
    @Test
    public void testSerializeWithEmptyHeader() throws IOException {
        StlModel model = new StlModel(null, List.of(), Unit.MILLIMETER);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel writeChannel = Channels.newChannel(outputStream);
        BinaryStlSerializer serializer = new BinaryStlSerializer();
        serializer.serialize(model, writeChannel);
        
        byte[] serializedData = outputStream.toByteArray();
        assertEquals(84, serializedData.length); // 80 bytes header + 4 bytes count
        
        for (int i = 0; i < 80; i++) {
            assertEquals(0, serializedData[i]);
        }
        assertEquals(0, serializedData[80]);
        assertEquals(0, serializedData[81]);
        assertEquals(0, serializedData[82]);
        assertEquals(0, serializedData[83]);
    }

    /**
     * Verifies that loading a binary STL file from resources, parsing it, and then
     * serializing it back results in exactly the same binary content as the original file.
     *
     * @throws IOException if an I/O error occurs during processing
     */
    @Test
    public void testSerializeBinaryResource() throws IOException {
        String resourcePath = "/stl/test-binary.stl";
        byte[] originalData;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            originalData = is.readAllBytes();
        }

        // Parse
        BinaryStlParser parser = new BinaryStlParser(Unit.MILLIMETER);
        StlModel model;
        try (ReadableByteChannel readChannel = Channels.newChannel(new ByteArrayInputStream(originalData))) {
            model = parser.parse(readChannel);
        }

        // Serialize
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (WritableByteChannel writeChannel = Channels.newChannel(outputStream)) {
            BinaryStlSerializer serializer = new BinaryStlSerializer();
            serializer.serialize(model, writeChannel);
        }

        // Verify binary identity
        byte[] serializedData = outputStream.toByteArray();
        assertArrayEquals(originalData, serializedData, "Serialized data does not match original binary resource");
    }

    /**
     * Verifies that an StlModel can be serialized to a channel in ASCII format
     * and then correctly deserialized back using AsciiStlParser.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testSerializeAscii() throws IOException {
        String headerName = "test-ascii";
        byte[] header = new byte[80];
        byte[] nameBytes = headerName.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(nameBytes, 0, header, 0, nameBytes.length);

        Vector3f normal = new Vector3f(0, 0, 1);
        Vector3f v1 = new Vector3f(0, 0, 0);
        Vector3f v2 = new Vector3f(1, 0, 0);
        Vector3f v3 = new Vector3f(0, 1, 0);
        StlFacet facet = new StlFacet(normal, v1, v2, v3, 0);
        StlModel originalModel = new StlModel(header, List.of(facet), Unit.MILLIMETER);

        // Serialize
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel writeChannel = Channels.newChannel(outputStream);
        StlSerializer serializer = new StlSerializer(StlSerializer.Format.ASCII);
        serializer.serialize(originalModel, writeChannel);

        // Deserialize
        byte[] serializedData = outputStream.toByteArray();
        ReadableByteChannel readChannel = Channels.newChannel(new ByteArrayInputStream(serializedData));
        AsciiStlParser parser = new AsciiStlParser(Unit.MILLIMETER);
        StlModel deserializedModel = parser.parse(readChannel);

        // Verify
        assertEquals(headerName, new String(deserializedModel.header(), java.nio.charset.StandardCharsets.US_ASCII).trim());
        assertEquals(originalModel.facetCount(), deserializedModel.facetCount());
        
        StlFacet f1 = originalModel.facets().get(0);
        StlFacet f2 = deserializedModel.facets().get(0);
        
        // Float precision in ASCII might differ slightly, but for these simple values it should be exact
        assertEquals(f1.normal(), f2.normal());
        assertEquals(f1.v1(), f2.v1());
        assertEquals(f1.v2(), f2.v2());
        assertEquals(f1.v3(), f2.v3());
    }

    /**
     * Verifies that the StlSerializer dispatcher defaults to BINARY.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testDispatcherDefault() throws IOException {
        StlModel model = new StlModel(new byte[80], List.of(), Unit.MILLIMETER);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel writeChannel = Channels.newChannel(outputStream);
        
        StlSerializer serializer = new StlSerializer();
        serializer.serialize(model, writeChannel);
        
        byte[] serializedData = outputStream.toByteArray();
        assertEquals(84, serializedData.length); // Binary format: 80 + 4
    }

    /**
     * Verifies that loading an ASCII STL file from resources, parsing it, and then
     * serializing it back in ASCII results in a model that is equivalent to the one
     * parsed from the original file.
     *
     * @throws IOException if an I/O error occurs during processing
     */
    @Test
    public void testSerializeAsciiResource() throws IOException {
        String resourcePath = "/stl/test-ascii.ast";
        byte[] originalData;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            originalData = is.readAllBytes();
        }

        // Parse original
        StlParser parser = new StlParser(Unit.MILLIMETER);
        StlModel originalModel;
        try (ReadableByteChannel readChannel = Channels.newChannel(new ByteArrayInputStream(originalData))) {
            originalModel = parser.parse(readChannel);
        }

        // Serialize to ASCII
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (WritableByteChannel writeChannel = Channels.newChannel(outputStream)) {
            StlSerializer serializer = new StlSerializer(StlSerializer.Format.ASCII);
            serializer.serialize(originalModel, writeChannel);
        }

        // Parse serialized
        byte[] serializedData = outputStream.toByteArray();
        StlModel deserializedModel;
        try (ReadableByteChannel readChannel = Channels.newChannel(new ByteArrayInputStream(serializedData))) {
            deserializedModel = parser.parse(readChannel);
        }

        // Verify models are equivalent
        assertEquals(originalModel.facetCount(), deserializedModel.facetCount());
        assertEquals(new String(originalModel.header(), java.nio.charset.StandardCharsets.US_ASCII).trim(),
                     new String(deserializedModel.header(), java.nio.charset.StandardCharsets.US_ASCII).trim());
        
        for (int i = 0; i < originalModel.facetCount(); i++) {
            StlFacet f1 = originalModel.facets().get(i);
            StlFacet f2 = deserializedModel.facets().get(i);
            
            // Use a small delta for float comparison due to ASCII precision
            float delta = 1e-5f;
            assertVectorEquals(f1.normal(), f2.normal(), delta);
            assertVectorEquals(f1.v1(), f2.v1(), delta);
            assertVectorEquals(f1.v2(), f2.v2(), delta);
            assertVectorEquals(f1.v3(), f2.v3(), delta);
        }
    }

    /**
     * Asserts that two Vector3f instances are equal within a given tolerance.
     *
     * @param expected the expected vector
     * @param actual the actual vector
     * @param delta the maximum difference between coordinates
     */
    private void assertVectorEquals(Vector3f expected, Vector3f actual, float delta) {
        assertEquals(expected.x(), actual.x(), delta);
        assertEquals(expected.y(), actual.y(), delta);
        assertEquals(expected.z(), actual.z(), delta);
    }
}
