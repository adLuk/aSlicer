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
package cz.ad.print3d.aslicer.logic.core.filler;

import clipper2.Clipper;
import clipper2.core.ClipType;
import clipper2.core.FillRule;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;
import clipper2.engine.Clipper64;
import clipper2.offset.EndType;
import clipper2.offset.JoinType;

import java.util.ArrayList;
import java.util.List;

/**
 * AdaptiveCubicFillerGenerator implements the Adaptive Cubic Infill algorithm.
 * This algorithm generates a 3D cubic infill pattern whose density varies
 * depending on the distance to the model's boundary.
 *
 * The implementation uses an Octree to manage the different subdivision levels:
 * 1. The root node of the Octree covers the entire model volume.
 * 2. Nodes are subdivided if they intersect the model's boundary (perimeters).
 * 3. This results in smaller cells (higher density) near surfaces and larger cells
 *    (lower density) in the interior.
 * 4. For each layer altitude, we identify the Octree leaf nodes that intersect
 *    the current layer plane and generate the cubic pattern at the appropriate scale.
 */
public class AdaptiveCubicFillerGenerator implements FillerGenerator {

    private static final double SCALE = 1000.0;
    private int maxDepth = 3; // Default max subdivision level

    private List<Paths64> layers;
    private float firstAltitude;
    private float layerHeight;
    private OctreeNode root;

    /**
     * Sets the maximum subdivision depth for the Octree.
     * Higher values allow for more drastic density changes but increase precalculation time.
     *
     * @param depth The maximum depth (0 to 8 recommended).
     */
    public void setMaxDepth(int depth) {
        this.maxDepth = depth;
    }

    /**
     * Provides the layers of the model and builds the Octree structure.
     * This method must be called before {@link #generateInfill}.
     *
     * @param layers        List of layer boundaries (bottom-to-top).
     * @param firstAltitude The altitude of the first layer in the list.
     * @param layerHeight   The height increment between layers.
     */
    public void setLayers(List<Paths64> layers, float firstAltitude, float layerHeight) {
        this.layers = layers;
        this.firstAltitude = firstAltitude;
        this.layerHeight = layerHeight;
        this.root = null;

        if (layers == null || layers.isEmpty()) {
            return;
        }

        // Determine global bounding box in scaled units
        long minX = Long.MAX_VALUE, minY = Long.MAX_VALUE, minZ = Math.round(firstAltitude * SCALE);
        long maxX = Long.MIN_VALUE, maxY = Long.MIN_VALUE, maxZ = Math.round((firstAltitude + (layers.size() - 1) * layerHeight) * SCALE);

        for (Paths64 layer : layers) {
            Rect64 bounds = Clipper.GetBounds(layer);
            minX = Math.min(minX, bounds.left);
            minY = Math.min(minY, bounds.top);
            maxX = Math.max(maxX, bounds.right);
            maxY = Math.max(maxY, bounds.bottom);
        }

        // Ensure root is large enough and centered
        long width = maxX - minX;
        long height = maxY - minY;
        long depth = maxZ - minZ;
        long size = Math.max(width, Math.max(height, depth));

        root = new OctreeNode(minX - 100, minY - 100, minZ - 100, size + 200, 0);
        subdivide(root);
    }

    private void subdivide(OctreeNode node) {
        if (node.depth >= maxDepth) {
            return;
        }

        // Check if node intersects any boundary in its Z range
        if (!intersectsBoundary(node)) {
            return;
        }

        node.subdivide();
        for (OctreeNode child : node.children) {
            subdivide(child);
        }
    }

    private boolean intersectsBoundary(OctreeNode node) {
        // Find layers in the altitude range of this node
        int iMin = (int) Math.max(0, (node.minZ / SCALE - firstAltitude) / layerHeight - 1);
        int iMax = (int) Math.min(layers.size() - 1, (node.maxZ() / SCALE - firstAltitude) / layerHeight + 1);

        for (int i = iMin; i <= iMax; i++) {
            Paths64 layer = layers.get(i);
            if (intersectsXY(layer, node)) {
                return true;
            }
        }
        return false;
    }

    private boolean intersectsXY(Paths64 paths, OctreeNode node) {
        Rect64 nodeRect = new Rect64(node.minX, node.minY, node.maxX(), node.maxY());
        for (Path64 path : paths) {
            // Check if any point is inside
            for (Point64 pt : path) {
                if (nodeRect.Intersects(new Rect64(pt.x, pt.y, pt.x, pt.y))) {
                    return true;
                }
            }
            // Check if any segment intersects the node boundaries
            for (int j = 0; j < path.size(); j++) {
                Point64 p1 = path.get(j);
                Point64 p2 = path.get((j + 1) % path.size());
                if (segmentIntersectsRect(p1, p2, nodeRect)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean segmentIntersectsRect(Point64 p1, Point64 p2, Rect64 rect) {
        // Cohen-Sutherland or simple bounding box check + intersection
        if (rect.Intersects(new Rect64(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.max(p1.x, p2.x), Math.max(p1.y, p2.y)))) {
            // More rigorous check could be added here
            return true;
        }
        return false;
    }

    @Override
    public Paths64 generateInfill(List<Paths64> walls, Paths64 originalLayer, float nozzleSize, float spacing, float angle, float altitude) {
        Paths64 infillBoundary;
        if (walls == null || walls.isEmpty()) {
            infillBoundary = originalLayer;
        } else {
            Paths64 innermostWall = walls.get(walls.size() - 1);
            infillBoundary = Clipper.InflatePaths(innermostWall, -0.5 * nozzleSize * SCALE, JoinType.Miter, EndType.Polygon);
        }

        if (infillBoundary == null || infillBoundary.isEmpty() || root == null || spacing <= 0) {
            return new Paths64();
        }

        Paths64 allInfillLines = new Paths64();
        generateInfillRecursive(root, altitude, spacing, infillBoundary, allInfillLines);

        return allInfillLines;
    }

    private void generateInfillRecursive(OctreeNode node, float altitude, float baseSpacing, Paths64 infillBoundary, Paths64 outLines) {
        long z = Math.round(altitude * SCALE);
        if (z < node.minZ || z > node.maxZ()) {
            return;
        }

        if (node.isLeaf()) {
            // Determine density for this node
            double nodeSpacing = baseSpacing * Math.pow(2, maxDepth - node.depth);
            Paths64 lines = generateCubicLines(node, altitude, (float) nodeSpacing);
            
            // Clip lines to node's 2D rect and infillBoundary
            Paths64 nodeRect = new Paths64();
            Path64 rectPath = new Path64();
            rectPath.add(new Point64(node.minX, node.minY));
            rectPath.add(new Point64(node.maxX(), node.minY));
            rectPath.add(new Point64(node.maxX(), node.maxY()));
            rectPath.add(new Point64(node.minX, node.maxY()));
            nodeRect.add(rectPath);

            Paths64 combinedClip = Clipper.Intersect(nodeRect, infillBoundary, FillRule.EvenOdd);
            if (combinedClip.isEmpty()) {
                return;
            }

            Clipper64 clipper = new Clipper64();
            clipper.addOpenSubject(lines);
            clipper.addClip(combinedClip);

            Paths64 clippedLines = new Paths64();
            Paths64 dummy = new Paths64();
            clipper.Execute(ClipType.Intersection, FillRule.EvenOdd, dummy, clippedLines);
            outLines.addAll(clippedLines);
        } else {
            for (OctreeNode child : node.children) {
                generateInfillRecursive(child, altitude, baseSpacing, infillBoundary, outLines);
            }
        }
    }

    private Paths64 generateCubicLines(OctreeNode node, float altitude, float spacing) {
        Paths64 lines = new Paths64();
        long scaledSpacing = Math.round(spacing * SCALE);
        if (scaledSpacing <= 0) return lines;

        long zShift = Math.round(altitude * SCALE);

        // Three sets of lines at 0, 120, 240 degrees with different Z shifts
        lines.addAll(generateParallelLines(node, scaledSpacing, 0, zShift));
        lines.addAll(generateParallelLines(node, scaledSpacing, 120, -zShift));
        lines.addAll(generateParallelLines(node, scaledSpacing, 240, 0));

        return lines;
    }

    private Paths64 generateParallelLines(OctreeNode node, long spacing, double angleDeg, long shift) {
        Paths64 lines = new Paths64();
        double angleRad = Math.toRadians(angleDeg);
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);

        // Line equation: x * cosA + y * sinA = k * spacing + shift
        // We need to find k range that covers the node's rectangle
        double[] cornersX = {node.minX, node.maxX(), node.maxX(), node.minX};
        double[] cornersY = {node.minY, node.minY, node.maxY(), node.maxY()};
        
        double minKValue = Double.MAX_VALUE;
        double maxKValue = -Double.MAX_VALUE;
        
        for (int i = 0; i < 4; i++) {
            double v = cornersX[i] * cosA + cornersY[i] * sinA - shift;
            minKValue = Math.min(minKValue, v);
            maxKValue = Math.max(maxKValue, v);
        }

        long minK = (long) Math.floor(minKValue / spacing);
        long maxK = (long) Math.ceil(maxKValue / spacing);

        for (long k = minK; k <= maxK; k++) {
            double constant = k * (double)spacing + shift;
            // Intersect line with node rectangle (node.minX, node.minY) to (node.maxX, node.maxY)
            // Use Liang-Barsky or similar for clipping to rect
            Point64 p1 = intersectLineWithRect(cosA, sinA, constant, node);
            Point64 p2 = intersectLineWithRectEnd(cosA, sinA, constant, node);
            
            if (p1 != null && p2 != null && (p1.x != p2.x || p1.y != p2.y)) {
                Path64 line = new Path64();
                line.add(p1);
                line.add(p2);
                lines.add(line);
            }
        }

        return lines;
    }

    private Point64 intersectLineWithRect(double cosA, double sinA, double c, OctreeNode node) {
        // Find intersections of x*cosA + y*sinA = c with node boundaries
        // This is a simplified version
        List<Point64> pts = new ArrayList<>();
        
        // x = minX -> y = (c - minX*cosA) / sinA
        if (Math.abs(sinA) > 0.0001) {
            double y = (c - node.minX * cosA) / sinA;
            if (y >= node.minY && y <= node.maxY()) pts.add(new Point64(node.minX, Math.round(y)));
            
            y = (c - node.maxX() * cosA) / sinA;
            if (y >= node.minY && y <= node.maxY()) pts.add(new Point64(node.maxX(), Math.round(y)));
        }
        
        // y = minY -> x = (c - minY*sinA) / cosA
        if (Math.abs(cosA) > 0.0001) {
            double x = (c - node.minY * sinA) / cosA;
            if (x >= node.minX && x <= node.maxX()) pts.add(new Point64(Math.round(x), node.minY));
            
            x = (c - node.maxY() * sinA) / cosA;
            if (x >= node.minX && x <= node.maxX()) pts.add(new Point64(Math.round(x), node.maxY()));
        }

        return pts.isEmpty() ? null : pts.get(0);
    }

    private Point64 intersectLineWithRectEnd(double cosA, double sinA, double c, OctreeNode node) {
        List<Point64> pts = new ArrayList<>();
        if (Math.abs(sinA) > 0.0001) {
            double y = (c - node.minX * cosA) / sinA;
            if (y >= node.minY && y <= node.maxY()) pts.add(new Point64(node.minX, Math.round(y)));
            y = (c - node.maxX() * cosA) / sinA;
            if (y >= node.minY && y <= node.maxY()) pts.add(new Point64(node.maxX(), Math.round(y)));
        }
        if (Math.abs(cosA) > 0.0001) {
            double x = (c - node.minY * sinA) / cosA;
            if (x >= node.minX && x <= node.maxX()) pts.add(new Point64(Math.round(x), node.minY));
            x = (c - node.maxY() * sinA) / cosA;
            if (x >= node.minX && x <= node.maxX()) pts.add(new Point64(Math.round(x), node.maxY()));
        }
        return pts.size() < 2 ? null : pts.get(pts.size() - 1);
    }

    private static class OctreeNode {
        long minX, minY, minZ;
        long size;
        int depth;
        OctreeNode[] children;

        OctreeNode(long minX, long minY, long minZ, long size, int depth) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.size = size;
            this.depth = depth;
        }

        long maxX() { return minX + size; }
        long maxY() { return minY + size; }
        long maxZ() { return minZ + size; }

        boolean isLeaf() { return children == null; }

        void subdivide() {
            children = new OctreeNode[8];
            long s = size / 2;
            for (int i = 0; i < 8; i++) {
                long dx = ((i & 1) == 0) ? 0 : s;
                long dy = ((i & 2) == 0) ? 0 : s;
                long dz = ((i & 4) == 0) ? 0 : s;
                children[i] = new OctreeNode(minX + dx, minY + dy, minZ + dz, s, depth + 1);
            }
        }
    }
}
