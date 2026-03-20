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
package cz.ad.print3d.aslicer.ui.desktop.persistence;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.math.Vector3;
import cz.ad.print3d.aslicer.ui.desktop.DesktopApp;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for scene persistence in DesktopApp.
 */
public class DesktopAppScenePersistenceTest {
    @TempDir
    Path tempDir;

    private Path originalWorkspacePath;
    private Path originalConfigPath;

    @BeforeEach
    void setUp() {
        originalWorkspacePath = ScenePersistence.WORKSPACE_PATH;
        ScenePersistence.WORKSPACE_PATH = tempDir.resolve(".aslicer").resolve("workspace.g3db");
        originalConfigPath = AppConfig.CONFIG_PATH;
        AppConfig.CONFIG_PATH = tempDir.resolve(".aslicer").resolve("aslicer.properties");
    }

    @AfterEach
    void tearDown() {
        ScenePersistence.WORKSPACE_PATH = originalWorkspacePath;
        AppConfig.CONFIG_PATH = originalConfigPath;
    }

    @Test
    void testScenePersistenceIntegration() throws InterruptedException {
        // Run 1: Load a model, it should be saved to workspace.g3db automatically
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef1 = new AtomicReference<>();
        AtomicReference<Vector3> posRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    java.nio.file.Path fileAPath = java.nio.file.Paths.get("..", "..", "logic", "model", "src", "test", "resources", "stl", "test-binary.stl").toAbsolutePath().normalize();
                    app.create(); // Need to initialize modelManager
                    app.modelManager.loadModel(fileAPath.toString());

                    assertTrue(Files.exists(ScenePersistence.WORKSPACE_PATH), "Workspace file should be created on loadModel");
                    
                    Vector3 pos = new Vector3();
                    app.modelManager.getInstances().get(0).transform.getTranslation(pos);
                    posRef.set(new Vector3(pos));
                    
                } catch (Throwable t) {
                    errorRef1.set(t);
                } finally {
                    latch1.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch1.await(10, TimeUnit.SECONDS));
        if (errorRef1.get() != null) fail(errorRef1.get());

        // Run 2: Start new app instance, it should load from workspace.g3db
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef2 = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    app.create(); // Initializes modelManager and loads initial scene
                    
                    assertEquals(1, app.modelManager.getInstances().size, "Should have loaded 1 model from workspace");
                    Vector3 pos = new Vector3();
                    app.modelManager.getInstances().get(0).transform.getTranslation(pos);
                    assertEquals(posRef.get().x, pos.x, 0.001f);
                    assertEquals(posRef.get().y, pos.y, 0.001f);
                    assertEquals(posRef.get().z, pos.z, 0.001f);

                } catch (Throwable t) {
                    errorRef2.set(t);
                } finally {
                    latch2.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch2.await(10, TimeUnit.SECONDS));
        if (errorRef2.get() != null) fail(errorRef2.get());
    }

    @Test
    void testRemoveModelUpdatesWorkspace() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef1 = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    app.create();
                    java.nio.file.Path fileAPath = java.nio.file.Paths.get("..", "..", "logic", "model", "src", "test", "resources", "stl", "test-binary.stl").toAbsolutePath().normalize();
                    app.modelManager.loadModel(fileAPath.toString());
                    assertEquals(1, app.modelManager.getInstances().size);
                    
                    ScenePersistence persistence = new ScenePersistence();
                    assertEquals(1, persistence.loadScene().size);

                    app.modelManager.removeModel(0);
                    assertEquals(0, app.modelManager.getInstances().size);
                    assertEquals(0, persistence.loadScene().size, "Workspace should be empty after removeModel");
                    
                } catch (Throwable t) {
                    errorRef1.set(t);
                } finally {
                    latch1.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch1.await(10, TimeUnit.SECONDS));
        if (errorRef1.get() != null) fail(errorRef1.get());
    }
}
