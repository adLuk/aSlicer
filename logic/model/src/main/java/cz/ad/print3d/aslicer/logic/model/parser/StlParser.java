package cz.ad.print3d.aslicer.logic.model.parser;

import cz.ad.print3d.aslicer.logic.model.stl.StlModel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Factory and dispatcher for STL parsing. 
 * It detects whether the input is binary or ASCII STL and delegates to the appropriate parser.
 */
public class StlParser implements ModelParser {

    private static final int PEEK_SIZE = 5;

    /**
     * Parses the STL content from the given channel.
     * It peeks at the beginning of the stream to determine if it is ASCII or binary format.
     *
     * @param channel the input STL channel
     * @return the parsed StlModel
     * @throws IOException if an I/O error occurs during parsing or format detection
     */
    @Override
    public StlModel parse(ReadableByteChannel channel) throws IOException {
        // Wrap the channel in a BufferedInputStream so we can mark and reset to peek at the beginning.
        BufferedInputStream bis = new BufferedInputStream(Channels.newInputStream(channel));
        bis.mark(PEEK_SIZE);
        
        byte[] peek = new byte[PEEK_SIZE];
        int read = bis.read(peek);
        bis.reset();

        if (read == PEEK_SIZE && 
            peek[0] == 's' && peek[1] == 'o' && peek[2] == 'l' && peek[3] == 'i' && peek[4] == 'd') {
            return new AsciiStlParser().parse(Channels.newChannel(bis));
        } else {
            return new BinaryStlParser().parse(Channels.newChannel(bis));
        }
    }
}
