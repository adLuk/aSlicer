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
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfig;
import cz.ad.print3d.aslicer.ui.desktop.persistence.ScenePersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates that all supported file formats (.stl, .ast, .3mf, .gcode) can be loaded into the scene.
 */
public class DesktopAppSupportedExtensionsTest {
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
    void testSupportedFormatsLoading() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    // Mock minimal GL20 for headless model building
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
                    DesktopApp app = new DesktopApp();

                    String[] formats = {
                            "stl/test-binary.stl",
                            "stl/test-ascii.ast",
                            "3mf/test-simple.3mf",
                            "gcode/example.gcode"
                    };

                    Path resourcesPath = Paths.get("..", "..", "logic", "model", "src", "test", "resources").toAbsolutePath().normalize();

                    for (String format : formats) {
                        Path fullPath = resourcesPath.resolve(format);
                        assertTrue(java.nio.file.Files.exists(fullPath), "Test file missing: " + fullPath);
                        app.loadModel(fullPath.toString());
                    }

                    // Check if all were added to the lists
                    // Note: .gcode might result in empty model but still should be added to models/instances
                    // unless loadModel has logic to skip empty ones. Let's see.
                    // Actually loadModel(filePath) calls loadModel(filePath, transform)
                    // If modelData != null, it converts it and adds it.
                    // GCodeModelParser returns a GCode object, which is not null.
                    
                    assertTrue(app.loadedModelPaths.size >= formats.length, "Not all formats were loaded: " + app.loadedModelPaths.size);

                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        if (errorRef.get() != null) {
            if (errorRef.get() instanceof RuntimeException) throw (RuntimeException) errorRef.get();
            if (errorRef.get() instanceof Error) throw (Error) errorRef.get();
            throw new RuntimeException(errorRef.get());
        }
    }
}
