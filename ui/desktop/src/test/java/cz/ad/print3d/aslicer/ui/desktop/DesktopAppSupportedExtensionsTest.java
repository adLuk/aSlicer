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
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    app.create();

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
                        app.modelManager.loadModel(fullPath.toString());
                    }

                    assertTrue(app.modelManager.getLoadedModelPaths().size >= formats.length, "Not all formats were loaded: " + app.modelManager.getLoadedModelPaths().size);

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
