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
package cz.ad.print3d.aslicer.logic.core;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;
import clipper2.Clipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Slicer implements 3D model slicing logic using Clipper2-java.
 * It processes GDX Model data and produces 2D polygons for each layer.
 * <br/>
 * The slicing process consists of:
 * 1. Extracting all triangles from the GDX model.
 * 2. Calculating the bounding box to determine layer ranges.
 * 3. For each layer height:
 *    a. Intersecting each triangle with the horizontal plane (X-Z plane at height Y).
 *    b. Collecting the resulting line segments.
 *    c. Connecting segments into closed loops (polygons).
 *    d. Using Clipper2 to process and clean the polygons.
 */
public class Slicer {

    private static final double SCALE = 1000.0; // Scale for Clipper2 (it uses long coordinates)

    /**
     * Slices a GDX Model into layers.
     *
     * @param model       The GDX model to slice.
     * @param layerHeight The height of each layer.
     * @return A list of layers, where each layer is a list of paths (polygons).
     */
    public List<Paths64> slice(Model model, float layerHeight) {
        // 1. Get all triangles from the model
        List<Triangle> triangles = extractTriangles(model);
        if (triangles.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Determine bounding box
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (Triangle tri : triangles) {
            minY = Math.min(minY, Math.min(tri.v1.y, Math.min(tri.v2.y, tri.v3.y)));
            maxY = Math.max(maxY, Math.max(tri.v1.y, Math.max(tri.v2.y, tri.v3.y)));
        }

        List<Paths64> layers = new ArrayList<>();
        // Slice at the middle of each layer for better representation
        for (float sliceY = minY + layerHeight / 2; sliceY < maxY; sliceY += layerHeight) {
            Paths64 layerPaths = sliceLayer(triangles, sliceY);
            if (!layerPaths.isEmpty()) {
                // Ensure correct winding order (outer CCW, holes CW)
                layerPaths = Clipper.Union(layerPaths, clipper2.core.FillRule.EvenOdd);
                layers.add(layerPaths);
            }
        }

        return layers;
    }

    private List<Triangle> extractTriangles(Model model) {
        List<Triangle> triangles = new ArrayList<>();
        for (MeshPart part : model.meshParts) {
            final int posOffset = part.mesh.getVertexAttributes().getOffset(VertexAttributes.Usage.Position);
            final int vertexSize = part.mesh.getVertexSize() / 4; // in floats

            FloatBuffer vertices = part.mesh.getVerticesBuffer();
            ShortBuffer indices = part.mesh.getIndicesBuffer();

            int numIndices = part.size;
            int offset = part.offset;

            for (int i = 0; i < numIndices; i += 3) {
                int i1 = indices.get(offset + i) & 0xFFFF;
                int i2 = indices.get(offset + i + 1) & 0xFFFF;
                int i3 = indices.get(offset + i + 2) & 0xFFFF;

                Triangle tri = new Triangle();
                tri.v1 = new Vector3(vertices.get(i1 * vertexSize + posOffset),
                                     vertices.get(i1 * vertexSize + posOffset + 1),
                                     vertices.get(i1 * vertexSize + posOffset + 2));
                tri.v2 = new Vector3(vertices.get(i2 * vertexSize + posOffset),
                                     vertices.get(i2 * vertexSize + posOffset + 1),
                                     vertices.get(i2 * vertexSize + posOffset + 2));
                tri.v3 = new Vector3(vertices.get(i3 * vertexSize + posOffset),
                                     vertices.get(i3 * vertexSize + posOffset + 1),
                                     vertices.get(i3 * vertexSize + posOffset + 2));
                triangles.add(tri);
            }
        }
        return triangles;
    }

    private Paths64 sliceLayer(List<Triangle> triangles, float sliceY) {
        List<LineSegment> segments = new ArrayList<>();
        for (Triangle tri : triangles) {
            LineSegment seg = intersectTriangle(tri, sliceY);
            if (seg != null) {
                segments.add(seg);
            }
        }

        // Connect segments into loops
        return connectSegments(segments);
    }

    private LineSegment intersectTriangle(Triangle tri, float sliceY) {
        // Check if triangle spans sliceY
        int below = 0, above = 0;
        if (tri.v1.y < sliceY) below++; else if (tri.v1.y > sliceY) above++;
        if (tri.v2.y < sliceY) below++; else if (tri.v2.y > sliceY) above++;
        if (tri.v3.y < sliceY) below++; else if (tri.v3.y > sliceY) above++;

        // Handle cases where vertices are exactly on sliceY
        // A robust implementation should handle this carefully to avoid duplicates
        // For now, we only consider triangles that cross the plane.
        if (above == 0 || below == 0) return null;

        // Find the two edges that intersect the plane
        List<Vector3> intersections = new ArrayList<>();
        checkAndAddIntersection(tri.v1, tri.v2, sliceY, intersections);
        checkAndAddIntersection(tri.v2, tri.v3, sliceY, intersections);
        checkAndAddIntersection(tri.v3, tri.v1, sliceY, intersections);

        if (intersections.size() >= 2) {
            return new LineSegment(intersections.get(0), intersections.get(1));
        }
        return null;
    }

    private void checkAndAddIntersection(Vector3 p1, Vector3 p2, float sliceY, List<Vector3> intersections) {
        if ((p1.y < sliceY && p2.y > sliceY) || (p1.y > sliceY && p2.y < sliceY)) {
            float t = (sliceY - p1.y) / (p2.y - p1.y);
            intersections.add(new Vector3(
                    p1.x + t * (p2.x - p1.x),
                    sliceY,
                    p1.z + t * (p2.z - p1.z)
            ));
        } else if (p1.y == sliceY) {
            intersections.add(new Vector3(p1.x, sliceY, p1.z));
        }
    }

    private Paths64 connectSegments(List<LineSegment> segments) {
        if (segments.isEmpty()) return new Paths64();

        Paths64 result = new Paths64();
        List<Path64> paths = new ArrayList<>();
        
        while (!segments.isEmpty()) {
            Path64 path = new Path64();
            LineSegment current = segments.remove(0);
            path.add(toPoint64(current.p1));
            Vector3 lastPoint = current.p2;
            path.add(toPoint64(lastPoint));

            boolean foundNext = true;
            while (foundNext) {
                foundNext = false;
                for (int i = 0; i < segments.size(); i++) {
                    LineSegment next = segments.get(i);
                    if (isClose(lastPoint, next.p1)) {
                        lastPoint = next.p2;
                        path.add(toPoint64(lastPoint));
                        segments.remove(i);
                        foundNext = true;
                        break;
                    } else if (isClose(lastPoint, next.p2)) {
                        lastPoint = next.p1;
                        path.add(toPoint64(lastPoint));
                        segments.remove(i);
                        foundNext = true;
                        break;
                    }
                }
            }
            if (path.size() > 2) {
                paths.add(path);
            }
        }

        // Use Clipper to clean and union paths
        for (Path64 path : paths) {
            result.add(path);
        }
        
        return Clipper.SimplifyPaths(result, 1.0);
    }

    private Point64 toPoint64(Vector3 v) {
        return new Point64((long) (v.x * SCALE), (long) (v.z * SCALE));
    }

    private boolean isClose(Vector3 v1, Vector3 v2) {
        return v1.dst2(v2) < 0.000001f;
    }

    private static class Triangle {
        Vector3 v1, v2, v3;
    }

    private static class LineSegment {
        Vector3 p1, p2;
        LineSegment(Vector3 p1, Vector3 p2) { this.p1 = p1; this.p2 = p2; }
    }
}
