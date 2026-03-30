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

import clipper2.core.Path64;
import clipper2.core.Paths64;
import clipper2.core.Point64;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ShellGeneratorTest {

    private static final double SCALE = 1000.0;

    private void mockGdxGL() {
        com.badlogic.gdx.Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                new Class<?>[]{com.badlogic.gdx.graphics.GL20.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("glGenBuffer")) return 1;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(boolean.class)) return true;
                    return null;
                }
        );
        com.badlogic.gdx.Gdx.gl = com.badlogic.gdx.Gdx.gl20;
    }

    @Test
    void testMonotonicInfillGeneration() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<List<Paths64>> infillRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.begin();
                    MeshPartBuilder cubeBuilder = modelBuilder.part("cube", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, new Material());
                    BoxShapeBuilder.build(cubeBuilder, 0, 0.5f, 0, 10f, 1f, 10f);
                    Model model = modelBuilder.end();

                    Slicer slicer = new Slicer();
                    List<Paths64> layers = slicer.slice(model, 0.5f);

                    ShellGenerator generator = new ShellGenerator();
                    generator.setOverlap(0.15f);
                    infillRef.set(generator.generateMonotonicInfill(layers, 0.4f, 1.0f, 45.0f));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Paths64> infills = infillRef.get();
        assertNotNull(infills);
        assertFalse(infills.isEmpty());

        for (Paths64 layerInfill : infills) {
            assertFalse(layerInfill.isEmpty());
            // Check that we have multiple lines
            assertTrue(layerInfill.size() > 5);
            
            // Check monotonic sorting (at least partially)
            double lastProjection = Double.NEGATIVE_INFINITY;
            double perpAngleRad = Math.toRadians(45.0 + 90.0);
            double cosA = Math.cos(perpAngleRad);
            double sinA = Math.sin(perpAngleRad);
            
            for (Path64 path : layerInfill) {
                Point64 p = path.get(0);
                double projection = p.x * cosA + p.y * sinA;
                assertTrue(projection >= lastProjection - 1.0, "Monotonic order failed: " + projection + " < " + lastProjection);
                lastProjection = projection;
            }
        }
    }

    @Test
    void testOverlapEffect() {
        Paths64 layer = new Paths64();
        Path64 square = new Path64();
        square.add(new Point64(0, 0));
        square.add(new Point64(10000, 0));
        square.add(new Point64(10000, 10000));
        square.add(new Point64(0, 10000));
        layer.add(square);

        ShellGenerator generator = new ShellGenerator();
        
        // No overlap
        generator.setOverlap(0.0f);
        Paths64 infillNoOverlap = generator.generateLayerInfill(layer, 0.4f, 1.0f, 0.0f);
        
        // Significant overlap (50%)
        generator.setOverlap(0.5f);
        Paths64 infillWithOverlap = generator.generateLayerInfill(layer, 0.4f, 1.0f, 0.0f);
        
        // Lines with overlap should be longer
        long totalLengthNoOverlap = calculateTotalLength(infillNoOverlap);
        long totalLengthWithOverlap = calculateTotalLength(infillWithOverlap);
        
        System.out.println("[DEBUG_LOG] No overlap length: " + totalLengthNoOverlap);
        System.out.println("[DEBUG_LOG] With overlap length: " + totalLengthWithOverlap);
        
        assertTrue(totalLengthWithOverlap > totalLengthNoOverlap, "Overlap should increase total line length");
    }

    private long calculateTotalLength(Paths64 paths) {
        long total = 0;
        for (Path64 path : paths) {
            for (int i = 0; i < path.size() - 1; i++) {
                Point64 p1 = path.get(i);
                Point64 p2 = path.get(i + 1);
                total += (long) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
            }
        }
        return total;
    }
}
