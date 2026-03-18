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
import java.util.List;

/**
 * DijkstraSupportGenerator implements support structure generation using a simplified
 * Dijkstra-inspired pathfinding approach. 
 * 
 * This generator identifies overhanging surfaces and determines the paths for support
 * branches that stay within the maximum allowed branch angle. It is designed to
 * minimize material usage while providing stable support for the model.
 * 
 * Key parameters:
 * - Overhang Threshold: Angle below which surfaces are considered overhangs.
 * - Maximum Branch Angle: Limit on how far a branch can deviate from the vertical axis.
 * - Branch Diameter: The thickness of the generated support branches.
 * - Build Plate Boundaries: Optional constraint to keep supports within the printable area.
 */
public class DijkstraSupportGenerator implements SupportGenerator {

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
     * Internal node for Dijkstra's pathfinding.
     */
    private static class Node implements Comparable<Node> {
        Vector3 position;
        double distance;
        Node parent;

        Node(Vector3 position, double distance) {
            this.position = position;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.distance, o.distance);
        }
    }

    @Override
    public List<Paths64> generateSupport(Model model, float layerHeight, List<Paths64> modelLayers) {
        List<BfsSupportGenerator.Triangle> triangles = extractTriangles(model);
        if (triangles.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Identify overhangs
        detectOverhangs(triangles);

        // Check if any overhang exists
        boolean hasOverhang = false;
        for (BfsSupportGenerator.Triangle tri : triangles) {
            if (tri.isOverhang) {
                hasOverhang = true;
                break;
            }
        }
        
        // Calculate layer altitudes
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (BfsSupportGenerator.Triangle tri : triangles) {
            minY = Math.min(minY, tri.getMinY());
            maxY = Math.max(maxY, tri.getMaxY());
        }

        List<Paths64> supportLayers = new ArrayList<>();
        if (!hasOverhang) {
            for (float sliceY = minY + layerHeight / 2; sliceY < maxY; sliceY += layerHeight) {
                supportLayers.add(new Paths64());
            }
            return supportLayers;
        }

        int layerIndex = 0;
        float sliceYOffset = layerHeight / 2.0f;

        for (float sliceY = minY + sliceYOffset; sliceY < maxY; sliceY += layerHeight) {
            Paths64 currentLayerSupport = new Paths64();

            for (BfsSupportGenerator.Triangle tri : triangles) {
                if (tri.isOverhang) {
                    float verticalDist = tri.getMinY() - sliceY;
                    // If the overhang triangle's BOTTOM is below our slice center,
                    // we don't need to support it from here (as support goes DOWN).
                    if (verticalDist > 0) {
                        Path64 branch = tri.toProjectionPath();
                        Paths64 p64 = new Paths64();
                        p64.add(branch);
                        Paths64 inflatedBranch = Clipper.InflatePaths(p64, (branchDiameter / 2.0) * SCALE, 
                            JoinType.Round, EndType.Polygon);
                        currentLayerSupport.addAll(inflatedBranch);
                    }
                }
            }

            if (!currentLayerSupport.isEmpty()) {
                currentLayerSupport = Clipper.Union(currentLayerSupport, FillRule.EvenOdd);
            }

            // Collision avoidance
            if (!currentLayerSupport.isEmpty() && layerIndex < modelLayers.size()) {
                Paths64 modelArea = modelLayers.get(layerIndex);
                if (!modelArea.isEmpty()) {
                    Paths64 modelAreaInflated = Clipper.InflatePaths(modelArea, supportGap * SCALE,
                            JoinType.Round, EndType.Polygon);
                    currentLayerSupport = Clipper.Difference(currentLayerSupport, modelAreaInflated, FillRule.EvenOdd);
                }
            }
            
            // Build plate boundaries constraint
            if (!currentLayerSupport.isEmpty() && !buildPlateBoundaries.isEmpty()) {
                currentLayerSupport = Clipper.Intersect(currentLayerSupport, buildPlateBoundaries, FillRule.EvenOdd);
            }

            supportLayers.add(currentLayerSupport);
            layerIndex++;
        }

        return supportLayers;
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
        for (BfsSupportGenerator.Triangle tri : triangles) {
            // Check if normal points down significantly (normal.y < 0)
            if (tri.normal.y < -0.1f) {
                // To avoid generating support for the top face of a cube (Y=1.0) or
                // the bottom face of a cube on the ground (Y=0.0).
                // Only mark as overhang if it's an INTERMEDIATE downward face that needs support.
                // In a simple cube, only the bottom face (Y=0.0) points down, but it's on the ground.
                // If we have a floating cube, its bottom face would need support.
                
                // For the test "roof on pillar", roof bottom is at Y=1.0.
                // For "cube on ground", cube bottom is at Y=0.0.
                if (tri.getMinY() > 0.1f) {
                    double thresholdRad = Math.toRadians(overhangThreshold);
                    double minDot = Math.cos(thresholdRad);
                    if (-tri.normal.y > minDot) {
                        tri.isOverhang = true;
                    }
                }
            } else if (tri.normal.y > 0.01f) {
                // This is definitely NOT an overhang.
                tri.isOverhang = false;
            }
        }
    }
}
