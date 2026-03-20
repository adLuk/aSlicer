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
import cz.ad.print3d.aslicer.ui.desktop.config.*;
import cz.ad.print3d.aslicer.ui.desktop.persistence.*;
import cz.ad.print3d.aslicer.ui.desktop.view.*;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the application persists all loaded files including order and multiplicity,
 * and that on the next run the same sequence is restored leading to the same object placement.
 */
public class DesktopAppLoadedFilesTest {
    @TempDir
    Path tempDir;

    private Path originalConfigPath;
    private Path originalWorkspacePath;

    @BeforeEach
    void setUp() {
        originalConfigPath = AppConfig.CONFIG_PATH;
        AppConfig.CONFIG_PATH = tempDir.resolve(".aslicer").resolve("loaded-files.properties");
        originalWorkspacePath = ScenePersistence.WORKSPACE_PATH;
        ScenePersistence.WORKSPACE_PATH = tempDir.resolve(".aslicer").resolve("workspace.g3db");
    }

    @AfterEach
    void tearDown() {
        AppConfig.CONFIG_PATH = originalConfigPath;
        ScenePersistence.WORKSPACE_PATH = originalWorkspacePath;
    }

    @Test
    void testLoadedFilesPersistenceAndPositions() throws InterruptedException {
        // First run: load files and save config
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef1 = new AtomicReference<>();
        AtomicReference<List<Float>> positionsXRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    app.create();
                    java.nio.file.Path fileAPath = java.nio.file.Paths.get("..", "..", "logic", "model", "src", "test", "resources", "stl", "test-binary.stl").toAbsolutePath().normalize();
                    java.nio.file.Path fileBPath = java.nio.file.Paths.get("..", "..", "logic", "model", "src", "test", "resources", "stl", "test-ascii.stl").toAbsolutePath().normalize();
                    String fileA = fileAPath.toString();
                    String fileB = fileBPath.toString();

                    // Load sequence with duplicates: A, B, A
                    app.modelManager.loadModel(fileA);
                    app.modelManager.loadModel(fileB);
                    app.modelManager.loadModel(fileA);

                    assertEquals(3, app.modelManager.getInstances().size);

                    List<Float> xs = new ArrayList<>();
                    Vector3 tmp = new Vector3();
                    for (int i = 0; i < app.modelManager.getInstances().size; i++) {
                        app.modelManager.getInstances().get(i).transform.getTranslation(tmp);
                        xs.add(tmp.x);
                    }
                    positionsXRef.set(xs);

                    // Persist configuration (including ordered file list)
                    app.saveAllConfig();
                } catch (Throwable t) {
                    errorRef1.set(t);
                } finally {
                    latch1.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch1.await(10, TimeUnit.SECONDS));
        if (errorRef1.get() != null) {
            if (errorRef1.get() instanceof RuntimeException) throw (RuntimeException) errorRef1.get();
            if (errorRef1.get() instanceof Error) throw (Error) errorRef1.get();
            throw new RuntimeException(errorRef1.get());
        }

        List<Float> firstRunXs = positionsXRef.get();
        assertNotNull(firstRunXs);
        assertEquals(3, firstRunXs.size());

        // Second run: create app and let it reload all files from config
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef2 = new AtomicReference<>();
        AtomicReference<List<Float>> positionsXRef2 = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GdxTestUtils.mockGdxGL();
                    DesktopApp app = new DesktopApp();
                    app.create();
                    // Recreate loaded files sequence from persisted configuration
                    java.util.List<String> files = app.appConfig.loadToDto().getLoadedFiles();
                    // Clear initially loaded if any (though it should be empty in this test setup)
                    app.modelManager.getInstances().clear();
                    for (String f : files) {
                        app.modelManager.loadModel(f);
                    }

                    assertEquals(3, app.modelManager.getInstances().size);

                    List<Float> xs = new ArrayList<>();
                    Vector3 tmp = new Vector3();
                    for (int i = 0; i < app.modelManager.getInstances().size; i++) {
                        app.modelManager.getInstances().get(i).transform.getTranslation(tmp);
                        xs.add(tmp.x);
                    }
                    positionsXRef2.set(xs);
                } catch (Throwable t) {
                    errorRef2.set(t);
                } finally {
                    latch2.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch2.await(10, TimeUnit.SECONDS));
        if (errorRef2.get() != null) {
            if (errorRef2.get() instanceof RuntimeException) throw (RuntimeException) errorRef2.get();
            if (errorRef2.get() instanceof Error) throw (Error) errorRef2.get();
            throw new RuntimeException(errorRef2.get());
        }

        List<Float> secondRunXs = positionsXRef2.get();
        assertNotNull(secondRunXs);
        assertEquals(3, secondRunXs.size());

        // The X translations must match exactly to reproduce placement
        for (int i = 0; i < firstRunXs.size(); i++) {
            assertEquals(firstRunXs.get(i), secondRunXs.get(i), 0.0001f);
        }

        // Also verify that the persisted list preserves order and multiplicity
        AppConfig cfg = new AppConfig();
        AppConfigDto dto = cfg.loadToDto();
        assertEquals(3, dto.getLoadedFiles().size());
        String expectedA = java.nio.file.Paths.get("..", "..", "logic", "model", "src", "test", "resources", "stl", "test-binary.stl").toAbsolutePath().normalize().toString();
        String expectedB = java.nio.file.Paths.get("..", "..", "logic", "model", "src", "test", "resources", "stl", "test-ascii.stl").toAbsolutePath().normalize().toString();
        assertEquals(expectedA, dto.getLoadedFiles().get(0));
        assertEquals(expectedB, dto.getLoadedFiles().get(1));
        assertEquals(expectedA, dto.getLoadedFiles().get(2));
    }
}
