package cz.ad.print3d.aslicer.logic.model.serializer;

import cz.ad.print3d.aslicer.logic.model.stl.StlModel;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * STL Serializer dispatcher that supports both binary and ASCII formats.
 * By default, it uses the binary format.
 */
public class StlSerializer implements ModelSerializer {

    /**
     * Enumeration of supported STL formats.
     */
    public enum Format {
        BINARY,
        ASCII
    }

    private Format format;

    /**
     * Creates a new StlSerializer with the default BINARY format.
     */
    public StlSerializer() {
        this(Format.BINARY);
    }

    /**
     * Creates a new StlSerializer with the specified format.
     *
     * @param format the format to use for serialization
     */
    public StlSerializer(Format format) {
        this.format = format;
    }

    /**
     * Gets the current serialization format.
     *
     * @return the current format
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Sets the serialization format.
     *
     * @param format the format to use
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Serializes the model using the configured format.
     *
     * @param model the model to serialize
     * @param channel the output channel
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serialize(StlModel model, WritableByteChannel channel) throws IOException {
        ModelSerializer serializer;
        if (format == Format.ASCII) {
            serializer = new AsciiStlSerializer();
        } else {
            serializer = new BinaryStlSerializer();
        }
        serializer.serialize(model, channel);
    }
}
