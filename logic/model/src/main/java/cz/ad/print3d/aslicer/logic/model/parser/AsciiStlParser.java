package cz.ad.print3d.aslicer.logic.model.parser;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.stl.StlModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Implementation of ModelParser for ASCII STL files.
 */
public class AsciiStlParser implements ModelParser {
    /**
     * Parses the ASCII STL content from the given channel.
     *
     * @param channel the input ASCII channel
     * @return the parsed StlModel
     * @throws IOException if an I/O error occurs during parsing
     */
    @Override
    public StlModel parse(ReadableByteChannel channel) throws IOException {
        // Use a BufferedReader to read line by line. 
        // Note: this will wrap the channel, and it might read more than necessary.
        // But since we are parsing the whole file, it's fine.
        BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.US_ASCII));
        
        List<StlFacet> facets = new ArrayList<>();
        String headerName = "";
        
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.startsWith("solid")) {
                headerName = line.substring(5).trim();
            } else if (line.startsWith("facet normal")) {
                facets.add(parseFacet(line, reader));
            } else if (line.startsWith("endsolid")) {
                break;
            }
        }
        
        // For StlModel, we need a byte[80] header. 
        // We'll store the name in it, truncated or padded with zeros.
        byte[] header = new byte[80];
        byte[] nameBytes = headerName.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 80));
        
        return new StlModel(header, facets);
    }

    /**
     * Parses a single facet from the ASCII STL file.
     *
     * @param facetLine the line containing the facet normal
     * @param reader the reader to read subsequent lines for vertices
     * @return the parsed StlFacet
     * @throws IOException if an I/O error occurs or the facet format is invalid
     */
    private StlFacet parseFacet(String facetLine, BufferedReader reader) throws IOException {
        Vector3f normal = parseVector(facetLine.substring("facet normal".length()).trim());
        
        Vector3f v1 = null, v2 = null, v3 = null;
        
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("outer loop")) continue;
            if (line.startsWith("vertex")) {
                Vector3f v = parseVector(line.substring("vertex".length()).trim());
                if (v1 == null) v1 = v;
                else if (v2 == null) v2 = v;
                else if (v3 == null) v3 = v;
            } else if (line.startsWith("endloop")) {
                continue;
            } else if (line.startsWith("endfacet")) {
                break;
            }
        }
        
        if (v1 == null || v2 == null || v3 == null) {
            throw new IOException("Invalid ASCII STL facet: missing vertices");
        }
        
        return new StlFacet(normal, v1, v2, v3, 0);
    }

    /**
     * Parses a 3D vector from a string of space-separated coordinates.
     *
     * @param text the string containing the coordinates
     * @return a new Vector3f instance
     * @throws IOException if the coordinates cannot be parsed
     */
    private Vector3f parseVector(String text) throws IOException {
        Scanner scanner = new Scanner(text);
        scanner.useLocale(java.util.Locale.US);
        try {
            float x = scanner.nextFloat();
            float y = scanner.nextFloat();
            float z = scanner.nextFloat();
            return new Vector3f(x, y, z);
        } catch (Exception e) {
            throw new IOException("Failed to parse vector: " + text, e);
        } finally {
            scanner.close();
        }
    }
}
