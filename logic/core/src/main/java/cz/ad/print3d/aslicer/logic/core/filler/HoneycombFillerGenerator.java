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

import java.util.List;

/**
 * HoneycombFillerGenerator implements a 2D hexagonal infill pattern.
 * The pattern is generated as a set of continuous zigzag paths that overlap
 * at certain segments to form a regular honeycomb grid.
 *
 * This implementation is efficient for 3D printing as it produces long paths
 * and minimizes retractions. The distance between parallel walls of the
 * hexagons is controlled by the spacing parameter.
 */
public class HoneycombFillerGenerator implements FillerGenerator {

    private static final double SCALE = 1000.0;

    /**
     * Generates honeycomb infill paths for a given boundary.
     *
     * @param walls         The list of wall paths for the current layer.
     * @param originalLayer The original boundary paths of the layer.
     * @param nozzleSize    The width of the extrusion line.
     * @param spacing       The distance between parallel walls of the hexagons.
     * @param angle         The rotation angle of the pattern in degrees.
     * @param altitude      The vertical coordinate of the current layer.
     * @return A {@link Paths64} containing the clipped honeycomb paths as open paths.
     */
    @Override
    public Paths64 generateInfill(List<Paths64> walls, Paths64 originalLayer, float nozzleSize, float spacing, float angle, float altitude) {
        Paths64 infillBoundary;
        if (walls == null || walls.isEmpty()) {
            infillBoundary = originalLayer;
        } else {
            Paths64 innermostWall = walls.get(walls.size() - 1);
            infillBoundary = Clipper.InflatePaths(innermostWall, -0.5 * nozzleSize * SCALE, JoinType.Miter, EndType.Polygon);
        }

        if (infillBoundary == null || infillBoundary.isEmpty() || spacing <= 0) {
            return new Paths64();
        }

        Rect64 bounds = Clipper.GetBounds(infillBoundary);
        long centerX = (bounds.left + bounds.right) / 2;
        long centerY = (bounds.top + bounds.bottom) / 2;

        long width = bounds.right - bounds.left;
        long height = bounds.bottom - bounds.top;
        long diag = (long) Math.sqrt(width * width + height * height) + 1000;

        Paths64 honeycombPaths = generateHoneycomb(centerX, centerY, diag, spacing, angle);

        Clipper64 clipper = new Clipper64();
        clipper.addOpenSubject(honeycombPaths);
        clipper.addClip(infillBoundary);

        Paths64 clippedPaths = new Paths64();
        Paths64 dummyClosed = new Paths64();
        clipper.Execute(ClipType.Intersection, FillRule.EvenOdd, dummyClosed, clippedPaths);

        return clippedPaths;
    }

    /**
     * Generates a grid of zigzag paths that form a honeycomb pattern.
     *
     * @param cX        Center X.
     * @param cY        Center Y.
     * @param range     Area to cover.
     * @param spacing   Distance between parallel hexagon walls.
     * @param angleDeg  Rotation angle.
     * @return A {@link Paths64} containing the zigzag paths.
     */
    private Paths64 generateHoneycomb(long cX, long cY, long range, float spacing, float angleDeg) {
        Paths64 allPaths = new Paths64();
        
        double s = (spacing * SCALE) / Math.sqrt(3.0);
        double dy = (spacing * SCALE) / 2.0;
        double dx = 1.5 * s;
        double periodX = 3.0 * s;
        double periodY = spacing * SCALE;

        double angleRad = Math.toRadians(angleDeg);
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);

        long halfRange = range / 2;
        int numX = (int) (range / periodX) + 2;
        int numY = (int) (range / periodY) + 2;

        for (int j = -numY; j <= numY; j++) {
            double baseY = j * periodY;

            // Generate "up" path
            Path64 pathUp = new Path64();
            for (int i = -numX; i <= numX; i++) {
                double baseX = i * periodX;
                addPoint(pathUp, baseX, baseY, cX, cY, cosA, sinA);
                addPoint(pathUp, baseX + s, baseY, cX, cY, cosA, sinA);
                addPoint(pathUp, baseX + 1.5 * s, baseY + dy, cX, cY, cosA, sinA);
                addPoint(pathUp, baseX + 2.5 * s, baseY + dy, cX, cY, cosA, sinA);
                if (i == numX) {
                    addPoint(pathUp, baseX + 3.0 * s, baseY, cX, cY, cosA, sinA);
                }
            }
            allPaths.add(pathUp);

            // Generate "down" path
            Path64 pathDown = new Path64();
            for (int i = -numX; i <= numX; i++) {
                double baseX = i * periodX;
                addPoint(pathDown, baseX, baseY, cX, cY, cosA, sinA);
                addPoint(pathDown, baseX + s, baseY, cX, cY, cosA, sinA);
                addPoint(pathDown, baseX + 1.5 * s, baseY - dy, cX, cY, cosA, sinA);
                addPoint(pathDown, baseX + 2.5 * s, baseY - dy, cX, cY, cosA, sinA);
                if (i == numX) {
                    addPoint(pathDown, baseX + 3.0 * s, baseY, cX, cY, cosA, sinA);
                }
            }
            allPaths.add(pathDown);
        }

        return allPaths;
    }

    private void addPoint(Path64 path, double lx, double ly, long cX, long cY, double cosA, double sinA) {
        long x = Math.round(cX + lx * cosA - ly * sinA);
        long y = Math.round(cY + lx * sinA + ly * cosA);
        path.add(new Point64(x, y));
    }
}
