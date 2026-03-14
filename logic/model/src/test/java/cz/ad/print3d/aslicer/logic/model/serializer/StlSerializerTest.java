package cz.ad.print3d.aslicer.logic.model.serializer;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.parser.StlParser;
import cz.ad.print3d.aslicer.logic.model.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.stl.StlModel;
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
 * Tests for StlSerializer.
 */
public class StlSerializerTest {

    /**
     * Verifies that an StlModel can be serialized to a channel and then correctly deserialized
     * back to an equivalent StlModel using StlParser.
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
        StlModel originalModel = new StlModel(header, List.of(facet));

        // Serialize
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel writeChannel = Channels.newChannel(outputStream);
        StlSerializer serializer = new StlSerializer();
        serializer.serialize(originalModel, writeChannel);

        // Deserialize
        byte[] serializedData = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedData);
        ReadableByteChannel readChannel = Channels.newChannel(inputStream);
        StlParser parser = new StlParser();
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
        StlModel model = new StlModel(null, List.of());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel writeChannel = Channels.newChannel(outputStream);
        StlSerializer serializer = new StlSerializer();
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
        String resourcePath = "/test-binary.stl";
        byte[] originalData;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Resource not found: " + resourcePath);
            originalData = is.readAllBytes();
        }

        // Parse
        StlParser parser = new StlParser();
        StlModel model;
        try (ReadableByteChannel readChannel = Channels.newChannel(new ByteArrayInputStream(originalData))) {
            model = parser.parse(readChannel);
        }

        // Serialize
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (WritableByteChannel writeChannel = Channels.newChannel(outputStream)) {
            StlSerializer serializer = new StlSerializer();
            serializer.serialize(model, writeChannel);
        }

        // Verify binary identity
        byte[] serializedData = outputStream.toByteArray();
        assertArrayEquals(originalData, serializedData, "Serialized data does not match original binary resource");
    }
}
