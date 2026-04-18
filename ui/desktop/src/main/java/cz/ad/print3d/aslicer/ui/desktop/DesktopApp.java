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

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import cz.ad.print3d.aslicer.logic.core.security.SecurityInitializer;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfig;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfigDto;
import cz.ad.print3d.aslicer.ui.desktop.model.ModelManager;
import cz.ad.print3d.aslicer.ui.desktop.persistence.ScenePersistence;
import cz.ad.print3d.aslicer.ui.desktop.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    /**
     * Main application class for the desktop user interface.
     * This class implements {@link ApplicationListener} and serves as the central hub
     * for the LibGDX application life cycle. It handles the 3D visualization, user
     * interactions, and configuration management. It coordinates several logical
     * blocks, including scene management, model management, and the user interface.
     */
public class DesktopApp implements ApplicationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DesktopApp.class);

    /**
     * Application configuration handler.
     * Responsible for loading and saving application-wide settings such as
     * window dimensions, last used directories, and UI preferences.
     */
    public final AppConfig appConfig;

    /**
     * File dialog for selecting models.
     * Provides a native-like interface for the user to browse and select
     * 3D model files (e.g., STL, OBJ) for import.
     */
    private final ModelFileDialog fileDialog;

    /**
     * Scene persistence handler for workspace saving/loading.
     * Manages the serialization and deserialization of the entire 3D scene,
     * including model positions, rotations, and scales.
     */
    private final ScenePersistence scenePersistence;

    /**
     * Scene manager for 3D components like camera, lighting, and grid.
     * Handles the setup and rendering of the 3D world, camera movement,
     * and visualization of the build plate grid.
     */
    public SceneManager sceneManager;

    /**
     * Model manager for handling models, instances, and selection logic.
     * Maintains the list of loaded 3D models and their instances,
     * and handles ray-casting for model selection.
     */
    public ModelManager modelManager;

    /**
     * Repository for printer configurations.
     * Provides access to definitions of supported 3D printers, including
     * their build volume, connection protocols, and capabilities.
     */
    public cz.ad.print3d.aslicer.logic.printer.PrinterRepository printerRepository;

    /**
     * Application-scope pool for managing active printer connections.
     * Handles the lifecycle of connections to 3D printers.
     */
    public cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool connectionPool;

    /**
     * Desktop UI manager for handling stages, menus, and dialogs.
     * Manages the 2D overlay (Scene2D.ui), including toolbars, windows,
     * and interaction between the UI and the 3D scene.
     */
    public DesktopUI desktopUI;

    /**
     * Main input multiplexer for the application.
     * Dispatches input events to both the 2D UI stages and the 3D scene
     * input processors in a prioritized manner.
     */
    private InputMultiplexer multiplexer;

    /**
     * Input processor for handling model selection in the 3D scene.
     * Listens for touch/click events to determine which 3D model in the scene
     * is being interacted with by the user.
     */
    public InputProcessor selectionProcessor;

    /**
     * Current width of the application window in pixels.
     * Used for layout calculations and resizing the viewport.
     */
    public int currentWidth;

    /**
     * Current height of the application window in pixels.
     * Used for layout calculations and resizing the viewport.
     */
    public int currentHeight;

    /**
     * Last used directory for opening models.
     * Persisted to provide a better user experience when opening multiple files.
     */
    public String lastDir = "";

    /**
     * Path to the last opened model.
     * Used for quick access or restoring the state of the application.
     */
    public String currentModelPath;

    /**
     * Current active view index (0 for Model, 1 for Grid).
     * Determines which view is currently being rendered and which UI elements are shown.
     */
    private int activeView = 0;

    /**
     * Creates a new DesktopApp instance.
     */
    public DesktopApp() {
        this.appConfig = new AppConfig();
        this.fileDialog = new ModelFileDialog();
        this.scenePersistence = new ScenePersistence();
        this.connectionPool = new cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool();
        try {
            java.nio.file.Path printerPath = AppConfig.CONFIG_PATH.getParent().resolve("printers.zip");
            this.printerRepository = new cz.ad.print3d.aslicer.logic.core.printer.ZipPrinterRepository(printerPath, null, true);
        } catch (java.io.IOException e) {
            LOGGER.error("Failed to initialize printer repository", e);
            // Fallback to a simple implementation if Zip fails
            this.printerRepository = new cz.ad.print3d.aslicer.logic.printer.PrinterRepository() {
                private final java.util.Map<String, java.util.Map<String, cz.ad.print3d.aslicer.logic.printer.Printer3D>> groups = new java.util.HashMap<>();
                @Override public java.util.List<String> getGroups() { return new java.util.ArrayList<>(groups.keySet()); }
                @Override public java.util.Map<String, cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrintersByGroup(String groupName) { return groups.getOrDefault(groupName, java.util.Collections.emptyMap()); }
                @Override public java.util.Optional<cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrinter(String groupName, String printerName) { return java.util.Optional.ofNullable(getPrintersByGroup(groupName).get(printerName)); }
                @Override public void savePrinter(String groupName, String printerName, cz.ad.print3d.aslicer.logic.printer.Printer3D printer) { groups.computeIfAbsent(groupName, ignored -> new java.util.HashMap<>()).put(printerName, printer); }
                @Override public boolean deletePrinter(String groupName, String printerName) { return groups.containsKey(groupName) && groups.get(groupName).remove(printerName) != null; }
                @Override public boolean deleteGroup(String groupName) { return groups.remove(groupName) != null; }
            };
        }
    }

    /**
     * Application entry point. Initializes the security layer and starts the
     * desktop application with the configured window dimensions.
     *
     * @param args command line arguments (currently not used)
     */
    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            LOGGER.debug("Application started with arguments: {}", (Object) args);
        }
        SecurityInitializer.init();
        DesktopApp app = new DesktopApp();
        AppConfigDto dto = app.appConfig.loadToDto();
        System.setProperty("aslicer.ssl.trust_system_certs", String.valueOf(dto.isTrustSystemCerts()));

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("aSlicer - 3D model processing tool");
        config.setWindowedMode(dto.getWindowWidth(), dto.getWindowHeight());
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(app, config);
    }

    /**
     * Saves all current application and scene configurations to persistent storage.
     * This includes window state, view settings, camera position, and the list
     * of loaded models. It also triggers scene persistence to save the 3D transforms
     * of all objects in the workspace.
     */
    public void saveAllConfig() {
        AppConfigDto dto = appConfig.loadToDto();
        dto.setWindowWidth(currentWidth);
        dto.setWindowHeight(currentHeight);
        dto.setLastDir(lastDir);
        dto.setLastFile(currentModelPath);

        if (modelManager != null) {
            java.util.List<String> paths = new java.util.ArrayList<>();
            for (String path : modelManager.getLoadedModelPaths()) {
                paths.add(path);
            }
            dto.setLoadedFiles(paths);
        }

        if (sceneManager != null) {
            dto.setGridSize(sceneManager.getAppGrid().getStep());
            dto.setRotateButton(sceneManager.getCameraController().rotateButton);
            dto.setTranslateButton(sceneManager.getCameraController().translateButton);
            dto.setForwardButton(sceneManager.getCameraController().forwardButton);
            dto.setForwardKey(sceneManager.getCameraController().forwardKey);
            dto.setBackwardKey(sceneManager.getCameraController().backwardKey);

            dto.setCameraPosX(sceneManager.getCamera().position.x);
            dto.setCameraPosY(sceneManager.getCamera().position.y);
            dto.setCameraPosZ(sceneManager.getCamera().position.z);
            dto.setCameraDirX(sceneManager.getCamera().direction.x);
            dto.setCameraDirY(sceneManager.getCamera().direction.y);
            dto.setCameraDirZ(sceneManager.getCamera().direction.z);
            dto.setCameraUpX(sceneManager.getCamera().up.x);
            dto.setCameraUpY(sceneManager.getCamera().up.y);
            dto.setCameraUpZ(sceneManager.getCamera().up.z);
            dto.setCameraTargetX(sceneManager.getCameraController().target.x);
            dto.setCameraTargetY(sceneManager.getCameraController().target.y);
            dto.setCameraTargetZ(sceneManager.getCameraController().target.z);
        }
        appConfig.saveFromDto(dto);
        if (modelManager != null) {
            scenePersistence.saveScene(modelManager.getLoadedModelPaths(), modelManager.getInstances());
        }
    }

    /**
     * Called when the application is first created.
     * Initializes core components like the scene manager, model manager,
     * UI stages, and input processors. It also triggers the initial loading
     * of the scene from the configuration or workspace.
     */
    @Override
    public void create() {
        // Start network information collection as early as possible
        new cz.ad.print3d.aslicer.logic.net.info.NetworkInformationCollector().collectAsync();

        AppConfigDto dto = appConfig.loadToDto();
        lastDir = dto.getLastDir();
        currentModelPath = dto.getLastFile();

        sceneManager = new SceneManager(dto);
        modelManager = new ModelManager(appConfig, scenePersistence);
        modelManager.addListener(new ModelManager.ModelManagerListener() {
            @Override
            public void onModelsChanged() {
                if (desktopUI != null && desktopUI.getModelListWindow() != null) {
                    desktopUI.getModelListWindow().updateList();
                }
            }

            @Override
            public void onSelectionChanged() {
                if (desktopUI != null && desktopUI.getModelListWindow() != null) {
                    desktopUI.getModelListWindow().setSelectedIndices(modelManager.getSelectedIndices());
                }
            }
        });

        setupUI();

        selectionProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT && activeView == 0) {
                    Ray ray = sceneManager.getCamera().getPickRay(screenX, screenY);
                    int index = modelManager.getObject(ray);
                    if (index >= 0) {
                        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                            if (modelManager.getSelectedIndices().contains(index, true)) {
                                modelManager.deselectModel(index);
                            } else {
                                modelManager.selectModel(index);
                            }
                        } else {
                            Array<Integer> indices = new Array<>();
                            indices.add(index);
                            modelManager.setSelectedIndices(indices);
                        }
                    } else {
                        if (!Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                            modelManager.clearSelection();
                        }
                    }
                }
                return false;
            }
        };

        multiplexer = new InputMultiplexer();
        if (desktopUI != null) {
            multiplexer.addProcessor(desktopUI.getDialogStage());
            multiplexer.addProcessor(desktopUI.getMenuStage());
            multiplexer.addProcessor(desktopUI.getActiveViewStage());
        }
        multiplexer.addProcessor(selectionProcessor);
        multiplexer.addProcessor(sceneManager.getCameraController());
        Gdx.input.setInputProcessor(multiplexer);

        loadInitialScene();
    }

    /**
     * Updates the input processor based on the current application state.
     * This method re-assembles the InputMultiplexer with the correct stage priorities,
     * including the currently active view stage.
     */
    private void updateInputProcessor() {
        if (multiplexer != null) {
            multiplexer.clear();
            if (desktopUI != null) {
                multiplexer.addProcessor(desktopUI.getDialogStage());
                multiplexer.addProcessor(desktopUI.getMenuStage());
                multiplexer.addProcessor(desktopUI.getActiveViewStage());
            }
            multiplexer.addProcessor(selectionProcessor);
            multiplexer.addProcessor(sceneManager.getCameraController());
        }
    }

    /**
     * Loads the initial scene state on application startup.
     * Attempts to restore the workspace from the last saved state (models and transforms).
     * If no workspace is found, it falls back to reloading files specified in the
     * application configuration.
     */
    protected void loadInitialScene() {
        AppConfigDto dto = appConfig.loadToDto();
        Array<ScenePersistence.SceneEntry> savedScene = scenePersistence.loadScene();
        if (savedScene.size > 0) {
            LOGGER.info("Restoring scene from workspace ({} models)", savedScene.size);
            for (ScenePersistence.SceneEntry entry : savedScene) {
                modelManager.loadModel(entry.filePath, entry.transform);
            }
        } else {
            java.util.List<String> loadedFiles = dto.getLoadedFiles();
            if (!loadedFiles.isEmpty()) {
                LOGGER.info("No workspace found, falling back to reloading {} files from config", loadedFiles.size());
                for (String file : loadedFiles) {
                    modelManager.loadModel(file);
                }
            } else {
                LOGGER.info("Starting with a clean scene");
            }
        }
    }

    /**
     * Sets up the 2D user interface.
     * Initializes the main toolbars, sidebars, and overlays using Scene2D.ui.
     * Configures listeners for UI events like opening files, clearing the scene,
     * and managing printer discovery.
     */
    protected void setupUI() {
        desktopUI = new DesktopUI();
        AppToolbar toolbar = new AppToolbar(desktopUI.getSkin(), new AppToolbar.ToolbarListener() {
            @Override
            public void onClear() {
                modelManager.clearModels();
                saveAllConfig();
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
                        modelManager.loadModel(result);
                        currentModelPath = result;
                    }
                    saveAllConfig();
                }
            }

            @Override
            public void onSettings() {
                toggleSettingsWindow();
            }

            @Override
            public void onAddPrinter() {
                togglePrinterDiscoveryWindow();
            }
        }, printerRepository);

        AppStageToolbar stageToolbar = new AppStageToolbar(desktopUI.getSkin(), index -> {
            activeView = index;
            if (desktopUI != null) {
                desktopUI.setActiveView(index);
                updateInputProcessor();
            }
        });

        AppSideToolbar sideToolbar = new AppSideToolbar(desktopUI.getSkin(), this::toggleModelListWindow);

        desktopUI.setupLayout(toolbar, stageToolbar, sideToolbar, sceneManager, modelManager);
    }

    /**
     * Toggles the visibility of the settings window.
     */
    public void toggleSettingsWindow() {
        AppConfigDto dto = appConfig.loadToDto();
        desktopUI.toggleSettingsWindow(
            sceneManager.getCameraController(),
            dto.getGridSize(),
            dto.isProtectedData(),
            sceneManager::updateGrid,
            (Boolean protectedData) -> {
                AppConfigDto currentDto = appConfig.loadToDto();
                currentDto.setProtectedData(protectedData);
                appConfig.saveFromDto(currentDto);
            },
            this::saveAllConfig
        );
    }

    /**
     * Toggles the visibility of the model list window.
     */
    public void toggleModelListWindow() {
        desktopUI.toggleModelListWindow(modelManager.getLoadedModelPaths(), modelManager.getLogicModels(), new ModelListWindow.ModelListListener() {
            @Override
            public void onRemoveModel(int index) {
                modelManager.removeModel(index);
            }

            @Override
            public void onDuplicateModel(int index) {
                modelManager.duplicateModel(index);
            }

            @Override
            public void onSelectModels(Array<Integer> indices) {
                modelManager.setSelectedIndices(indices);
            }
        });
        if (desktopUI.getModelListWindow() != null) {
            desktopUI.getModelListWindow().setSelectedIndices(modelManager.getSelectedIndices());
        }
    }

    /**
     * Toggles the visibility of the printer discovery window.
     * Initializes the {@link cz.ad.print3d.aslicer.logic.core.printer.ZipPrinterRepository}
     * with encryption enabled to store printer configurations securely in the user's
     * configuration directory.
     */
    public void togglePrinterDiscoveryWindow() {
        AppConfigDto dto = appConfig.loadToDto();
        desktopUI.togglePrinterDiscoveryWindow(
            connectionPool,
            printerRepository,
            dto.getWizardWidth(),
            dto.getWizardHeight(),
            (width, height) -> {
                AppConfigDto currentDto = appConfig.loadToDto();
                currentDto.setWizardWidth(width);
                currentDto.setWizardHeight(height);
                appConfig.saveFromDto(currentDto);
            }
        );
    }

    /**
     * Called when the application window is resized.
     * Updates the viewport dimensions for both the 3D scene and the 2D UI stages.
     *
     * @param width  new window width in pixels
     * @param height new window height in pixels
     */
    @Override
    public void resize(int width, int height) {
        currentWidth = width;
        currentHeight = height;
        if (sceneManager != null) {
            sceneManager.resize(width, height);
        }
        if (desktopUI != null) {
            desktopUI.resize(width, height);
        }
    }

    private float poolCheckTimer = 0;
    private static final float POOL_CHECK_INTERVAL = 30f; // 30 seconds

    /**
     * Main rendering loop called by LibGDX for each frame.
     * Clears the screen and draws the active UI stages.
     */
    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        
        // Periodically ensure all pooled connections are alive
        poolCheckTimer += delta;
        if (poolCheckTimer >= POOL_CHECK_INTERVAL) {
            poolCheckTimer = 0;
            if (connectionPool != null) {
                connectionPool.ensureAllConnected();
            }
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (desktopUI != null) {
            desktopUI.render(Gdx.graphics.getDeltaTime());
        }
    }

    /**
     * Called when the application is paused (e.g., when the window loses focus).
     */
    @Override
    public void pause() {}

    /**
     * Called when the application is resumed after being paused.
     */
    @Override
    public void resume() {}

    /**
     * Called when the application is destroyed.
     * Saves all configurations and disposes of all resources, including the
     * scene manager, model manager, and UI skin.
     */
    @Override
    public void dispose() {
        saveAllConfig();
        if (connectionPool != null) connectionPool.clear();
        if (sceneManager != null) sceneManager.dispose();
        if (modelManager != null) modelManager.dispose();
        if (desktopUI != null) desktopUI.dispose();
    }
}
