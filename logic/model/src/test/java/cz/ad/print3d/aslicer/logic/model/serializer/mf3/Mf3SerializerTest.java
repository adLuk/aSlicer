package cz.ad.print3d.aslicer.logic.model.serializer.mf3;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.mf3.relationship.Mf3Relationships;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;

/**
 * Tests for {@link Mf3Serializer}.
 */
public class Mf3SerializerTest {

    /**
     * Verifies that the serializer can handle a Mf3Model.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testSerialize() throws IOException {
        Mf3Serializer serializer = new Mf3Serializer();
        Mf3Model model = new Mf3Model(Collections.emptyMap(), Collections.emptyList(), Unit.MILLIMETER, new Mf3Relationships());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel(outputStream);
        
        // This should not throw any exceptions.
        serializer.serialize(model, channel);
    }
}
