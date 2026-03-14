package cz.ad.print3d.aslicer.logic.model.serializer.mf3;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.mf3.ThreeMfModel;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;

/**
 * Tests for {@link ThreeMfSerializer}.
 */
public class ThreeMfSerializerTest {

    /**
     * Verifies that the serializer can handle a ThreeMfModel.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testSerialize() throws IOException {
        ThreeMfSerializer serializer = new ThreeMfSerializer();
        ThreeMfModel model = new ThreeMfModel(Collections.emptyMap(), Collections.emptyList(), Unit.MILLIMETER);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel channel = Channels.newChannel(outputStream);
        
        // This should not throw any exceptions.
        serializer.serialize(model, channel);
    }
}
