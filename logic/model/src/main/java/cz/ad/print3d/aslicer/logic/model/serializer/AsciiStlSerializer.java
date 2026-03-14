package cz.ad.print3d.aslicer.logic.model.serializer;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.stl.StlModel;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Implementation of ModelSerializer for ASCII STL files.
 */
public class AsciiStlSerializer implements ModelSerializer {

    /**
     * Serializes the given model to the specified channel in ASCII STL format.
     *
     * @param model the model to serialize
     * @param channel the output ASCII channel
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serialize(StlModel model, WritableByteChannel channel) throws IOException {
        PrintWriter writer = new PrintWriter(Channels.newWriter(channel, StandardCharsets.US_ASCII));
        
        String solidName = "aSlicer";
        if (model.header() != null) {
            solidName = new String(model.header(), StandardCharsets.US_ASCII).trim();
            if (solidName.isEmpty()) {
                solidName = "aSlicer";
            }
        }
        
        writer.printf(Locale.US, "solid %s%n", solidName);
        
        for (StlFacet facet : model.facets()) {
            writer.printf(Locale.US, "  facet normal %e %e %e%n", 
                facet.normal().x(), facet.normal().y(), facet.normal().z());
            writer.println("    outer loop");
            writeVertex(writer, facet.v1());
            writeVertex(writer, facet.v2());
            writeVertex(writer, facet.v3());
            writer.println("    endloop");
            writer.println("  endfacet");
        }
        
        writer.printf(Locale.US, "endsolid %s%n", solidName);
        writer.flush();
        // We do NOT close the writer because it would close the underlying channel,
        // which might be managed elsewhere. But flush is necessary.
    }

    /**
     * Writes a vertex in ASCII format.
     *
     * @param writer the writer to use
     * @param vertex the vertex to write
     */
    private void writeVertex(PrintWriter writer, Vector3f vertex) {
        writer.printf(Locale.US, "      vertex %e %e %e%n", vertex.x(), vertex.y(), vertex.z());
    }
}
