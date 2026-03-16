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
package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the model placement logic in DesktopApp.
 */
public class ModelPlacementTest {

    @TempDir
    Path tempDir;

    private Path originalConfigPath;

    @BeforeEach
    void setUp() {
        originalConfigPath = AppConfig.CONFIG_PATH;
        AppConfig.CONFIG_PATH = tempDir.resolve(".aSlicer-desktop-test-placement.properties");
    }

    @AfterEach
    void tearDown() {
        AppConfig.CONFIG_PATH = originalConfigPath;
    }

    @Test
    void testPlacement() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    
                    ModelBuilder modelBuilder = new ModelBuilder();
                    Model model = modelBuilder.createBox(1f, 1f, 1f, 
                        new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    
                    ModelInstance inst1 = new ModelInstance(model);
                    // First model should be at origin
                    app.placeNearExisting(inst1);
                    app.instances.add(inst1);
                    
                    Vector3 pos1 = new Vector3();
                    inst1.transform.getTranslation(pos1);
                    assertEquals(0f, pos1.x, 0.001f);
                    assertEquals(0f, pos1.y, 0.001f);
                    assertEquals(0.5f, pos1.z, 0.001f);
                    
                    ModelInstance inst2 = new ModelInstance(model);
                    // Second model should be placed near first
                    // First box is 1x1x1 centered at 0,0,0 -> bounds are -0.5 to 0.5
                    // new box is 1x1x1 centered at 0,0,0 -> bounds are -0.5 to 0.5
                    // default distance is 0.5
                    // currentMaxX = 0.5
                    // newMinX = -0.5
                    // offsetX = 0.5 - (-0.5) + 0.5 = 1.5
                    app.placeNearExisting(inst2);
                    
                    Vector3 pos2 = new Vector3();
                    inst2.transform.getTranslation(pos2);
                    assertEquals(1.5f, pos2.x, 0.001f);
                    assertEquals(0.5f, pos2.z, 0.001f);
                    
                    app.instances.add(inst2);
                    
                    // Third model
                    ModelInstance inst3 = new ModelInstance(model);
                    // totalBounds.max.x = 1.5 + 0.5 = 2.0
                    // newMinX = -0.5
                    // offsetX = 2.0 - (-0.5) + 0.5 = 3.0
                    app.placeNearExisting(inst3);
                    Vector3 pos3 = new Vector3();
                    inst3.transform.getTranslation(pos3);
                    assertEquals(3.0f, pos3.x, 0.001f);
                    assertEquals(0.5f, pos3.z, 0.001f);

                    model.dispose();
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

            private void mockGdxGL() {
                Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                        com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                        new Class[]{com.badlogic.gdx.graphics.GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture")) return 1;
                            if (method.getReturnType().equals(int.class)) return 0;
                            if (method.getReturnType().equals(boolean.class)) return true;
                            return null;
                        }
                );
                Gdx.gl = Gdx.gl20;
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }
    }

    @Test
    void testPlacementWithConfigurableDistance() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    
                    // Set custom distance
                    AppConfigDto dto = app.appConfig.loadToDto();
                    dto.setDistance(1.0f);
                    app.appConfig.saveFromDto(dto);
                    
                    ModelBuilder modelBuilder = new ModelBuilder();
                    Model model = modelBuilder.createBox(1f, 1f, 1f, 
                        new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    
                    ModelInstance inst1 = new ModelInstance(model);
                    app.placeNearExisting(inst1);
                    app.instances.add(inst1);
                    
                    ModelInstance inst2 = new ModelInstance(model);
                    // currentMaxX = 0.5
                    // newMinX = -0.5
                    // distance = 1.0
                    // offsetX = 0.5 - (-0.5) + 1.0 = 2.0
                    app.placeNearExisting(inst2);
                    
                    Vector3 pos2 = new Vector3();
                    inst2.transform.getTranslation(pos2);
                    assertEquals(2.0f, pos2.x, 0.001f);
                    assertEquals(0.5f, pos2.z, 0.001f);

                    model.dispose();
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

            private void mockGdxGL() {
                Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                        com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                        new Class[]{com.badlogic.gdx.graphics.GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture")) return 1;
                            if (method.getReturnType().equals(int.class)) return 0;
                            if (method.getReturnType().equals(boolean.class)) return true;
                            return null;
                        }
                );
                Gdx.gl = Gdx.gl20;
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }
    }

    @Test
    void testZPlacement() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    mockGdxGL();
                    DesktopApp app = new DesktopApp();

                    ModelBuilder modelBuilder = new ModelBuilder();
                    // Box 1x1x1 centered at 0,0,0 has min.z = -0.5
                    Model model = modelBuilder.createBox(1f, 1f, 1f,
                            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

                    ModelInstance inst1 = new ModelInstance(model);
                    // Bottom of model (min.z = -0.5) should be shifted to Z=0
                    // Translation Z should be 0.5
                    app.placeNearExisting(inst1);

                    Vector3 pos1 = new Vector3();
                    inst1.transform.getTranslation(pos1);
                    assertEquals(0.5f, pos1.z, 0.001f, "First model should be shifted up to Z=0.5 because its min.z is -0.5");

                    model.dispose();
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }

            private void mockGdxGL() {
                Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) java.lang.reflect.Proxy.newProxyInstance(
                        com.badlogic.gdx.graphics.GL20.class.getClassLoader(),
                        new Class[]{com.badlogic.gdx.graphics.GL20.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("glGenBuffer") || method.getName().equals("glGenTexture")) return 1;
                            if (method.getReturnType().equals(int.class)) return 0;
                            if (method.getReturnType().equals(boolean.class)) return true;
                            return null;
                        }
                );
                Gdx.gl = Gdx.gl20;
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test should complete within 5 seconds");
        if (errorRef.get() != null) {
            throw new RuntimeException(errorRef.get());
        }
    }
}
