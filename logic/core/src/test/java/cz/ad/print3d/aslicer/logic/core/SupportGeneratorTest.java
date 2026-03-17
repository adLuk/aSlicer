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
import clipper2.core.Paths64;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SupportGeneratorTest {

    @Test
    void testSupportForOverhang() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<List<Paths64>> supportRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.begin();
                    // Vertical pillar (1x1x1) at center (0, 0.5, 0) -> Y from 0 to 1
                    modelBuilder.part("pillar", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, new Material())
                            .box(0, 0.5f, 0, 1f, 1f, 1f);
                    // Overhanging roof (3x1x3) at center (0, 1.5, 0) -> Y from 1 to 2
                    modelBuilder.part("roof", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, new Material())
                            .box(0, 1.5f, 0, 3f, 1f, 3f);
                    Model model = modelBuilder.end();

                    Slicer slicer = new Slicer();
                    List<Paths64> modelLayers = slicer.slice(model, 0.5f);

                    SupportGenerator generator = new SupportGenerator();
                    generator.setSupportGap(0.1f);
                    supportRef.set(generator.generateSupport(model, 0.5f, modelLayers));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Paths64> supports = supportRef.get();
        assertNotNull(supports);
        
        // Model height is 2.0. layerHeight 0.5. 
        // Slices at 0.25, 0.75, 1.25, 1.75.
        assertEquals(4, supports.size());
        
        // Layer 0 (0.25): Pillar is at (0,0) with size 1x1. Roof is above (bottom at Y=1.0).
        // Overhang triangles of roof are at Y=1.0. 1.0 > 0.25.
        // Roof projection is 3x3 (from -1.5 to 1.5).
        assertFalse(supports.get(0).isEmpty());
        double area0 = Clipper.Area(supports.get(0));
        // Area of roof = 3000 * 3000 = 9,000,000.
        // Area of pillar = 1000 * 1000 = 1,000,000.
        // Inflated pillar gap 0.1 -> pillar area ~ (1000+200)*(1000+200) = 1,440,000.
        // expected area ~ 9,000,000 - 1,440,000 = 7,560,000.
        assertTrue(Math.abs(area0) > 7000000 && Math.abs(area0) < 8000000, "Area was: " + area0);

        // Layer 1 (0.75): Still below roof (bottom at Y=1.0). Pillar is still there.
        assertFalse(supports.get(1).isEmpty());

        // Layer 2 (1.25): Inside roof (Y from 1.0 to 2.0).
        // Roof bottom is at 1.0, which is BELOW 1.25.
        // No overhangs above 1.25. (Roof top is at 2.0 but it's not an overhang).
        assertTrue(supports.get(2).isEmpty());
    }

    @Test
    void testNoSupportNeeded() throws InterruptedException {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        AtomicReference<List<Paths64>> supportRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    ModelBuilder modelBuilder = new ModelBuilder();
                    modelBuilder.begin();
                    // Simple cube on the ground
                    modelBuilder.part("cube", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, new Material())
                            .box(0, 0.5f, 0, 1f, 1f, 1f);
                    Model model = modelBuilder.end();

                    Slicer slicer = new Slicer();
                    List<Paths64> modelLayers = slicer.slice(model, 0.5f);

                    SupportGenerator generator = new SupportGenerator();
                    supportRef.set(generator.generateSupport(model, 0.5f, modelLayers));
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    latch.countDown();
                    com.badlogic.gdx.Gdx.app.exit();
                }
            }
        }, config);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        List<Paths64> supports = supportRef.get();
        assertNotNull(supports);
        
        for (Paths64 layer : supports) {
            assertTrue(layer.isEmpty());
        }
    }

    private void mockGdxGL() {
        com.badlogic.gdx.Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                new Class[]{com.badlogic.gdx.graphics.GL20.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("glGenBuffer")) return 1;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(boolean.class)) return true;
                    return null;
                }
        );
        com.badlogic.gdx.Gdx.gl = com.badlogic.gdx.Gdx.gl20;
    }
}
