package cz.ad.print3d.aslicer.logic.model.parser.stl;

import cz.ad.print3d.aslicer.logic.model.basic.Unit;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Factory and dispatcher for STL parsing. 
 * It detects whether the input is binary or ASCII STL and delegates to the appropriate parser.
 */
public class StlParser implements ModelParser<StlModel> {
    /**
     * Number of bytes to peek from the stream to detect the format (ASCII 'solid').
     */
    private static final int PEEK_SIZE = 5;
    /**
     * The unit to be used for the parsed STL model.
     */
    private final Unit unit;

    /**
     * Default constructor using millimeter as default unit.
     */
    public StlParser() {
        this(Unit.MILLIMETER);
    }

    /**
     * Creates a new STL parser with the specified unit.
     *
     * @param unit the measurement unit to associate with the model
     */
    public StlParser(final Unit unit) {
        this.unit = unit;
    }

    /**
     * Parses the STL content from the given channel.
     * It peeks at the beginning of the stream to determine if it is ASCII or binary format.
     *
     * @param channel the input STL channel
     * @return the parsed StlModel
     * @throws IOException if an I/O error occurs during parsing or format detection
     */
    @Override
    public StlModel parse(final ReadableByteChannel channel) throws IOException {
        // Wrap the channel in a BufferedInputStream so we can mark and reset to peek at the beginning.
        final BufferedInputStream bis = new BufferedInputStream(Channels.newInputStream(channel));
        bis.mark(PEEK_SIZE);

        final byte[] peek = new byte[PEEK_SIZE];
        final int read = bis.read(peek);
        bis.reset();

        if (read == PEEK_SIZE && 
            peek[0] == 's' && peek[1] == 'o' && peek[2] == 'l' && peek[3] == 'i' && peek[4] == 'd') {
            return new AsciiStlParser(unit).parse(Channels.newChannel(bis));
        } else {
            return new BinaryStlParser(unit).parse(Channels.newChannel(bis));
        }
    }
}
