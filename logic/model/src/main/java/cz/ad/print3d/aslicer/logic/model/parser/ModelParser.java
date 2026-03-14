package cz.ad.print3d.aslicer.logic.model.parser;

import cz.ad.print3d.aslicer.logic.model.stl.StlModel;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Interface for parsing 3D model data from a binary input channel.
 */
public interface ModelParser {
    /**
     * Parses the binary content from the given channel and returns an StlModel.
     *
     * @param channel the input binary channel
     * @return the parsed StlModel
     * @throws IOException if an I/O error occurs during parsing
     */
    StlModel parse(ReadableByteChannel channel) throws IOException;
}
