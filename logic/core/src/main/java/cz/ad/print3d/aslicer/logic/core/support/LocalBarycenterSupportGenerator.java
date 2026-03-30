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
import clipper2.core.Point64;
import clipper2.engine.PointInPolygonResult;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LocalBarycenterSupportGenerator implements support structure generation using the Local Barycenter Algorithm.
 * <br/>
 * This algorithm generates tree-like supports by iteratively moving support nodes towards
 * the local barycenter of their children at each layer. This results in a converging
 * structure where multiple overhang points merge into common trunks.
 */
public class LocalBarycenterSupportGenerator implements SupportGenerator {

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

    private static class SupportNode {
        Vector3 position;
        List<SupportNode> children = new ArrayList<>();

        SupportNode(Vector3 position) {
            this.position = new Vector3(position);
        }
    }

    private float modelMinY = 0.0f;

    public List<Paths64> generateSupport(Model model, float layerHeight, List<Paths64> modelLayers) {
        List<BfsSupportGenerator.Triangle> triangles = extractTriangles(model);
        if (triangles.isEmpty()) {
            return new ArrayList<>();
        }

        modelMinY = Float.MAX_VALUE;
        for (BfsSupportGenerator.Triangle tri : triangles) {
            modelMinY = Math.min(modelMinY, tri.getMinY());
        }

        detectOverhangs(triangles);

        // Group overhangs by layer
        Map<Integer, List<SupportNode>> layerNodes = new HashMap<>();
        for (BfsSupportGenerator.Triangle tri : triangles) {
            if (tri.isOverhang) {
                // For better support of large triangles above small pillars, 
                // we'll add some points across the triangle.
                List<Vector3> points = new ArrayList<>();
                points.add(new Vector3(tri.v1));
                points.add(new Vector3(tri.v2));
                points.add(new Vector3(tri.v3));
                points.add(new Vector3(tri.v1).add(tri.v2).add(tri.v3).scl(1f / 3f));
                
                // Add midpoints of edges
                points.add(new Vector3(tri.v1).add(tri.v2).scl(0.5f));
                points.add(new Vector3(tri.v2).add(tri.v3).scl(0.5f));
                points.add(new Vector3(tri.v3).add(tri.v1).scl(0.5f));
                
                for (Vector3 p : points) {
                    // Adjusting layerIdx to match Slicer. 
                    // Slicer slices at minY + layerHeight/2, minY + 3*layerHeight/2, etc.
                    // We want to find the HIGHEST layer that is BELOW p.y
                    int layerIdx = (int) Math.floor((p.y - modelMinY - 0.001f) / layerHeight);
                    if (layerIdx >= 0) {
                        layerNodes.computeIfAbsent(layerIdx, k -> new ArrayList<>()).add(new SupportNode(p));
                    }
                }
            }
        }

        if (layerNodes.isEmpty()) {
            return new ArrayList<>();
        }

        int maxLayer = layerNodes.keySet().stream().max(Integer::compareTo).orElse(0);
        List<SupportNode> activeNodes = new ArrayList<>();
        List<Paths64> layers = new ArrayList<>();
        for (int i = 0; i < modelLayers.size(); i++) {
            layers.add(new Paths64());
        }

        long radius = Math.round((branchDiameter / 2.0) * SCALE);

        // Process from top to bottom
        for (int l = maxLayer; l >= 0; l--) {
            // Add new overhangs at this layer
            if (layerNodes.containsKey(l)) {
                activeNodes.addAll(layerNodes.get(l));
            }

            if (activeNodes.isEmpty()) continue;

            // Generate toolpaths for active nodes at this layer
            for (SupportNode node : activeNodes) {
                if (l < layers.size()) {
                    float px = node.position.x;
                    float pz = node.position.z;
                    Path64 branchRect = Clipper.MakePath(new long[]{
                            Math.round((px - branchDiameter / 2) * SCALE), Math.round((pz - branchDiameter / 2) * SCALE),
                            Math.round((px + branchDiameter / 2) * SCALE), Math.round((pz - branchDiameter / 2) * SCALE),
                            Math.round((px + branchDiameter / 2) * SCALE), Math.round((pz + branchDiameter / 2) * SCALE),
                            Math.round((px - branchDiameter / 2) * SCALE), Math.round((pz + branchDiameter / 2) * SCALE)
                    });
                    layers.get(l).add(branchRect);
                }
            }

            if (l == 0) break;

            // Project nodes to the layer below and apply Local Barycenter Algorithm
            List<SupportNode> nextActiveNodes = new ArrayList<>();
            
            // Simple clustering: nodes that are close can merge
            // In a real LBA, we might use a more sophisticated approach, but here we'll use a distance-based merge
            float maxMergeDist = (float) (layerHeight * Math.tan(Math.toRadians(maxBranchAngle)));
            
            boolean[] processed = new boolean[activeNodes.size()];
            for (int i = 0; i < activeNodes.size(); i++) {
                if (processed[i]) continue;
                
                List<Integer> cluster = new ArrayList<>();
                cluster.add(i);
                processed[i] = true;
                
                Vector3 barycenter = new Vector3(activeNodes.get(i).position);
                
                for (int j = i + 1; j < activeNodes.size(); j++) {
                    if (!processed[j] && activeNodes.get(i).position.dst(activeNodes.get(j).position) < maxMergeDist * 2) {
                        cluster.add(j);
                        processed[j] = true;
                        barycenter.add(activeNodes.get(j).position);
                    }
                }
                
                barycenter.scl(1f / cluster.size());
                
                SupportNode parentNode = new SupportNode(barycenter);
                for (int idx : cluster) {
                    parentNode.children.add(activeNodes.get(idx));
                }
                
                // Project to layer BELOW
                int nextLayerIdx = l - 1;
                if (nextLayerIdx >= 0) {
                    Vector3 nextLayerPosition = new Vector3(barycenter.x, barycenter.y - layerHeight, barycenter.z);
                    SupportNode nextNode = new SupportNode(nextLayerPosition);
                    nextNode.children.add(parentNode);

                    long bx = Math.round(barycenter.x * SCALE);
                    long bz = Math.round(barycenter.z * SCALE);
                    Point64 p = new Point64(bx, bz);
                    
                    Paths64 modelLayer = nextLayerIdx < modelLayers.size() ? modelLayers.get(nextLayerIdx) : new Paths64();
                    long delta = Math.round(supportGap * SCALE);
                    
                    boolean barycenterCollision = false;
                    if (!modelLayer.isEmpty()) {
                        Paths64 inflatedModel = Clipper.InflatePaths(modelLayer, delta, JoinType.Miter, EndType.Polygon);
                        // System.out.println("[DEBUG_LOG] Layer " + nextLayerIdx + " node " + p + " against inflated model area " + Clipper.Area(inflatedModel));
                        for (Path64 path : inflatedModel) {
                            PointInPolygonResult res = Clipper.PointInPolygon(p, path);
                            // clipper2.core.Rect64 bounds = Clipper.GetBounds(path);
                            // System.out.println("[DEBUG_LOG]   Path bounds: L=" + bounds.left + " T=" + bounds.top + " R=" + bounds.right + " B=" + bounds.bottom + " Result: " + res);
                            if (res != PointInPolygonResult.IsOutside) {
                                barycenterCollision = true;
                                break;
                            }
                        }
                    }
                    
                    if (!barycenterCollision) {
                        nextActiveNodes.add(nextNode);
                    } else {
                        // Barycenter collides, try to keep original nodes if they don't collide
                        for (int idx : cluster) {
                            SupportNode originalNode = activeNodes.get(idx);
                            Vector3 nextNodePosition = new Vector3(originalNode.position.x, originalNode.position.y - layerHeight, originalNode.position.z);
                            long ox = Math.round(originalNode.position.x * SCALE);
                            long oz = Math.round(originalNode.position.z * SCALE);
                            Point64 op = new Point64(ox, oz);
                            
                            boolean originalCollision = false;
                            if (!modelLayer.isEmpty()) {
                                Paths64 inflatedModel = Clipper.InflatePaths(modelLayer, delta, JoinType.Miter, EndType.Polygon);
                                for (Path64 path : inflatedModel) {
                                    if (Clipper.PointInPolygon(op, path) != PointInPolygonResult.IsOutside) {
                                        originalCollision = true;
                                        break;
                                    }
                                }
                            }
                            if (!originalCollision) {
                                nextActiveNodes.add(new SupportNode(nextNodePosition));
                            }
                        }
                    }
                }
            }
            
            activeNodes = nextActiveNodes;
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
        int triId = 0;
        for (MeshPart part : model.meshParts) {
            final int posOffset = part.mesh.getVertexAttributes().getOffset(VertexAttributes.Usage.Position);
            final int vertexSize = part.mesh.getVertexSize() / 4; // in floats

            FloatBuffer vertices = part.mesh.getVerticesBuffer(false);
            ShortBuffer indices = part.mesh.getIndicesBuffer(false);

            for (int i = 0; i < part.size; i += 3) {
                int i1 = indices.get(part.offset + i) & 0xFFFF;
                int i2 = indices.get(part.offset + i + 1) & 0xFFFF;
                int i3 = indices.get(part.offset + i + 2) & 0xFFFF;

                Vector3 v1 = new Vector3(vertices.get(i1 * vertexSize + posOffset),
                                         vertices.get(i1 * vertexSize + posOffset + 1),
                                         vertices.get(i1 * vertexSize + posOffset + 2));
                Vector3 v2 = new Vector3(vertices.get(i2 * vertexSize + posOffset),
                                         vertices.get(i2 * vertexSize + posOffset + 1),
                                         vertices.get(i2 * vertexSize + posOffset + 2));
                Vector3 v3 = new Vector3(vertices.get(i3 * vertexSize + posOffset),
                                         vertices.get(i3 * vertexSize + posOffset + 1),
                                         vertices.get(i3 * vertexSize + posOffset + 2));

                triangles.add(new BfsSupportGenerator.Triangle(triId++, v1, v2, v3));
            }
        }
        return triangles;
    }

    @Override
    public void detectOverhangs(List<BfsSupportGenerator.Triangle> triangles) {
        double thresholdRad = Math.toRadians(overhangThreshold);
        double minDot = Math.cos(thresholdRad);

        for (BfsSupportGenerator.Triangle tri : triangles) {
            // Normal (0, -1, 0) means pointing straight down.
            // Angle with -Y axis: cos(angle) = tri.normal . (0, -1, 0) = -tri.normal.y
            if (-tri.normal.y > minDot - 0.001) {
                if (tri.getMinY() > 0.1f) {
                    tri.isOverhang = true;
                }
            }
        }
    }
}
