package cz.ad.print3d.aslicer.logic.model.serializer;

import cz.ad.print3d.aslicer.logic.model.Model;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * Interface for serializing 3D model data to a binary output channel.
 *
 * @param <T> the type of model to serialize
 */
public interface ModelSerializer<T extends Model> {
    /**
     * Serializes the given model into the specified binary output channel.
     *
     * @param model the model to serialize
     * @param channel the output binary channel
     * @throws IOException if an I/O error occurs during serialization
     */
    void serialize(T model, WritableByteChannel channel) throws IOException;
}
