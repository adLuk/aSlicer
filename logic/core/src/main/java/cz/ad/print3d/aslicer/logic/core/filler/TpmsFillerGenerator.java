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
import cz.ad.print3d.aslicer.logic.core.WallTracer;

import java.util.List;

/**
 * TpmsFillerGenerator implements infill patterns based on Triply Periodic Minimal Surfaces (TPMS).
 * TPMS are surfaces that are periodic in three independent directions and have zero mean curvature.
 * In 3D printing, they are popular for infill because they provide high strength-to-weight ratios
 * and are often self-supporting.
 *
 * This generator currently supports:
 * - GYROID: sin(x)cos(y) + sin(y)cos(z) + sin(z)cos(x) = 0
 * - SCHWARZ_P: cos(x) + cos(y) + cos(z) = 0
 *
 * The generation algorithm:
 * 1. Determines the bounding box of the infill area.
 * 2. Evaluates the implicit TPMS function on a 2D grid at the current altitude (Y coordinate).
 * 3. Uses the Marching Squares algorithm to extract isolines (f(x, y, z) = 0).
 * 4. Clips the resulting paths to the infill boundary using Clipper2.
 */
public class TpmsFillerGenerator implements FillerGenerator {

    /**
     * Scaling factor to convert between model units and Clipper2's long units.
     */
    private static final double SCALE = 1000.0;

    /**
     * Supported TPMS patterns.
     */
    public enum TpmsType {
        /** Gyroid surface: sin(x)cos(y) + sin(y)cos(z) + sin(z)cos(x) = 0 */
        GYROID,
        /** Schwarz P surface: cos(x) + cos(y) + cos(z) = 0 */
        SCHWARZ_P
    }

    private TpmsType type = TpmsType.GYROID;

    /**
     * Sets the type of TPMS pattern to generate.
     * @param type The TPMS type.
     */
    public void setType(TpmsType type) {
        this.type = type;
    }

    /**
     * Generates TPMS infill paths for a given boundary.
     *
     * @param walls         The list of wall paths for the current layer.
     * @param originalLayer The original boundary paths of the layer.
     * @param nozzleSize    The width of the extrusion line.
     * @param spacing       The period of the TPMS surface (distance between repetitions in mm).
     * @param angle         The rotation angle for the pattern in degrees.
     * @param altitude      The vertical coordinate (Y) of the current layer.
     * @return A {@link Paths64} containing the clipped infill paths.
     */
    @Override
    public Paths64 generateInfill(List<Paths64> walls, Paths64 originalLayer, float nozzleSize, float spacing, float angle, float altitude) {
        Paths64 infillBoundary;
        if (walls == null || walls.isEmpty()) {
            infillBoundary = originalLayer;
        } else {
            // Infill is bounded by the innermost wall, offset inwards by half nozzle size.
            Paths64 innermostWall = walls.get(walls.size() - 1);
            infillBoundary = Clipper.InflatePaths(innermostWall, -0.5 * nozzleSize * SCALE, JoinType.Miter, EndType.Polygon);
        }

        if (infillBoundary == null || infillBoundary.isEmpty() || spacing <= 0) {
            return new Paths64();
        }

        Rect64 bounds = Clipper.GetBounds(infillBoundary);
        
        // Period L = spacing.
        double L = spacing;
        double omega = 2.0 * Math.PI / L;
        double angleRad = Math.toRadians(angle);
        
        // Grid resolution for Marching Squares. 
        // We use a fixed number of cells per period for consistent detail.
        double resolution = L / 16.0;
        if (resolution < 0.05) resolution = 0.05; // Safety floor

        Paths64 lines = new Paths64();
        
        // Convert bounds to model units
        double minX = bounds.left / SCALE;
        double maxX = bounds.right / SCALE;
        double minZ = bounds.top / SCALE;
        double maxZ = bounds.bottom / SCALE;

        // Add small margin
        minX -= resolution;
        maxX += resolution;
        minZ -= resolution;
        maxZ += resolution;

        int nx = (int) Math.ceil((maxX - minX) / resolution);
        int nz = (int) Math.ceil((maxZ - minZ) / resolution);

        // Evaluate grid of TPMS function values
        double[][] grid = new double[nx + 1][nz + 1];
        for (int i = 0; i <= nx; i++) {
            double x = minX + i * resolution;
            for (int j = 0; j <= nz; j++) {
                double z = minZ + j * resolution;
                grid[i][j] = evaluateTpms(x, altitude, z, omega, angleRad);
            }
        }

        // Apply Marching Squares to find isolines (zero-crossings)
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < nz; j++) {
                double v1 = grid[i][j];
                double v2 = grid[i + 1][j];
                double v3 = grid[i + 1][j + 1];
                double v4 = grid[i][j + 1];
                
                generateMarchingSquaresLines(i, j, v1, v2, v3, v4, minX, minZ, resolution, lines);
            }
        }

        // Clip the generated lines to the boundary
        Clipper64 clipper = new Clipper64();
        clipper.addOpenSubject(lines);
        clipper.addClip(infillBoundary);

        Paths64 clippedLines = new Paths64();
        Paths64 dummyClosed = new Paths64();
        clipper.Execute(ClipType.Intersection, FillRule.EvenOdd, dummyClosed, clippedLines);

        return clippedLines;
    }

    private double evaluateTpms(double x, double y, double z, double omega, double angleRad) {
        // Apply rotation around Y axis
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        
        double rx = x * cosA - z * sinA;
        double rz = x * sinA + z * cosA;

        double ox = rx * omega;
        double oy = y * omega;
        double oz = rz * omega;
        
        switch (type) {
            case GYROID:
                return Math.sin(ox) * Math.cos(oy) + Math.sin(oy) * Math.cos(oz) + Math.sin(oz) * Math.cos(ox);
            case SCHWARZ_P:
                return Math.cos(ox) + Math.cos(oy) + Math.cos(oz);
            default:
                return 0;
        }
    }

    private void generateMarchingSquaresLines(int i, int j, double v1, double v2, double v3, double v4, 
                                             double minX, double minZ, double res, Paths64 lines) {
        int mask = 0;
        if (v1 > 0) mask |= 1;
        if (v2 > 0) mask |= 2;
        if (v3 > 0) mask |= 4;
        if (v4 > 0) mask |= 8;

        if (mask == 0 || mask == 15) return;

        // p1..p4 are points on the four edges of the cell
        Point64 p1 = interpolate(i, j, i + 1, j, v1, v2, minX, minZ, res);
        Point64 p2 = interpolate(i + 1, j, i + 1, j + 1, v2, v3, minX, minZ, res);
        Point64 p3 = interpolate(i + 1, j + 1, i, j + 1, v3, v4, minX, minZ, res);
        Point64 p4 = interpolate(i, j + 1, i, j, v4, v1, minX, minZ, res);

        switch (mask) {
            case 1: case 14: addLine(p4, p1, lines); break;
            case 2: case 13: addLine(p1, p2, lines); break;
            case 3: case 12: addLine(p4, p2, lines); break;
            case 4: case 11: addLine(p2, p3, lines); break;
            case 5: addLine(p4, p3, lines); addLine(p1, p2, lines); break; 
            case 6: case 9: addLine(p1, p3, lines); break;
            case 7: case 8: addLine(p4, p3, lines); break;
            case 10: addLine(p1, p4, lines); addLine(p2, p3, lines); break;
        }
    }

    private Point64 interpolate(int i1, int j1, int i2, int j2, double v1, double v2, double minX, double minZ, double res) {
        double t = -v1 / (v2 - v1);
        double x = (minX + i1 * res) + (i2 - i1) * t * res;
        double z = (minZ + j1 * res) + (j2 - j1) * t * res;
        return new Point64(Math.round(x * SCALE), Math.round(z * SCALE));
    }

    private void addLine(Point64 start, Point64 end, Paths64 lines) {
        Path64 line = new Path64();
        line.add(start);
        line.add(end);
        lines.add(line);
    }
}
