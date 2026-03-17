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

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cz.ad.print3d.aslicer.logic.core.ModelGdxConverter;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParserFactory;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfig;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfigDto;
import cz.ad.print3d.aslicer.ui.desktop.persistence.ScenePersistence;
import cz.ad.print3d.aslicer.ui.desktop.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main application class for the desktop user interface.
 * Handles the 3D visualization, user interactions, and configuration management.
 */
public class DesktopApp implements ApplicationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DesktopApp.class);
    /**
     * Application configuration handler.
     */
    public final AppConfig appConfig;

    /**
     * File dialog for selecting models.
     */
    private final ModelFileDialog fileDialog;

    /**
     * Scene persistence handler for workspace saving/loading.
     */
    private final ScenePersistence scenePersistence;

    /**
     * The perspective camera used for 3D visualization.
     */
    public PerspectiveCamera cam;

    /**
     * Controller for handling user camera input.
     */
    public CameraInputController camController;

    /**
     * Batch for rendering models.
     */
    public ModelBatch modelBatch;

    /**
     * List of loaded GDX models.
     */
    public final Array<Model> models = new Array<>();

    /**
     * List of rendered model instances in the scene.
     */
    public final Array<ModelInstance> instances = new Array<>();

    /**
     * List of file paths for the loaded models.
     */
    public final Array<String> loadedModelPaths = new Array<>();

    /**
     * Temporary vector for calculations.
     */
    private final Vector3 tempVector = new Vector3();

    /**
     * Temporary bounding box for calculations.
     */
    private final BoundingBox tempBounds = new BoundingBox();

    /**
     * 3D environment including lights.
     */
    public Environment environment;

    /**
     * 2D stage for UI components.
     */
    public Stage stage;

    /**
     * UI skin for styling.
     */
    public Skin skin;

    /**
     * Window for application settings.
     */
    public SettingsWindow settingsWindow;

    /**
     * Window for listing loaded models.
     */
    public ModelListWindow modelListWindow;

    /**
     * Grid rendered on the ground.
     */
    public AppGrid appGrid;

    /**
     * Current width of the application window.
     */
    public int currentWidth;

    /**
     * Current height of the application window.
     */
    public int currentHeight;

    /**
     * Last used directory for opening models.
     */
    public String lastDir = "";

    /**
     * Path to the last opened model.
     */
    public String currentModelPath;

    public DesktopApp() {
        this.appConfig = new AppConfig();
        this.fileDialog = new ModelFileDialog();
        this.scenePersistence = new ScenePersistence();
    }

    public static void main(String[] args) {
        LOGGER.info("aSlicer Desktop application starting");
        DesktopApp app = new DesktopApp();
        AppConfigDto dto = app.appConfig.loadToDto();
        int width = dto.getWindowWidth();
        int height = dto.getWindowHeight();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("aSlicer Desktop");
        config.setWindowedMode(width, height);
        new Lwjgl3Application(app, config);
    }

    /**
     * Saves all current application settings to the persistent configuration.
     * This includes window dimensions, the last used directory and file,
     * camera state, and user input settings.
     */
    public void saveAllConfig() {
        AppConfigDto dto = appConfig.loadToDto();
        dto.setWindowWidth(currentWidth);
        dto.setWindowHeight(currentHeight);
        dto.setLastDir(lastDir);
        dto.setLastFile(currentModelPath);

        java.util.List<String> paths = new java.util.ArrayList<>();
        for (String path : loadedModelPaths) {
            paths.add(path);
        }
        dto.setLoadedFiles(paths);
        LOGGER.info("Saving list of loaded files ({}): {}", paths.size(), paths);

        if (appGrid != null) {
            dto.setGridSize(appGrid.getStep());
        }

        if (camController != null) {
            dto.setRotateButton(camController.rotateButton);
            dto.setTranslateButton(camController.translateButton);
            dto.setForwardButton(camController.forwardButton);
            dto.setForwardKey(camController.forwardKey);
            dto.setBackwardKey(camController.backwardKey);

            // Camera state
            dto.setCameraPosX(cam.position.x);
            dto.setCameraPosY(cam.position.y);
            dto.setCameraPosZ(cam.position.z);

            dto.setCameraDirX(cam.direction.x);
            dto.setCameraDirY(cam.direction.y);
            dto.setCameraDirZ(cam.direction.z);

            dto.setCameraUpX(cam.up.x);
            dto.setCameraUpY(cam.up.y);
            dto.setCameraUpZ(cam.up.z);

            dto.setCameraTargetX(camController.target.x);
            dto.setCameraTargetY(camController.target.y);
            dto.setCameraTargetZ(camController.target.z);
        }
        appConfig.saveFromDto(dto);
        scenePersistence.saveScene(loadedModelPaths, instances);
    }

    /**
     * Initializes the application, including the 3D environment, camera, configuration,
     * and UI. It also reloads any models that were active in the previous session.
     * If no models were active (or if this is the first run), the scene starts empty.
     */
    @Override
    public void create() {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        modelBatch = new ModelBatch();

        AppConfigDto dto = appConfig.loadToDto();

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (dto.isCameraStateLoaded()) {
            cam.position.set(dto.getCameraPosX(), dto.getCameraPosY(), dto.getCameraPosZ());
            cam.direction.set(dto.getCameraDirX(), dto.getCameraDirY(), dto.getCameraDirZ());
            cam.up.set(dto.getCameraUpX(), dto.getCameraUpY(), dto.getCameraUpZ());
        } else {
            cam.position.set(10f, 10f, 10f);
            cam.lookAt(0, 0, 0);
        }
        cam.near = 1f;
        cam.far = 1500f;
        cam.update();

        camController = new CameraInputController(cam);
        if (dto.isCameraTargetLoaded()) {
            camController.target.set(dto.getCameraTargetX(), dto.getCameraTargetY(), dto.getCameraTargetZ());
        }
        camController.rotateButton = dto.getRotateButton();
        camController.translateButton = dto.getTranslateButton();
        camController.forwardButton = dto.getForwardButton();

        appGrid = new AppGrid(dto.getGridSize());
        camController.forwardKey = dto.getForwardKey();
        camController.backwardKey = dto.getBackwardKey();
        lastDir = dto.getLastDir();

        setupUI();

        InputMultiplexer multiplexer = new InputMultiplexer();
        if (stage != null) {
            multiplexer.addProcessor(stage);
        }
        multiplexer.addProcessor(camController);
        Gdx.input.setInputProcessor(multiplexer);

        loadInitialScene();
    }

    /**
     * Loads the initial scene state on application startup.
     * It first attempts to load the scene from the workspace file (workspace.g3db).
     * If no workspace file is found, it falls back to the list of loaded files from the application configuration.
     * This method ensures that the 3D scene is restored to its previous state, including model transformations.
     */
    public void loadInitialScene() {
        AppConfigDto dto = appConfig.loadToDto();
        Array<ScenePersistence.SceneEntry> savedScene = scenePersistence.loadScene();
        if (savedScene.size > 0) {
            LOGGER.info("Restoring scene from workspace ({} models)", savedScene.size);
            for (ScenePersistence.SceneEntry entry : savedScene) {
                loadModel(entry.filePath, entry.transform);
            }
        } else {
            java.util.List<String> loadedFiles = dto.getLoadedFiles();
            if (!loadedFiles.isEmpty()) {
                LOGGER.info("No workspace found, falling back to reloading {} files from config", loadedFiles.size());
                for (String file : loadedFiles) {
                    loadModel(file);
                }
            } else {
                LOGGER.info("Starting with a clean scene");
            }
        }
    }

    /**
     * Sets up the user interface components (stage, skin, toolbar, etc.).
     */
    public void setupUI() {
        stage = new Stage(new ScreenViewport());
        skin = createSkin();

        Table root = new Table();
        root.top().left();
        root.setFillParent(true);

        AppToolbar menuBar = new AppToolbar(skin, new AppToolbar.ToolbarListener() {
            @Override
            public void onClear() {
                for (Model m : models) {
                    m.dispose();
                }
                models.clear();
                instances.clear();
                loadedModelPaths.clear();
                currentModelPath = null;
                if (modelListWindow != null) {
                    modelListWindow.updateList();
                }
                saveAllConfig();
                LOGGER.info("Cleared all models");
            }

            @Override
            public void onOpen() {
                String[] results = fileDialog.showOpenDialog(lastDir);
                if (results != null && results.length > 0) {
                    String lastFilePath = results[results.length - 1];
                    String newDir = fileDialog.extractDirectory(lastFilePath);
                    if (newDir != null) {
                        lastDir = newDir;
                    }
                    for (String result : results) {
                        loadModel(result);
                    }
                }
            }

            @Override
            public void onSettings() {
                toggleSettingsWindow();
            }
        });

        AppSideToolbar sideBar = new AppSideToolbar(skin, new AppSideToolbar.SideToolbarListener() {
            @Override
            public void onToggleModelList() {
                toggleModelListWindow();
            }
        });

        root.add(menuBar).expandX().fillX().colspan(2).row();
        root.add(sideBar).expandY().fillY().left();
        root.add().expand().fill();

        stage.addActor(root);
    }

    private Skin createSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.font = font;
        skin.add("default", textButtonStyle);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.BLACK;
        listStyle.fontColorUnselected = Color.WHITE;
        listStyle.selection = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("default", listStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        skin.add("default", scrollPaneStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        windowStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.9f));
        skin.add("default", windowStyle);

        return skin;
    }

    /**
     * Toggles the visibility of the settings window.
     */
    public void toggleSettingsWindow() {
        if (settingsWindow == null) {
            float initialGridSize = appGrid != null ? appGrid.getStep() : 5.0f;
            settingsWindow = new SettingsWindow(skin, camController, initialGridSize, this::updateGrid, this::saveAllConfig);
            stage.addActor(settingsWindow);
        } else {
            settingsWindow.setVisible(!settingsWindow.isVisible());
            if (settingsWindow.isVisible()) {
                settingsWindow.toFront();
            }
        }
    }

    /**
     * Toggles the visibility of the model list window.
     */
    public void toggleModelListWindow() {
        if (modelListWindow == null) {
            modelListWindow = new ModelListWindow(skin, loadedModelPaths, new ModelListWindow.ModelListListener() {
                @Override
                public void onRemoveModel(int index) {
                    removeModel(index);
                }

                @Override
                public void onDuplicateModel(int index) {
                    duplicateModel(index);
                }
            });
            stage.addActor(modelListWindow);
        } else {
            modelListWindow.setVisible(!modelListWindow.isVisible());
            if (modelListWindow.isVisible()) {
                modelListWindow.toFront();
                modelListWindow.updateList();
            }
        }
    }

    /**
     * Removes a loaded model from the application state, disposing of its resources
     * and updating the scene and UI accordingly.
     *
     * @param index the index of the model to remove from the loaded files
     */
    public void removeModel(int index) {
        if (index < 0 || index >= loadedModelPaths.size) return;

        LOGGER.info("Removing model at index {}: {}", index, loadedModelPaths.get(index));

        loadedModelPaths.removeIndex(index);
        Model gdxModel = models.removeIndex(index);
        if (gdxModel != null) {
            gdxModel.dispose();
        }
        instances.removeIndex(index);

        if (modelListWindow != null) {
            modelListWindow.updateList();
        }
        scenePersistence.saveScene(loadedModelPaths, instances);
    }

    /**
     * Duplicates a loaded model at the specified index, creating a new instance
     * in the scene and adding it to the list of loaded models.
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
     * Updates the grid with a new step size.
     * Recreates the grid model if the step size has changed.
     *
     * @param newStep the new distance between grid lines
     */
    private void updateGrid(float newStep) {
        if (appGrid != null) {
            if (Math.abs(appGrid.getStep() - newStep) < 0.0001f) return;
            appGrid.dispose();
        }
        appGrid = new AppGrid(newStep);
        LOGGER.info("Grid updated to step size: {}", newStep);
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
     * If the transformation is null, the model is automatically placed near already loaded instances.
     * This method automatically triggers a scene persistence save on success.
     *
     * @param filePath the path to the 3D model file to load
     * @param transform the optional transformation to apply; if null, automatic placement is used
     */
    public void loadModel(String filePath, com.badlogic.gdx.math.Matrix4 transform) {
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
                currentModelPath = filePath;
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
                if (modelListWindow != null) {
                    modelListWindow.updateList();
                }
                scenePersistence.saveScene(loadedModelPaths, instances);
                LOGGER.info("Successfully loaded and placed model from {}", filePath);
            }
        } catch (IOException e) {
            LOGGER.error("Error parsing model file: {}", filePath, e);
        }
    }

    /**
     * Places the new model instance near already existing objects to avoid collision
     * and ensures the bottom part of the model is placed at Z=0 coordinates.
     *
     * <p>This method calculates the bounding box of the new instance and detects if its
     * bottom part (minimum Z coordinate) is different from zero. If so, it computes
     * the necessary Z-offset to bring it to the ground level (Z=0). For horizontal
     * placement, it uses the configured distance to position the model along the X-axis
     * relative to the currently loaded models.</p>
     *
     * @param newInstance the model instance to position; its transform matrix will be updated
     */
    void placeNearExisting(ModelInstance newInstance) {
        // Calculate bounding box of the new instance in its local space
        BoundingBox newBounds = new BoundingBox();
        newInstance.calculateBoundingBox(newBounds);

        // Calculate offset to bring bottom Z to 0
        float offsetZ = -newBounds.min.z;
        float offsetX = 0;

        if (instances.isEmpty()) {
            LOGGER.debug("First model, placing at origin (X=0, Y=0) with Z adjusted to 0");
        } else {
            float distance = appConfig.loadToDto().getDistance();

            // Calculate total bounding box of all existing instances in world space
            BoundingBox totalBounds = new BoundingBox();
            totalBounds.inf();

            for (ModelInstance inst : instances) {
                BoundingBox currentBounds = new BoundingBox();
                inst.calculateBoundingBox(currentBounds);
                currentBounds.mul(inst.transform);
                totalBounds.ext(currentBounds);
            }

            // Place along X axis after the existing objects
            float currentMaxX = totalBounds.max.x;
            float newMinX = newBounds.min.x;

            offsetX = currentMaxX - newMinX + distance;
            LOGGER.info("Placed new model near existing ones with offset X: {}. Configured distance: {}", offsetX, distance);
        }

        // Apply translation
        newInstance.transform.setToTranslation(offsetX, 0, offsetZ);

        if (offsetZ != 0) {
            LOGGER.info("Detected model bottom Z at {}, moved to 0 by offset: {}", newBounds.min.z, offsetZ);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (width > 0 && height > 0) {
            currentWidth = width;
            currentHeight = height;
        }
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(cam);
        if (appGrid != null) {
            modelBatch.render(appGrid.getInstance());
        }
        if (instances.size > 0) {
            modelBatch.render(instances, environment);
        }
        modelBatch.end();

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        saveAllConfig();
        if (appGrid != null) {
            appGrid.dispose();
        }
        modelBatch.dispose();
        for (Model m : models) {
            m.dispose();
        }
        models.clear();
        instances.clear();
        stage.dispose();
        skin.dispose();
    }
}
