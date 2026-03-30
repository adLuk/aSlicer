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
package cz.ad.print3d.aslicer.logic.core.support;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;
import clipper2.Clipper;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.*;

/**
 * BfsSupportGenerator implements support structure generation by identifying
 * connected overhang regions using a Geometric Breadth-First Search (BFS).
 * <br/>
 * This generator projects identified overhang areas downward and merges them
 * using boolean operations (Clipper2) to form optimized support structures.
 * <br/>
 * Collision avoidance is performed at each layer to maintain a specific gap
 * between the support structure and the model's walls.
 */
public class BfsSupportGenerator implements SupportGenerator {

    private static final double SCALE = 1000.0;
    private float overhangThreshold = 45.0f; // degrees from horizontal
    private float supportGap = 0.2f; // distance between support and model
    private float maxBranchAngle = 45.0f;
    private float branchDiameter = 1.0f;
    private float collisionDetail = 0.1f;
    private Paths64 buildPlateBoundaries = new Paths64();

    /**
     * Internal representation of a triangle with normal and adjacency support.
     * Stores the geometric data and the results of the overhang analysis.
     */
    public static class Triangle {
        /** Unique ID of the triangle. */
        public int id;
        /** Vertex positions. */
        public Vector3 v1, v2, v3;
        /** Surface normal vector. */
        public Vector3 normal;
        /** Flag indicating if this triangle is part of an overhang region. */
        public boolean isOverhang;
        /** Unique IDs of the vertices (after merging close vertices). */
        public int[] vertexIds;
        /** List of adjacent triangle IDs (sharing an edge). */
        public List<Integer> neighbors = new ArrayList<>();

        public Triangle(int id, Vector3 v1, Vector3 v2, Vector3 v3) {
            this.id = id;
            this.v1 = new Vector3(v1);
            this.v2 = new Vector3(v2);
            this.v3 = new Vector3(v3);
            calculateNormal();
        }

        /**
         * Calculates the normal vector of the triangle based on its vertices.
         * Assumes counter-clockwise winding order.
         */
        private void calculateNormal() {
            Vector3 edge1 = new Vector3(v2).sub(v1);
            Vector3 edge2 = new Vector3(v3).sub(v1);
            normal = edge1.crs(edge2).nor();
        }

        /**
         * Gets the minimum vertical coordinate (Y) of the triangle.
         * @return Minimum Y coordinate.
         */
        public float getMinY() {
            return Math.min(v1.y, Math.min(v2.y, v3.y));
        }

        /**
         * Gets the maximum vertical coordinate (Y) of the triangle.
         * @return Maximum Y coordinate.
         */
        public float getMaxY() {
            return Math.max(v1.y, Math.max(v2.y, v3.y));
        }

        /**
         * Projects the triangle onto the horizontal plane (X-Z) and converts it to a Clipper2 Path.
         * @return A Path64 representing the 2D projection of the triangle.
         */
        public Path64 toProjectionPath() {
            Path64 path = new Path64();
            path.add(new Point64(Math.round(v1.x * SCALE), Math.round(v1.z * SCALE)));
            path.add(new Point64(Math.round(v2.x * SCALE), Math.round(v2.z * SCALE)));
            path.add(new Point64(Math.round(v3.x * SCALE), Math.round(v3.z * SCALE)));
            return path;
        }
    }

    @Override
    public void setOverhangThreshold(float degrees) {
        this.overhangThreshold = degrees;
    }

    @Override
    public void setSupportGap(float gap) {
        this.supportGap = gap;
    }

    @Override
    public void setMaxBranchAngle(float degrees) {
        this.maxBranchAngle = degrees;
    }

    @Override
    public void setBranchDiameter(float diameter) {
        this.branchDiameter = diameter;
    }

    @Override
    public void setCollisionDetail(float detail) {
        this.collisionDetail = detail;
    }

    @Override
    public void setBuildPlateBoundaries(Paths64 boundaries) {
        this.buildPlateBoundaries = boundaries;
    }

    @Override
    public List<Paths64> generateSupport(Model model, float layerHeight, List<Paths64> modelLayers) {
        List<Triangle> triangles = extractTriangles(model);
        if (triangles.isEmpty()) {
            return new ArrayList<>();
        }

        buildAdjacency(triangles);
        detectOverhangs(triangles);
        List<List<Triangle>> islands = clusterOverhangs(triangles);

        // Calculate layer altitudes
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (Triangle tri : triangles) {
            minY = Math.min(minY, tri.getMinY());
            maxY = Math.max(maxY, tri.getMaxY());
        }

        List<Paths64> supportLayers = new ArrayList<>();
        int layerIndex = 0;
        // Slice at the middle of each layer for better representation
        for (float sliceY = minY + layerHeight / 2; sliceY < maxY; sliceY += layerHeight) {
            Paths64 currentLayerSupport = new Paths64();

            // 1. Identify which islands/triangles are above this layer
            for (List<Triangle> island : islands) {
                Paths64 islandProjection = new Paths64();
                for (Triangle tri : island) {
                    if (tri.getMinY() > sliceY) {
                        islandProjection.add(tri.toProjectionPath());
                    }
                }

                if (!islandProjection.isEmpty()) {
                    // Greedy Merging: Union all triangles in the island
                    Paths64 mergedIsland = Clipper.Union(islandProjection, FillRule.EvenOdd);
                    currentLayerSupport.addAll(mergedIsland);
                }
            }

            // Greedy Merging: Union all islands for this layer
            if (!currentLayerSupport.isEmpty()) {
                currentLayerSupport = Clipper.Union(currentLayerSupport, FillRule.EvenOdd);
            }

            // Avoid collisions with the model at this layer
            if (!currentLayerSupport.isEmpty() && layerIndex < modelLayers.size()) {
                Paths64 modelArea = modelLayers.get(layerIndex);
                if (!modelArea.isEmpty()) {
                    // Inflate model area to maintain gap
                    Paths64 modelAreaInflated = Clipper.InflatePaths(modelArea, supportGap * SCALE,
                            JoinType.Round, EndType.Polygon);
                    currentLayerSupport = Clipper.Difference(currentLayerSupport, modelAreaInflated, FillRule.EvenOdd);
                }
            }

            supportLayers.add(currentLayerSupport);
            layerIndex++;
        }

        return supportLayers;
    }

    /**
     * Extracts all triangles from a LibGDX Model.
     *
     * @param model The model to extract from.
     * @return A list of extracted Triangle objects.
     */
    private List<Triangle> extractTriangles(Model model) {
        List<Triangle> triangles = new ArrayList<>();
        int triangleId = 0;
        for (MeshPart part : model.meshParts) {
            final int posOffset = part.mesh.getVertexAttributes().getOffset(VertexAttributes.Usage.Position);
            final int vertexSize = part.mesh.getVertexSize() / 4;

            FloatBuffer vertices = part.mesh.getVerticesBuffer(false);
            ShortBuffer indices = part.mesh.getIndicesBuffer(false);

            int numIndices = part.size;
            int offset = part.offset;

            for (int i = 0; i < numIndices; i += 3) {
                int i1 = indices.get(offset + i) & 0xFFFF;
                int i2 = indices.get(offset + i + 1) & 0xFFFF;
                int i3 = indices.get(offset + i + 2) & 0xFFFF;

                Vector3 v1 = new Vector3(vertices.get(i1 * vertexSize + posOffset),
                                         vertices.get(i1 * vertexSize + posOffset + 1),
                                         vertices.get(i1 * vertexSize + posOffset + 2));
                Vector3 v2 = new Vector3(vertices.get(i2 * vertexSize + posOffset),
                                         vertices.get(i2 * vertexSize + posOffset + 1),
                                         vertices.get(i2 * vertexSize + posOffset + 2));
                Vector3 v3 = new Vector3(vertices.get(i3 * vertexSize + posOffset),
                                         vertices.get(i3 * vertexSize + posOffset + 1),
                                         vertices.get(i3 * vertexSize + posOffset + 2));
                triangles.add(new Triangle(triangleId++, v1, v2, v3));
            }
        }
        return triangles;
    }

    /**
     * Builds the mesh adjacency graph by identifying shared edges.
     * Vertices that are extremely close (within 0.001 units) are merged.
     *
     * @param triangles The list of triangles to analyze.
     */
    private void buildAdjacency(List<Triangle> triangles) {
        // Map to merge vertices that are very close
        Map<VertexKey, Integer> vertexMap = new HashMap<>();
        int nextVertexId = 0;

        for (Triangle tri : triangles) {
            tri.vertexIds = new int[3];
            tri.vertexIds[0] = getVertexId(tri.v1, vertexMap, nextVertexId);
            if (tri.vertexIds[0] == nextVertexId) nextVertexId++;
            tri.vertexIds[1] = getVertexId(tri.v2, vertexMap, nextVertexId);
            if (tri.vertexIds[1] == nextVertexId) nextVertexId++;
            tri.vertexIds[2] = getVertexId(tri.v3, vertexMap, nextVertexId);
            if (tri.vertexIds[2] == nextVertexId) nextVertexId++;
        }

        // Map from edge to triangles
        Map<EdgeKey, List<Integer>> edgeMap = new HashMap<>();
        for (Triangle tri : triangles) {
            addEdge(tri.id, tri.vertexIds[0], tri.vertexIds[1], edgeMap);
            addEdge(tri.id, tri.vertexIds[1], tri.vertexIds[2], edgeMap);
            addEdge(tri.id, tri.vertexIds[2], tri.vertexIds[0], edgeMap);
        }

        // Fill neighbors
        for (List<Integer> sharingTriangles : edgeMap.values()) {
            if (sharingTriangles.size() > 1) {
                for (int i = 0; i < sharingTriangles.size(); i++) {
                    for (int j = i + 1; j < sharingTriangles.size(); j++) {
                        int id1 = sharingTriangles.get(i);
                        int id2 = sharingTriangles.get(j);
                        triangles.get(id1).neighbors.add(id2);
                        triangles.get(id2).neighbors.add(id1);
                    }
                }
            }
        }
    }

    @Override
    public void detectOverhangs(List<Triangle> triangles) {
        // Overhang if normal angle with -Y is small.
        // n . (0, -1, 0) = -n.y
        // We want angle < threshold, so cos(angle) > cos(threshold)
        double thresholdRad = Math.toRadians(overhangThreshold);
        double minDot = Math.cos(thresholdRad);

        for (Triangle tri : triangles) {
            if (-tri.normal.y > minDot) {
                // To avoid generating support for the bottom face of a cube on the ground (Y=0),
                // we only mark it as overhang if it's significantly above Y=0.
                if (tri.getMinY() > 0.1f) {
                    tri.isOverhang = true;
                }
            }
        }
    }

    /**
     * Groups connected overhang triangles into "islands" using a Breadth-First Search (BFS).
     *
     * @param triangles The list of triangles to cluster.
     * @return A list of islands, where each island is a list of connected overhang Triangle objects.
     */
    private List<List<Triangle>> clusterOverhangs(List<Triangle> triangles) {
        List<List<Triangle>> islands = new ArrayList<>();
        boolean[] visited = new boolean[triangles.size()];

        for (int i = 0; i < triangles.size(); i++) {
            Triangle tri = triangles.get(i);
            if (tri.isOverhang && !visited[i]) {
                List<Triangle> island = new ArrayList<>();
                Queue<Integer> queue = new LinkedList<>();
                queue.add(i);
                visited[i] = true;

                while (!queue.isEmpty()) {
                    int currentId = queue.poll();
                    Triangle currentTri = triangles.get(currentId);
                    island.add(currentTri);

                    for (int neighborId : currentTri.neighbors) {
                        if (triangles.get(neighborId).isOverhang && !visited[neighborId]) {
                            visited[neighborId] = true;
                            queue.add(neighborId);
                        }
                    }
                }
                islands.add(island);
            }
        }
        return islands;
    }

    private int getVertexId(Vector3 v, Map<VertexKey, Integer> map, int currentNextId) {
        VertexKey key = new VertexKey(v);
        return map.computeIfAbsent(key, k -> currentNextId);
    }

    private void addEdge(int triId, int v1, int v2, Map<EdgeKey, List<Integer>> edgeMap) {
        EdgeKey key = new EdgeKey(v1, v2);
        edgeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(triId);
    }

    private static class VertexKey {
        private final int x, y, z;
        private static final float PRECISION = 1000f;

        public VertexKey(Vector3 v) {
            this.x = Math.round(v.x * PRECISION);
            this.y = Math.round(v.y * PRECISION);
            this.z = Math.round(v.z * PRECISION);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VertexKey)) return false;
            VertexKey vertexKey = (VertexKey) o;
            return x == vertexKey.x && y == vertexKey.y && z == vertexKey.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    private static class EdgeKey {
        private final int v1, v2;

        public EdgeKey(int i1, int i2) {
            this.v1 = Math.min(i1, i2);
            this.v2 = Math.max(i1, i2);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EdgeKey)) return false;
            EdgeKey edgeKey = (EdgeKey) o;
            return v1 == edgeKey.v1 && v2 == edgeKey.v2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(v1, v2);
        }
    }
}
