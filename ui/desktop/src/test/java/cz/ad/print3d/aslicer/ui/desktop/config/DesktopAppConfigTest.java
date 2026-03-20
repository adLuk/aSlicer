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
package cz.ad.print3d.aslicer.ui.desktop.config;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import cz.ad.print3d.aslicer.ui.desktop.DesktopApp;
import cz.ad.print3d.aslicer.ui.desktop.GdxTestUtils;
import cz.ad.print3d.aslicer.ui.desktop.persistence.ScenePersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DesktopAppConfigTest {
    @TempDir
    Path tempDir;

    private Path originalConfigPath;
    private Path originalWorkspacePath;

    @BeforeEach
    void setUp() {
        originalConfigPath = AppConfig.CONFIG_PATH;
        AppConfig.CONFIG_PATH = tempDir.resolve(".aslicer").resolve("aslicer.properties");
        originalWorkspacePath = ScenePersistence.WORKSPACE_PATH;
        ScenePersistence.WORKSPACE_PATH = tempDir.resolve(".aslicer").resolve("workspace.g3db");
    }

    @AfterEach
    void tearDown() {
        AppConfig.CONFIG_PATH = originalConfigPath;
        ScenePersistence.WORKSPACE_PATH = originalWorkspacePath;
    }

    @Test
    void testPropertyLoading() throws IOException {
        Properties props = new Properties();
        props.setProperty("control.rotateButton", "1");
        props.setProperty("test.float", "123.45");

        Files.createDirectories(AppConfig.CONFIG_PATH.getParent());
        try (OutputStream os = Files.newOutputStream(AppConfig.CONFIG_PATH)) {
            props.store(os, null);
        }

        AppConfig config = new AppConfig();
        assertEquals(1, config.getInt("control.rotateButton", 0));
        assertEquals(123.45f, config.getFloat("test.float", 0f));
    }

    @Test
    void testPropertySaving() throws IOException {
        AppConfig config = new AppConfig();
        config.setProperty("test.key", "test.value");
        config.save();

        assertTrue(Files.exists(AppConfig.CONFIG_PATH));
        AppConfig config2 = new AppConfig();
        assertEquals("test.value", config2.getProperty("test.key"));
    }

    @Test
    void testLastDirPersistence() throws IOException {
        DesktopApp app = new DesktopApp();
        app.lastDir = "/some/test/path";
        app.currentWidth = 1024;
        app.currentHeight = 768;

        app.saveAllConfig();

        AppConfig config = new AppConfig();
        assertEquals("/some/test/path", config.getProperty("last.dir"));
        assertEquals(1024, config.getInt("window.width", 0));
        assertEquals(768, config.getInt("window.height", 0));
    }

    @Test
    void testLastFilePersistence() throws IOException {
        DesktopApp app = new DesktopApp();
        app.currentModelPath = "/some/test/file.stl";

        app.saveAllConfig();

        AppConfig config = new AppConfig();
        assertEquals("/some/test/file.stl", config.getProperty("last.file"));

        app.currentModelPath = null;
        app.saveAllConfig();
        config = new AppConfig();
        assertFalse(config.containsKey("last.file"));
    }

    @Test
    void testCameraPersistence() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    app.create();
                    PerspectiveCamera cam = app.sceneManager.getCamera();
                    cam.position.set(1.5f, 2.5f, 3.5f);
                    cam.direction.set(0.1f, 0.2f, 0.3f);
                    cam.up.set(0f, 1f, 0f);

                    CameraInputController camController = app.sceneManager.getCameraController();
                    camController.target.set(10f, 20f, 30f);

                    app.saveAllConfig();

                    AppConfig config = new AppConfig();
                    assertEquals(1.5f, config.getFloat("camera.pos.x", 0f));
                    assertEquals(2.5f, config.getFloat("camera.pos.y", 0f));
                    assertEquals(3.5f, config.getFloat("camera.pos.z", 0f));

                    assertEquals(0.1f, config.getFloat("camera.dir.x", 0f), 0.001f);
                    assertEquals(0.2f, config.getFloat("camera.dir.y", 0f), 0.001f);
                    assertEquals(0.3f, config.getFloat("camera.dir.z", 0f), 0.001f);

                    assertEquals(0f, config.getFloat("camera.up.x", 1f));
                    assertEquals(1f, config.getFloat("camera.up.y", 0f));
                    assertEquals(0f, config.getFloat("camera.up.z", 1f));

                    assertEquals(10f, config.getFloat("camera.target.x", 0f));
                    assertEquals(20f, config.getFloat("camera.target.y", 0f));
                    assertEquals(30f, config.getFloat("camera.target.z", 0f));
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (errorRef.get() != null) {
            if (errorRef.get() instanceof RuntimeException) throw (RuntimeException) errorRef.get();
            if (errorRef.get() instanceof Error) throw (Error) errorRef.get();
            throw new RuntimeException(errorRef.get());
        }
    }
}
