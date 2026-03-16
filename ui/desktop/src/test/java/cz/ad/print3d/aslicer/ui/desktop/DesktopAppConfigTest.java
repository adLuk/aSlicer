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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DesktopAppConfigTest {
    @TempDir
    Path tempDir;

    private Path originalConfigPath;

    @BeforeEach
    void setUp() {
        originalConfigPath = DesktopApp.configPath;
        DesktopApp.configPath = tempDir.resolve(".aSlicer-desktop.properties");
    }

    @AfterEach
    void tearDown() {
        DesktopApp.configPath = originalConfigPath;
    }

    @Test
    void testPropertyLoading() throws IOException {
        Properties props = new Properties();
        props.setProperty("control.rotateButton", "1");
        props.setProperty("control.translateButton", "2");
        props.setProperty("control.forwardButton", "0");
        props.setProperty("control.forwardKey", String.valueOf(Input.Keys.W));
        props.setProperty("control.backwardKey", String.valueOf(Input.Keys.S));
        props.setProperty("test.float", "123.45");

        try (OutputStream os = Files.newOutputStream(DesktopApp.configPath)) {
            props.store(os, null);
        }

        Properties loaded = DesktopApp.loadConfig();
        assertEquals("1", loaded.getProperty("control.rotateButton"));
        assertEquals("2", loaded.getProperty("control.translateButton"));
        assertEquals("0", loaded.getProperty("control.forwardButton"));
        assertEquals(String.valueOf(Input.Keys.W), loaded.getProperty("control.forwardKey"));
        assertEquals(String.valueOf(Input.Keys.S), loaded.getProperty("control.backwardKey"));
        
        assertEquals(1, DesktopApp.getIntProperty(loaded, "control.rotateButton", 0));
        assertEquals(2, DesktopApp.getIntProperty(loaded, "control.translateButton", 0));
        assertEquals(0, DesktopApp.getIntProperty(loaded, "control.forwardButton", 1));
        assertEquals(Input.Keys.W, DesktopApp.getIntProperty(loaded, "control.forwardKey", 0));
        assertEquals(Input.Keys.S, DesktopApp.getIntProperty(loaded, "control.backwardKey", 0));
        assertEquals(123.45f, DesktopApp.getFloatProperty(loaded, "test.float", 0f));
    }

    @Test
    void testPropertySaving() throws IOException {
        Properties props = new Properties();
        props.setProperty("test.key", "test.value");
        DesktopApp.saveConfig(props);

        assertTrue(Files.exists(DesktopApp.configPath));
        Properties loaded = DesktopApp.loadConfig();
        assertEquals("test.value", loaded.getProperty("test.key"));
    }
    
    @Test
    void testLastDirPersistence() throws IOException {
        DesktopApp app = new DesktopApp();
        app.lastDir = "/some/test/path";
        app.currentWidth = 1024;
        app.currentHeight = 768;

        app.saveAllConfig();

        Properties loaded = DesktopApp.loadConfig();
        assertEquals("/some/test/path", loaded.getProperty("last.dir"));
        assertEquals("1024", loaded.getProperty("window.width"));
        assertEquals("768", loaded.getProperty("window.height"));
    }

    @Test
    void testLastFilePersistence() throws IOException {
        DesktopApp app = new DesktopApp();
        app.currentModelPath = "/some/test/file.stl";

        app.saveAllConfig();

        Properties loaded = DesktopApp.loadConfig();
        assertEquals("/some/test/file.stl", loaded.getProperty("last.file"));

        app.currentModelPath = null;
        app.saveAllConfig();
        loaded = DesktopApp.loadConfig();
        assertTrue(!loaded.containsKey("last.file"));
    }

    @Test
    void testCameraPersistence() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    DesktopApp app = new DesktopApp();
                    app.cam = new PerspectiveCamera(67, 800, 600);
                    app.cam.position.set(1.5f, 2.5f, 3.5f);
                    app.cam.direction.set(0.1f, 0.2f, 0.3f);
                    app.cam.up.set(0f, 1f, 0f);

                    app.camController = new CameraInputController(app.cam);
                    app.camController.target.set(10f, 20f, 30f);

                    app.saveAllConfig();

                    Properties loaded = DesktopApp.loadConfig();
                    assertEquals(1.5f, DesktopApp.getFloatProperty(loaded, "camera.pos.x", 0f));
                    assertEquals(2.5f, DesktopApp.getFloatProperty(loaded, "camera.pos.y", 0f));
                    assertEquals(3.5f, DesktopApp.getFloatProperty(loaded, "camera.pos.z", 0f));

                    assertEquals(0.1f, DesktopApp.getFloatProperty(loaded, "camera.dir.x", 0f), 0.001f);
                    assertEquals(0.2f, DesktopApp.getFloatProperty(loaded, "camera.dir.y", 0f), 0.001f);
                    assertEquals(0.3f, DesktopApp.getFloatProperty(loaded, "camera.dir.z", 0f), 0.001f);

                    assertEquals(0f, DesktopApp.getFloatProperty(loaded, "camera.up.x", 1f));
                    assertEquals(1f, DesktopApp.getFloatProperty(loaded, "camera.up.y", 0f));
                    assertEquals(0f, DesktopApp.getFloatProperty(loaded, "camera.up.z", 1f));

                    assertEquals(10f, DesktopApp.getFloatProperty(loaded, "camera.target.x", 0f));
                    assertEquals(20f, DesktopApp.getFloatProperty(loaded, "camera.target.y", 0f));
                    assertEquals(30f, DesktopApp.getFloatProperty(loaded, "camera.target.z", 0f));
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
