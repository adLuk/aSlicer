package cz.ad.print3d.aslicer.logic.model.parser;

import cz.ad.print3d.aslicer.logic.model.Model;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Interface for parsing 3D model data from a binary input channel.
 *
 * @param <T> the type of model to parse
 */
public interface ModelParser<T extends Model> {
    /**
     * Parses the binary content from the given channel and returns a model.
     *
     * @param channel the input binary channel
     * @return the parsed model
     * @throws IOException if an I/O error occurs during parsing
     */
    T parse(ReadableByteChannel channel) throws IOException;
}
