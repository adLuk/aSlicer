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
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import clipper2.core.Rect64;
import cz.ad.print3d.aslicer.logic.core.WallTracer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FillerGeneratorTest {

    private static final double SCALE = 1000.0;

    @Test
    void testRectilinearInfill() {
        BasicFillerGenerator generator = new BasicFillerGenerator();
        generator.setPattern(BasicFillerGenerator.InfillPattern.RECTILINEAR);
        Paths64 square = createSquare(0, 0, 10); // 10x10 square
        
        // 2mm spacing, 0 degrees, 0.1 altitude
        Paths64 infill = generator.generateInfill(null, square, 0.4f, 2.0f, 0, 0.1f);
        
        assertFalse(infill.isEmpty());
        assertTrue(infill.size() >= 5);
        
        // Verify that all points are within or on the boundary (roughly)
        for (Path64 path : infill) {
            for (Point64 pt : path) {
                assertTrue(pt.x >= -5000 && pt.x <= 5000);
                assertTrue(pt.y >= -5000 && pt.y <= 5000);
            }
        }
    }

    @Test
    void testGridInfill() {
        BasicFillerGenerator generator = new BasicFillerGenerator();
        generator.setPattern(BasicFillerGenerator.InfillPattern.GRID);
        Paths64 square = createSquare(0, 0, 10);
        
        Paths64 infill = generator.generateInfill(null, square, 0.4f, 2.0f, 0, 0.1f);
        
        assertFalse(infill.isEmpty());
        assertTrue(infill.size() >= 10);
    }

    @Test
    void testTrianglesInfill() {
        BasicFillerGenerator generator = new BasicFillerGenerator();
        generator.setPattern(BasicFillerGenerator.InfillPattern.TRIANGLES);
        Paths64 square = createSquare(0, 0, 10);
        
        Paths64 infill = generator.generateInfill(null, square, 0.4f, 2.0f, 0, 0.1f);
        
        assertFalse(infill.isEmpty());
        assertTrue(infill.size() >= 15);
    }

    @Test
    void testInfillWithWalls() {
        BasicFillerGenerator generator = new BasicFillerGenerator();
        generator.setPattern(BasicFillerGenerator.InfillPattern.RECTILINEAR);
        WallTracer tracer = new WallTracer();
        Paths64 square = createSquare(0, 0, 10);
        
        float nozzleSize = 0.4f;
        List<Paths64> walls = tracer.traceWallsForLayer(square, nozzleSize, 2);
        
        Paths64 infill = generator.generateInfill(walls, square, nozzleSize, 2.0f, 45, 0.1f);
        
        assertFalse(infill.isEmpty());
        
        // Verify points are inside the walls
        for (Path64 path : infill) {
            for (Point64 pt : path) {
                assertTrue(pt.x >= -4200 && pt.x <= 4200, "Point X " + pt.x + " out of bounds");
                assertTrue(pt.y >= -4200 && pt.y <= 4200, "Point Y " + pt.y + " out of bounds");
            }
        }
    }

    @Test
    void testEmptyInputs() {
        BasicFillerGenerator generator = new BasicFillerGenerator();
        
        assertTrue(generator.generateInfill(null, null, 0.4f, 2.0f, 0, 0.1f).isEmpty());
        assertTrue(generator.generateInfill(null, new Paths64(), 0.4f, 2.0f, 0, 0.1f).isEmpty());
        assertTrue(generator.generateInfill(null, createSquare(0, 0, 10), 0.4f, 0, 0, 0.1f).isEmpty());
        assertTrue(generator.generateInfill(null, createSquare(0, 0, 10), 0.4f, -1.0f, 0, 0.1f).isEmpty());
    }

    @Test
    void testDifferentAngles() {
        BasicFillerGenerator generator = new BasicFillerGenerator();
        generator.setPattern(BasicFillerGenerator.InfillPattern.RECTILINEAR);
        Paths64 square = createSquare(0, 0, 10);
        
        Paths64 infill45 = generator.generateInfill(null, square, 0.4f, 2.0f, 45, 0.1f);
        assertFalse(infill45.isEmpty());
        
        Paths64 infill90 = generator.generateInfill(null, square, 0.4f, 2.0f, 90, 0.1f);
        assertFalse(infill90.isEmpty());
    }

    @Test
    void testGyroidInfill() {
        TpmsFillerGenerator generator = new TpmsFillerGenerator();
        generator.setType(TpmsFillerGenerator.TpmsType.GYROID);
        Paths64 square = createSquare(0, 0, 10);
        
        // Use larger spacing (period) for easier verification
        Paths64 infill = generator.generateInfill(null, square, 0.4f, 5.0f, 0, 1.0f);
        
        assertFalse(infill.isEmpty());
        for (Path64 path : infill) {
            for (Point64 pt : path) {
                assertTrue(pt.x >= -5000 && pt.x <= 5000);
                assertTrue(pt.y >= -5000 && pt.y <= 5000);
            }
        }
    }

    @Test
    void testSchwarzPInfill() {
        TpmsFillerGenerator generator = new TpmsFillerGenerator();
        generator.setType(TpmsFillerGenerator.TpmsType.SCHWARZ_P);
        Paths64 square = createSquare(0, 0, 10);
        
        Paths64 infill = generator.generateInfill(null, square, 0.4f, 5.0f, 0, 1.0f);
        
        assertFalse(infill.isEmpty());
        for (Path64 path : infill) {
            for (Point64 pt : path) {
                assertTrue(pt.x >= -5000 && pt.x <= 5000);
                assertTrue(pt.y >= -5000 && pt.y <= 5000);
            }
        }
    }

    @Test
    void testHoneycombInfill() {
        HoneycombFillerGenerator generator = new HoneycombFillerGenerator();
        Paths64 square = createSquare(0, 0, 10);
        
        Paths64 infill = generator.generateInfill(null, square, 0.4f, 2.0f, 0, 0.1f);
        
        assertFalse(infill.isEmpty());
        // Honeycomb with 2.0 spacing in 10.0 square should have several paths
        assertTrue(infill.size() > 0);
        
        for (Path64 path : infill) {
            for (Point64 pt : path) {
                assertTrue(pt.x >= -5000 && pt.x <= 5000);
                assertTrue(pt.y >= -5000 && pt.y <= 5000);
            }
        }

        // Test with angle
        Paths64 infill45 = generator.generateInfill(null, square, 0.4f, 2.0f, 45, 0.1f);
        assertFalse(infill45.isEmpty());
    }

    @Test
    void testLightningInfill() {
        LightningFillerGenerator generator = new LightningFillerGenerator();
        
        // Create a 10-layer cube (10x10 square)
        List<Paths64> layers = new ArrayList<>();
        float firstAltitude = 0.1f;
        float layerHeight = 0.2f;
        for (int i = 0; i < 10; i++) {
            layers.add(createSquare(0, 0, 10));
        }
        
        generator.setLayers(layers, firstAltitude, layerHeight);
        
        // Top layer (altitude ~1.9) should have large infill area (ceiling)
        Paths64 infillTop = generator.generateInfill(null, layers.get(9), 0.4f, 2.0f, 0, 1.9f);
        assertFalse(infillTop.isEmpty(), "Top layer should have infill");
        
        // Bottom layer (altitude 0.1) should have smaller infill area due to thinning
        Paths64 infillBottom = generator.generateInfill(null, layers.get(0), 0.4f, 2.0f, 0, 0.1f);
        assertFalse(infillBottom.isEmpty(), "Bottom layer should still have some infill");
        
        Rect64 boundsTop = Clipper.GetBounds(infillTop);
        Rect64 boundsBottom = Clipper.GetBounds(infillBottom);
        
        long widthTop = boundsTop.right - boundsTop.left;
        long widthBottom = boundsBottom.right - boundsBottom.left;
        
        // With 9 layer drops (0.2 each) and 1.0 thinning factor, it should shrink by ~1.8mm each side
        // Total shrink = 3.6mm. Original 10mm -> ~6.4mm.
        assertTrue(widthBottom < widthTop, "Bottom infill should be smaller than top infill");
        assertTrue(widthBottom > 0, "Bottom infill should not have disappeared yet");
    }

    @Test
    void testLightningInfillThinning() {
        LightningFillerGenerator generator = new LightningFillerGenerator();
        generator.setThinningFactor(2.0f); // Fast thinning
        
        List<Paths64> layers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            layers.add(createSquare(0, 0, 10));
        }
        generator.setLayers(layers, 0.1f, 1.0f); // 1mm layer height
        
        // 9 layers diff = 9mm. With 2.0 factor, thinning = 18mm each side.
        // 10mm square should disappear completely at the bottom.
        Paths64 infillBottom = generator.generateInfill(null, layers.get(0), 0.4f, 2.0f, 0, 0.1f);
        assertTrue(infillBottom.isEmpty(), "Infill should have disappeared with fast thinning");
    }

    @Test
    void testLightningInfillMultipleCeilings() {
        LightningFillerGenerator generator = new LightningFillerGenerator();
        
        // Layer 0: Square 10x10
        // Layer 1: Square 10x10
        // Layer 2: Square 5x5 (Ceiling at Layer 1!)
        // Layer 3: Square 5x5 (Ceiling at Layer 3)
        List<Paths64> layers = new ArrayList<>();
        layers.add(createSquare(0, 0, 10));
        layers.add(createSquare(0, 0, 10));
        layers.add(createSquare(0, 0, 5));
        layers.add(createSquare(0, 0, 5));
        
        generator.setLayers(layers, 0.0f, 1.0f);
        
        // Layer 3 is a ceiling (5x5)
        Paths64 infill3 = generator.generateInfill(null, layers.get(3), 0.4f, 1.0f, 0, 3.0f);
        assertFalse(infill3.isEmpty());
        Rect64 bounds3 = Clipper.GetBounds(infill3);
        assertEquals(5000, bounds3.right - bounds3.left, 500); // ~5mm

        // Layer 1 is a ceiling for the outer part (10x10 - 5x5)
        Paths64 infill1 = generator.generateInfill(null, layers.get(1), 0.4f, 1.0f, 0, 1.0f);
        assertFalse(infill1.isEmpty());
        Rect64 bounds1 = Clipper.GetBounds(infill1);
        assertEquals(10000, bounds1.right - bounds1.left, 500); // Should cover the 10mm width
    }

    @Test
    void testAdaptiveCubicInfill() {
        AdaptiveCubicFillerGenerator generator = new AdaptiveCubicFillerGenerator();
        generator.setMaxDepth(2);
        
        List<Paths64> layers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            layers.add(createSquare(0, 0, 10));
        }
        generator.setLayers(layers, 0.0f, 1.0f);
        
        // At altitude 2.0, generate infill
        Paths64 infill = generator.generateInfill(null, layers.get(2), 0.4f, 5.0f, 0, 2.0f);
        
        assertFalse(infill.isEmpty(), "Adaptive Cubic infill should not be empty");
        for (Path64 path : infill) {
            for (Point64 pt : path) {
                assertTrue(pt.x >= -5000 && pt.x <= 5000, "Point X out of bounds: " + pt.x);
                assertTrue(pt.y >= -5000 && pt.y <= 5000, "Point Y out of bounds: " + pt.y);
            }
        }
    }

    @Test
    void testSmallBoundary() {
        BasicFillerGenerator generator = new BasicFillerGenerator();
        Paths64 tinySquare = createSquare(0, 0, 0.1);
        generator.generateInfill(null, tinySquare, 0.4f, 2.0f, 0, 0.1f);
    }

    private Paths64 createSquare(double x, double y, double size) {
        long x1 = Math.round((x - size / 2) * SCALE);
        long x2 = Math.round((x + size / 2) * SCALE);
        long y1 = Math.round((y - size / 2) * SCALE);
        long y2 = Math.round((y + size / 2) * SCALE);

        Path64 path = new Path64();
        path.add(new Point64(x1, y1));
        path.add(new Point64(x2, y1));
        path.add(new Point64(x2, y2));
        path.add(new Point64(x1, y2));
        
        Paths64 paths = new Paths64();
        paths.add(path);
        return paths;
    }
}
