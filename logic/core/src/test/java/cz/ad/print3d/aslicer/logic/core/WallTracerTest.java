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

import clipper2.Clipper;
import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WallTracerTest {

    @Test
    void testTraceWallsSquare() {
        // Create a 10x10 square (scaled by 1000)
        Paths64 square = new Paths64();
        Path64 path = new Path64();
        path.add(new Point64(0, 0));
        path.add(new Point64(10000, 0));
        path.add(new Point64(10000, 10000));
        path.add(new Point64(0, 10000));
        square.add(path);

        WallTracer tracer = new WallTracer();
        // nozzleSize = 0.4, wallCount = 2
        List<Paths64> walls = tracer.traceWallsForLayer(square, 0.4f, 2);

        assertNotNull(walls);
        assertEquals(2, walls.size());

        // First wall: offset -0.2 * 1000 = -200
        // Expected square: (200, 200) to (9800, 9800)
        // Side = 9600. Area = 9600 * 9600 = 92,160,000
        double area1 = Clipper.Area(walls.get(0));
        assertEquals(92160000.0, Math.abs(area1), 10.0);

        // Second wall: offset -(0.2 + 0.4) * 1000 = -600
        // Expected square: (600, 600) to (9400, 9400)
        // Side = 8800. Area = 8800 * 8800 = 77,440,000
        double area2 = Clipper.Area(walls.get(1));
        assertEquals(77440000.0, Math.abs(area2), 10.0);
    }

    @Test
    void testThinWall() {
        // Create a thin rectangle 1.0 x 10.0
        Paths64 rect = new Paths64();
        Path64 path = new Path64();
        path.add(new Point64(0, 0));
        path.add(new Point64(1000, 0));
        path.add(new Point64(1000, 10000));
        path.add(new Point64(0, 10000));
        rect.add(path);

        WallTracer tracer = new WallTracer();
        // nozzleSize = 0.4, wallCount = 3
        // First wall offset: -0.2. Remaining width: 0.6. OK.
        // Second wall offset: -0.6. Remaining width: -0.2. Should DISAPPEAR.
        List<Paths64> walls = tracer.traceWallsForLayer(rect, 0.4f, 3);

        assertNotNull(walls);
        assertEquals(1, walls.size()); // Only 1 wall should remain
    }

    @Test
    void testEmptyInput() {
        WallTracer tracer = new WallTracer();
        assertTrue(tracer.traceWalls(null, 0.4f, 2).isEmpty());
        assertTrue(tracer.traceWalls(Collections.emptyList(), 0.4f, 2).isEmpty());
        assertTrue(tracer.traceWallsForLayer(null, 0.4f, 2).isEmpty());
        assertTrue(tracer.traceWallsForLayer(new Paths64(), 0.4f, 2).isEmpty());
    }

    @Test
    void testInvalidParameters() {
        Paths64 square = new Paths64();
        Path64 path = new Path64();
        path.add(new Point64(0, 0));
        path.add(new Point64(1000, 0));
        path.add(new Point64(1000, 1000));
        path.add(new Point64(0, 1000));
        square.add(path);

        WallTracer tracer = new WallTracer();
        assertTrue(tracer.traceWallsForLayer(square, 0, 1).isEmpty());
        assertTrue(tracer.traceWallsForLayer(square, -0.1f, 1).isEmpty());
        assertTrue(tracer.traceWallsForLayer(square, 0.4f, 0).isEmpty());
        assertTrue(tracer.traceWallsForLayer(square, 0.4f, -1).isEmpty());
    }
}
