package cz.ad.print3d.aslicer.logic.model.serializer;

import cz.ad.print3d.aslicer.logic.model.stl.StlModel;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * Interface for serializing 3D model data to a binary output channel.
 */
public interface ModelSerializer {
    /**
     * Serializes the given model into the specified binary output channel.
     *
     * @param model the model to serialize
     * @param channel the output binary channel
     * @throws IOException if an I/O error occurs during serialization
     */
    void serialize(StlModel model, WritableByteChannel channel) throws IOException;
}
