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

import clipper2.Clipper;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MstSupportGenerator implements support structure generation using a Minimum Spanning Tree (MST) approach.
 *
 * This algorithm treats overhang points as nodes in a graph and finds the MST to connect them
 * into a tree-like structure. This minimizes the total length of the support structure
 * while ensuring that every overhang is connected to either another supported point
 * or the build plate.
 */
public class MstSupportGenerator implements SupportGenerator {

    private static final double SCALE = 1000.0;
    private float overhangThreshold = 45.0f;
    private float supportGap = 0.2f;
    private float maxBranchAngle = 45.0f;
    private float branchDiameter = 1.0f;
    private float collisionDetail = 0.1f;
    private Paths64 buildPlateBoundaries = new Paths64();

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

    /**
     * Internal node for MST graph.
     */
    private static class Node {
        int id;
        Vector3 position;

        Node(int id, Vector3 position) {
            this.id = id;
            this.position = position;
        }
    }

    /**
     * Internal edge for MST graph.
     */
    private static class Edge implements Comparable<Edge> {
        int from, to;
        double weight;

        Edge(int from, int to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        @Override
        public int compareTo(Edge o) {
            return Double.compare(this.weight, o.weight);
        }
    }

    /**
     * Disjoint Set Union (DSU) for Kruskal's algorithm.
     */
    private static class UnionFind {
        int[] parent;

        UnionFind(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }

        int find(int i) {
            if (parent[i] == i) return i;
            return parent[i] = find(parent[i]);
        }

        void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) parent[rootI] = rootJ;
        }
    }

    @Override
    public List<Paths64> generateSupport(Model model, float layerHeight, List<Paths64> modelLayers) {
        List<BfsSupportGenerator.Triangle> triangles = extractTriangles(model);
        if (triangles.isEmpty()) {
            return new ArrayList<>();
        }

        detectOverhangs(triangles);

        List<Node> nodes = new ArrayList<>();
        for (BfsSupportGenerator.Triangle tri : triangles) {
            if (tri.isOverhang) {
                Vector3 centroid = new Vector3(tri.v1).add(tri.v2).add(tri.v3).scl(1f / 3f);
                nodes.add(new Node(nodes.size(), centroid));
            }
        }

        if (nodes.isEmpty()) {
            return new ArrayList<>();
        }

        // Add virtual ground nodes for each overhang
        int nOverhangs = nodes.size();
        for (int i = 0; i < nOverhangs; i++) {
            Vector3 groundPos = new Vector3(nodes.get(i).position);
            groundPos.y = 0;
            nodes.add(new Node(nodes.size(), groundPos));
        }

        // Virtual common ground node to connect all ground nodes with 0 cost
        int rootNodeId = nodes.size();
        nodes.add(new Node(rootNodeId, new Vector3(0, -100, 0))); // Dummy position

        List<Edge> edges = new ArrayList<>();
        
        // Connect overhang points to each other
        for (int i = 0; i < nOverhangs; i++) {
            for (int j = i + 1; j < nOverhangs; j++) {
                Node a = nodes.get(i);
                Node b = nodes.get(j);
                double dist = a.position.dst(b.position);
                
                // Weight calculation: prefer vertical connections, avoid too steep angles
                float dy = Math.abs(a.position.y - b.position.y);
                float dxz = (float) Math.sqrt(Math.pow(a.position.x - b.position.x, 2) + Math.pow(a.position.z - b.position.z, 2));
                
                float angle = (float) Math.toDegrees(Math.atan2(dxz, dy));
                if (angle <= maxBranchAngle) {
                   edges.add(new Edge(i, j, dist));
                }
            }
        }

        // Connect overhang points to their corresponding ground points
        for (int i = 0; i < nOverhangs; i++) {
            Node p = nodes.get(i);
            Node g = nodes.get(i + nOverhangs);
            double dist = p.position.dst(g.position);
            edges.add(new Edge(i, i + nOverhangs, dist));
        }

        // Connect all ground points to virtual root with 0 cost
        for (int i = 0; i < nOverhangs; i++) {
            edges.add(new Edge(i + nOverhangs, rootNodeId, 0.0));
        }

        // Find MST
        Collections.sort(edges);
        UnionFind uf = new UnionFind(nodes.size());
        List<Edge> mst = new ArrayList<>();
        for (Edge edge : edges) {
            if (uf.find(edge.from) != uf.find(edge.to)) {
                uf.union(edge.from, edge.to);
                mst.add(edge);
            }
        }

        // Convert MST edges to layer paths
        List<Paths64> layers = new ArrayList<>();
        for (int i = 0; i < modelLayers.size(); i++) {
            layers.add(new Paths64());
        }

        long radius = Math.round((branchDiameter / 2.0) * SCALE);
        for (Edge edge : mst) {
            // Skip edges to dummy root
            if (edge.from == rootNodeId || edge.to == rootNodeId) continue;
            // Skip edges between ground points if they are both virtual
            if (edge.from >= nOverhangs && edge.to >= nOverhangs) continue;

            Node a = nodes.get(edge.from);
            Node b = nodes.get(edge.to);

            // Interpolate edge through layers
            float minY = Math.min(a.position.y, b.position.y);
            float maxY = Math.max(a.position.y, b.position.y);

            for (int l = 0; l < modelLayers.size(); l++) {
                float layerY = (l + 0.5f) * layerHeight;
                if (layerY >= minY && layerY <= maxY) {
                    float t = (maxY > minY) ? (layerY - a.position.y) / (b.position.y - a.position.y) : 0;
                    float px = a.position.x + t * (b.position.x - a.position.x);
                    float pz = a.position.z + t * (b.position.z - a.position.z);

                    Path64 branchCircle = Clipper.MakePath(new long[]{
                            Math.round((px - branchDiameter / 2) * SCALE), Math.round((pz - branchDiameter / 2) * SCALE),
                            Math.round((px + branchDiameter / 2) * SCALE), Math.round((pz - branchDiameter / 2) * SCALE),
                            Math.round((px + branchDiameter / 2) * SCALE), Math.round((pz + branchDiameter / 2) * SCALE),
                            Math.round((px - branchDiameter / 2) * SCALE), Math.round((pz + branchDiameter / 2) * SCALE)
                    });
                    layers.get(l).add(branchCircle);
                }
            }
        }

        // Final processing: Union branches and subtract model with gap
        List<Paths64> result = new ArrayList<>();
        long delta = Math.round(supportGap * SCALE);

        for (int i = 0; i < layers.size(); i++) {
            Paths64 layerPaths = Clipper.Union(layers.get(i), FillRule.EvenOdd);
            if (delta > 0 && !modelLayers.get(i).isEmpty()) {
                Paths64 inflatedModel = Clipper.InflatePaths(modelLayers.get(i), delta, JoinType.Miter, EndType.Polygon);
                layerPaths = Clipper.Difference(layerPaths, inflatedModel, FillRule.EvenOdd);
            }
            if (!buildPlateBoundaries.isEmpty()) {
                layerPaths = Clipper.Intersect(layerPaths, buildPlateBoundaries, FillRule.EvenOdd);
            }
            result.add(layerPaths);
        }

        return result;
    }

    private List<BfsSupportGenerator.Triangle> extractTriangles(Model model) {
        List<BfsSupportGenerator.Triangle> triangles = new ArrayList<>();
        int triangleId = 0;
        for (MeshPart part : model.meshParts) {
            final int posOffset = part.mesh.getVertexAttributes().getOffset(VertexAttributes.Usage.Position);
            final int vertexSize = part.mesh.getVertexSize() / 4;
            FloatBuffer vertices = part.mesh.getVerticesBuffer();
            ShortBuffer indices = part.mesh.getIndicesBuffer();
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
                triangles.add(new BfsSupportGenerator.Triangle(triangleId++, v1, v2, v3));
            }
        }
        return triangles;
    }

    @Override
    public void detectOverhangs(List<BfsSupportGenerator.Triangle> triangles) {
        double thresholdRad = Math.toRadians(overhangThreshold);
        double minDot = Math.cos(thresholdRad);

        for (BfsSupportGenerator.Triangle tri : triangles) {
            // Normal (0, -1, 0) means pointing straight down
            if (-tri.normal.y > minDot) {
                // Ignore faces very close to ground
                if (tri.getMinY() > 0.1f) {
                    tri.isOverhang = true;
                }
            }
        }
    }
}
