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
package cz.ad.print3d.aslicer.ui.desktop.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import cz.ad.print3d.aslicer.logic.core.ModelGdxConverter;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParserFactory;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfig;
import cz.ad.print3d.aslicer.ui.desktop.persistence.ScenePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ModelManager handles the collection of loaded models, their instances, and selection logic.
 * It provides methods for loading, removing, and duplicating models in the 3D scene.
 */
public class ModelManager implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelManager.class);

    private final Array<Model> models = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();
    private final Array<String> loadedModelPaths = new Array<>();
    private final Array<Integer> selectedIndices = new Array<>();

    private final AppConfig appConfig;
    private final ScenePersistence scenePersistence;

    private final Vector3 tempVector = new Vector3();
    private final BoundingBox tempBounds = new BoundingBox();

    /**
     * Interface for listening to model collection changes.
     */
    public interface ModelManagerListener {
        /**
         * Called when a new model is added.
         */
        void onModelsChanged();
        
        /**
         * Called when model selection changes.
         */
        void onSelectionChanged();
    }

    private final Array<ModelManagerListener> listeners = new Array<>();

    /**
     * Creates a new ModelManager.
     *
     * @param appConfig        the application configuration
     * @param scenePersistence the scene persistence handler
     */
    public ModelManager(AppConfig appConfig, ScenePersistence scenePersistence) {
        this.appConfig = appConfig;
        this.scenePersistence = scenePersistence;
    }

    /**
     * Adds a listener for model changes.
     *
     * @param listener the listener to add
     */
    public void addListener(ModelManagerListener listener) {
        listeners.add(listener);
    }

    /**
     * Loads a model from the specified file path and adds it to the scene.
     *
     * @param filePath the path to the model file to load
     */
    public void loadModel(String filePath) {
        loadModel(filePath, null);
    }

    /**
     * Loads a 3D model from the given path, converts it to a GDX model, and adds it to the scene
     * with an optional pre-defined transformation.
     *
     * @param filePath  the path to the 3D model file to load
     * @param transform the optional transformation to apply; if null, automatic placement is used
     */
    public void loadModel(String filePath, Matrix4 transform) {
        if (filePath == null) return;
        Path path = Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            LOGGER.error("Model file not found at: {}", path.toAbsolutePath());
            return;
        }

        try {
            cz.ad.print3d.aslicer.logic.model.Model modelData = ModelParserFactory.parse(path);
            if (modelData != null) {
                LOGGER.info("Loading model from {}", filePath);
                loadedModelPaths.add(filePath);

                Model gdxModel = ModelGdxConverter.convertToGdxModel(modelData);
                ModelInstance gdxInstance = new ModelInstance(gdxModel);

                if (transform != null) {
                    gdxInstance.transform.set(transform);
                    LOGGER.debug("Applying pre-defined transform for {}", filePath);
                } else {
                    placeNearExisting(gdxInstance);
                }

                models.add(gdxModel);
                instances.add(gdxInstance);
                
                notifyModelsChanged();
                saveScene();
                LOGGER.info("Successfully loaded and placed model from {}", filePath);
            }
        } catch (IOException e) {
            LOGGER.error("Error parsing model file: {}", filePath, e);
        }
    }

    /**
     * Removes a model at the specified index from the scene.
     *
     * @param index the index of the model to remove
     */
    public void removeModel(int index) {
        if (index < 0 || index >= instances.size) return;

        LOGGER.info("Removing model at index {}: {}", index, loadedModelPaths.get(index));

        instances.removeIndex(index);
        Model model = models.removeIndex(index);
        model.dispose();
        loadedModelPaths.removeIndex(index);

        // Update selection
        selectedIndices.removeValue(index, false);
        for (int i = 0; i < selectedIndices.size; i++) {
            int selIdx = selectedIndices.get(i);
            if (selIdx > index) {
                selectedIndices.set(i, selIdx - 1);
            }
        }

        notifyModelsChanged();
        notifySelectionChanged();
        saveScene();
    }

    /**
     * Duplicates the model at the specified index.
     *
     * @param index the index of the model to duplicate
     */
    public void duplicateModel(int index) {
        if (index < 0 || index >= loadedModelPaths.size) return;

        String path = loadedModelPaths.get(index);
        LOGGER.info("Duplicating model at index {}: {}", index, path);
        loadModel(path);
    }

    /**
     * Identifies the model instance intersected by the given ray.
     *
     * @param ray the ray to test for intersection
     * @return the index of the intersected model instance, or -1 if no intersection is found
     */
    public int getObject(Ray ray) {
        int result = -1;
        float distance = -1;

        for (int i = 0; i < instances.size; ++i) {
            final ModelInstance instance = instances.get(i);
            instance.calculateBoundingBox(tempBounds);
            tempBounds.mul(instance.transform);

            if (Intersector.intersectRayBounds(ray, tempBounds, tempVector)) {
                float dist2 = ray.origin.dst2(tempVector);
                if (result == -1 || dist2 < distance) {
                    result = i;
                    distance = dist2;
                }
            }
        }
        return result;
    }

    /**
     * Updates the material highlights of all model instances in the scene
     * based on the current selection.
     */
    public void updateHighlights() {
        for (int i = 0; i < instances.size; i++) {
            ModelInstance inst = instances.get(i);
            Model model = models.get(i);
            boolean selected = selectedIndices.contains(i, false);

            for (int j = 0; j < inst.materials.size; j++) {
                Material instMat = inst.materials.get(j);
                if (selected) {
                    instMat.set(ColorAttribute.createDiffuse(Color.ORANGE));
                } else {
                    Material modelMat = model.materials.get(j);
                    instMat.set(modelMat.get(ColorAttribute.Diffuse));
                }
            }
        }
    }

    /**
     * Places the new model instance near already existing objects to avoid collision.
     *
     * @param newInstance the model instance to position
     */
    public void placeNearExisting(ModelInstance newInstance) {
        BoundingBox newBounds = new BoundingBox();
        newInstance.calculateBoundingBox(newBounds);

        float offsetZ = -newBounds.min.z;
        float offsetX = 0;

        if (instances.isEmpty()) {
            LOGGER.debug("First model, placing at origin (X=0, Y=0) with Z adjusted to 0");
        } else {
            float distance = appConfig.loadToDto().getDistance();
            BoundingBox totalBounds = new BoundingBox();
            totalBounds.inf();

            for (ModelInstance inst : instances) {
                BoundingBox currentBounds = new BoundingBox();
                inst.calculateBoundingBox(currentBounds);
                currentBounds.mul(inst.transform);
                totalBounds.ext(currentBounds);
            }

            float currentMaxX = totalBounds.max.x;
            float newMinX = newBounds.min.x;

            offsetX = currentMaxX - newMinX + distance;
            LOGGER.info("Placed new model near existing ones with offset X: {}. Configured distance: {}", offsetX, distance);
        }

        newInstance.transform.setToTranslation(offsetX, 0, offsetZ);

        if (offsetZ != 0) {
            LOGGER.info("Detected model bottom Z at {}, moved to 0 by offset: {}", newBounds.min.z, offsetZ);
        }
    }

    private void notifyModelsChanged() {
        for (ModelManagerListener listener : listeners) {
            listener.onModelsChanged();
        }
    }

    private void notifySelectionChanged() {
        for (ModelManagerListener listener : listeners) {
            listener.onSelectionChanged();
        }
    }

    private void saveScene() {
        scenePersistence.saveScene(loadedModelPaths, instances);
    }

    /**
     * Returns the array of model instances.
     *
     * @return the instances array
     */
    public Array<ModelInstance> getInstances() {
        return instances;
    }

    /**
     * Returns the array of loaded model file paths.
     *
     * @return the loaded model paths array
     */
    public Array<String> getLoadedModelPaths() {
        return loadedModelPaths;
    }

    /**
     * Returns the array of selected model indices.
     *
     * @return the selected indices array
     */
    public Array<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    /**
     * Removes all models from the scene and clears the selection.
     * This operation is persisted to the workspace.
     */
    public void clearModels() {
        LOGGER.info("Clearing all models from the scene");
        for (Model model : models) {
            model.dispose();
        }
        models.clear();
        instances.clear();
        loadedModelPaths.clear();
        selectedIndices.clear();

        notifyModelsChanged();
        notifySelectionChanged();
        saveScene();
    }

    @Override
    public void dispose() {
        for (Model model : models) {
            model.dispose();
        }
        models.clear();
        instances.clear();
        loadedModelPaths.clear();
        selectedIndices.clear();
    }
}
