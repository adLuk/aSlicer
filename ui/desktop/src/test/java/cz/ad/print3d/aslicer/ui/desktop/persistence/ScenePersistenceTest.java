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
import cz.ad.print3d.aslicer.ui.desktop.DesktopApp;
import cz.ad.print3d.aslicer.ui.desktop.config.*;
import cz.ad.print3d.aslicer.ui.desktop.persistence.*;
import cz.ad.print3d.aslicer.ui.desktop.view.*;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
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
 * Unit tests for ScenePersistence class.
 */
public class ScenePersistenceTest {

    @TempDir
    Path tempDir;

    private Path originalWorkspacePath;
    private ScenePersistence persistence;

    @BeforeEach
    void setUp() {
        originalWorkspacePath = ScenePersistence.WORKSPACE_PATH;
        ScenePersistence.WORKSPACE_PATH = tempDir.resolve(".aslicer").resolve("workspace.g3db");
        persistence = new ScenePersistence();
    }

    @AfterEach
    void tearDown() {
        ScenePersistence.WORKSPACE_PATH = originalWorkspacePath;
    }

    @Test
    void testSaveAndLoadEmptyScene() {
        Array<String> paths = new Array<>();
        Array<ModelInstance> instances = new Array<>();

        persistence.saveScene(paths, instances);
        assertTrue(Files.exists(ScenePersistence.WORKSPACE_PATH), "Workspace file should be created");

        Array<ScenePersistence.SceneEntry> loaded = persistence.loadScene();
        assertEquals(0, loaded.size, "Loaded scene should be empty");
    }

    @Test
    void testSaveAndLoadSceneWithModels() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    Array<String> paths = new Array<>();
                    paths.add("model1.stl");
                    paths.add("model2.stl");

                    // In headless mode we can create a basic Model
                    Model model = new Model();
                    Array<ModelInstance> instances = new Array<>();

                    ModelInstance inst1 = new ModelInstance(model);
                    inst1.transform.setToTranslation(1.5f, 2.5f, 3.5f);
                    instances.add(inst1);

                    ModelInstance inst2 = new ModelInstance(model);
                    inst2.transform.setToTranslation(10f, 20f, 30f);
                    inst2.transform.rotate(Vector3.Y, 45f);
                    instances.add(inst2);

                    persistence.saveScene(paths, instances);
                    assertTrue(Files.exists(ScenePersistence.WORKSPACE_PATH), "Workspace file should exist after saving");

                    Array<ScenePersistence.SceneEntry> loaded = persistence.loadScene();
                    assertEquals(2, loaded.size, "Should load 2 models");

                    assertEquals("model1.stl", loaded.get(0).filePath);
                    Vector3 pos1 = new Vector3();
                    loaded.get(0).transform.getTranslation(pos1);
                    assertEquals(1.5f, pos1.x, 0.001f);
                    assertEquals(2.5f, pos1.y, 0.001f);
                    assertEquals(3.5f, pos1.z, 0.001f);

                    assertEquals("model2.stl", loaded.get(1).filePath);
                    Vector3 pos2 = new Vector3();
                    loaded.get(1).transform.getTranslation(pos2);
                    assertEquals(10f, pos2.x, 0.001f);
                    assertEquals(20f, pos2.y, 0.001f);
                    assertEquals(30f, pos2.z, 0.001f);
                    
                    // Verify rotation is also preserved by checking a point
                    Vector3 v = new Vector3(1, 0, 0);
                    v.mul(loaded.get(1).transform);
                    // After translation 10,20,30 and rotate Y 45, (1,0,0) becomes (10+cos45, 20, 30-sin45)
                    // cos 45 = sin 45 = 0.707
                    assertEquals(10.707f, v.x, 0.001f);
                    assertEquals(30 - 0.7071f, v.z, 0.001f);

                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                    Gdx.app.exit();
                }
            }
        }, new HeadlessApplicationConfiguration());

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out");
        if (errorRef.get() != null) {
            fail(errorRef.get());
        }
    }

    @Test
    void testLoadMissingFile() {
        assertFalse(Files.exists(ScenePersistence.WORKSPACE_PATH));
        Array<ScenePersistence.SceneEntry> loaded = persistence.loadScene();
        assertNotNull(loaded);
        assertEquals(0, loaded.size, "Should return empty array for missing file");
    }

    @Test
    void testSaveMismatch() {
        Array<String> paths = new Array<>();
        paths.add("onlyPath.stl");
        Array<ModelInstance> instances = new Array<>(); // Empty, mismatch

        persistence.saveScene(paths, instances);
        assertFalse(Files.exists(ScenePersistence.WORKSPACE_PATH), "Should not save if there is a mismatch");
    }

    @Test
    void testSaveHandlesNulls() {
        persistence.saveScene(null, null);
        assertFalse(Files.exists(ScenePersistence.WORKSPACE_PATH), "Should not save if inputs are null");
    }

    @Test
    void testDirectoryCreation() {
        Path deepPath = tempDir.resolve("a").resolve("b").resolve("workspace.g3db");
        ScenePersistence.WORKSPACE_PATH = deepPath;
        
        Array<String> paths = new Array<>();
        Array<ModelInstance> instances = new Array<>();

        persistence.saveScene(paths, instances);
        assertTrue(Files.exists(deepPath), "Should create parent directories and file");
    }
}
