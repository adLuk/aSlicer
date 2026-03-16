/*
 * aSlicer - 3D model processing tool.
 * Copyright (C) 2026 cz.ad.print3d.aslicer contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cz.ad.print3d.aslicer.logic.model.serializer.stl;

import cz.ad.print3d.aslicer.logic.model.basic.Vector3f;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlFacet;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import cz.ad.print3d.aslicer.logic.model.serializer.ModelSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Implementation of ModelSerializer for ASCII STL files.
 */
public class AsciiStlSerializer implements ModelSerializer<StlModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsciiStlSerializer.class);

    /**
     * Serializes the given model to the specified channel in ASCII STL format.
     *
     * @param model the model to serialize
     * @param channel the output ASCII channel
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serialize(final StlModel model, final WritableByteChannel channel) throws IOException {
        LOGGER.info("Starting ASCII STL serialization");
        final PrintWriter writer = new PrintWriter(Channels.newWriter(channel, StandardCharsets.US_ASCII));

        String solidName = "aSlicer";
        if (model.header() != null) {
            final String headerContent = new String(model.header(), StandardCharsets.US_ASCII).trim();
            if (!headerContent.isEmpty()) {
                solidName = headerContent;
            }
        }
        LOGGER.debug("Solid name: {}", solidName);

        writer.printf(Locale.US, "solid %s%n", solidName);

        int count = 0;
        for (final StlFacet facet : model.facets()) {
            LOGGER.trace("Serializing facet {}", count++);
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
        LOGGER.info("Finished ASCII STL serialization, wrote {} facets", count);
        // We do NOT close the writer because it would close the underlying channel,
        // which might be managed elsewhere. But flush is necessary.
    }

    /**
     * Writes a vertex in ASCII format.
     *
     * @param writer the writer to use
     * @param vertex the vertex to write
     */
    private void writeVertex(final PrintWriter writer, final Vector3f vertex) {
        writer.printf(Locale.US, "      vertex %e %e %e%n", vertex.x(), vertex.y(), vertex.z());
    }
}
