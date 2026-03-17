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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.UBJsonReader;
import com.badlogic.gdx.utils.UBJsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles the persistence of the 3D scene state using LibGDX's binary UBJSON format.
 * This class saves and loads the list of models and their transformations to a workspace.g3db file.
 * The use of .g3db extension follows the convention for GDX binary model files, which are UBJSON based.
 */
public class ScenePersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenePersistence.class);
    
    /**
     * Path to the workspace file. Package-private to allow override in tests.
     */
    public static Path WORKSPACE_PATH = Paths.get(System.getProperty("user.home"), ".aslicer", "workspace.g3db");

    /**
     * Represents an entry in the scene, containing the file path of the model and its transformation matrix.
     */
    public static class SceneEntry {
        /**
         * The absolute or relative path to the model file.
         */
        public String filePath;
        
        /**
         * The transformation matrix (position, rotation, scale) of the model instance.
         */
        public Matrix4 transform = new Matrix4();

        /**
         * Default constructor for serialization.
         */
        public SceneEntry() {}

        /**
         * Creates a new scene entry with the specified file path and transformation.
         *
         * @param filePath the path to the model file
         * @param transform the transformation matrix
         */
        public SceneEntry(String filePath, Matrix4 transform) {
            this.filePath = filePath;
            this.transform.set(transform);
        }
    }

    /**
     * Saves the current state of the scene to the workspace file.
     * This includes all model paths and their respective world transformations.
     * Uses LibGDX's UBJsonWriter to create a binary representation of the scene.
     *
     * @param modelPaths an array of strings containing the file paths of loaded models
     * @param instances an array of ModelInstance objects containing the scene's 3D models and their transforms
     */
    public void saveScene(Array<String> modelPaths, Array<ModelInstance> instances) {
        if (modelPaths == null || instances == null) {
            LOGGER.error("Cannot save scene: modelPaths or instances is null");
            return;
        }
        
        if (modelPaths.size != instances.size) {
            LOGGER.error("Mismatch between model paths and instances count ({} vs {})", modelPaths.size, instances.size);
            return;
        }

        FileHandle file = getFileHandle();
        try {
            if (!file.parent().exists()) {
                file.parent().mkdirs();
            }
            
            try (UBJsonWriter writer = new UBJsonWriter(file.write(false))) {
                writer.object();
                writer.array("models");
                for (int i = 0; i < modelPaths.size; i++) {
                    writer.object();
                    writer.set("path", modelPaths.get(i));
                    writer.set("transform", instances.get(i).transform.val);
                    writer.pop();
                }
                writer.pop();
                writer.pop();
            }
            LOGGER.info("Scene successfully saved to {}", WORKSPACE_PATH);
        } catch (Exception e) {
            LOGGER.error("Failed to save scene to {}: {}", WORKSPACE_PATH, e.getMessage());
        }
    }

    /**
     * Loads the scene state from the workspace file.
     * If the file does not exist or an error occurs during loading, an empty array is returned.
     * Uses LibGDX's UBJsonReader to parse the binary workspace file.
     *
     * @return an array of SceneEntry objects representing the loaded scene
     */
    public Array<SceneEntry> loadScene() {
        Array<SceneEntry> entries = new Array<>();
        FileHandle file = getFileHandle();
        
        if (!file.exists()) {
            LOGGER.debug("No workspace file found at {}, starting with empty scene", WORKSPACE_PATH);
            return entries;
        }

        try {
            UBJsonReader reader = new UBJsonReader();
            JsonValue root = reader.parse(file);
            JsonValue models = root.get("models");
            if (models != null) {
                for (JsonValue entry = models.child; entry != null; entry = entry.next) {
                    String path = entry.getString("path");
                    float[] transform = entry.get("transform").asFloatArray();
                    if (path != null && transform != null && transform.length == 16) {
                        entries.add(new SceneEntry(path, new Matrix4(transform)));
                    }
                }
            }
            LOGGER.info("Successfully loaded {} models from workspace", entries.size);
        } catch (Exception e) {
            LOGGER.error("Failed to load scene from {}: {}", WORKSPACE_PATH, e.getMessage());
        }
        
        return entries;
    }

    /**
     * Helper method to get the FileHandle for the workspace path.
     * Handles both GDX-initialized and non-initialized environments (for basic tests).
     *
     * @return a FileHandle pointing to the workspace file
     */
    private FileHandle getFileHandle() {
        if (Gdx.files == null) {
            // Fallback for tests if Gdx.files is not initialized
            return new FileHandle(WORKSPACE_PATH.toFile());
        }
        return Gdx.files.absolute(WORKSPACE_PATH.toAbsolutePath().toString());
    }
}
