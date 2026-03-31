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
 * Handles the 3D visualization, user interactions, and configuration management.
 * It coordinates several logical blocks, including scene management, model management,
 * and the user interface.
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
     * Scene manager for 3D components like camera, lighting, and grid.
     */
    public SceneManager sceneManager;

    /**
     * Model manager for handling models, instances, and selection logic.
     */
    public ModelManager modelManager;

    /**
     * Repository for printer configurations.
     */
    public cz.ad.print3d.aslicer.logic.printer.PrinterRepository printerRepository;

    /**
     * Desktop UI manager for handling stages, menus, and dialogs.
     */
    public DesktopUI desktopUI;

    /**
     * Main input multiplexer for the application.
     */
    private InputMultiplexer multiplexer;

    /**
     * Input processor for handling model selection in the 3D scene.
     */
    public InputProcessor selectionProcessor;

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

    /**
     * Current active view index (0 for Model, 1 for Grid).
     */
    private int activeView = 0;

    /**
     * Creates a new DesktopApp instance.
     */
    public DesktopApp() {
        this.appConfig = new AppConfig();
        this.fileDialog = new ModelFileDialog();
        this.scenePersistence = new ScenePersistence();
        try {
            this.printerRepository = new cz.ad.print3d.aslicer.logic.core.printer.ZipPrinterRepository(java.nio.file.Paths.get("printers.zip"));
        } catch (java.io.IOException e) {
            LOGGER.error("Failed to initialize printer repository", e);
            // Fallback to a simple implementation if Zip fails
            this.printerRepository = new cz.ad.print3d.aslicer.logic.printer.PrinterRepository() {
                private final java.util.Map<String, java.util.Map<String, cz.ad.print3d.aslicer.logic.printer.Printer3D>> groups = new java.util.HashMap<>();
                @Override public java.util.List<String> getGroups() { return new java.util.ArrayList<>(groups.keySet()); }
                @Override public java.util.Map<String, cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrintersByGroup(String groupName) { return groups.getOrDefault(groupName, java.util.Collections.emptyMap()); }
                @Override public java.util.Optional<cz.ad.print3d.aslicer.logic.printer.Printer3D> getPrinter(String groupName, String printerName) { return java.util.Optional.ofNullable(getPrintersByGroup(groupName).get(printerName)); }
                @Override public void savePrinter(String groupName, String printerName, cz.ad.print3d.aslicer.logic.printer.Printer3D printer) { groups.computeIfAbsent(groupName, k -> new java.util.HashMap<>()).put(printerName, printer); }
                @Override public boolean deletePrinter(String groupName, String printerName) { return groups.containsKey(groupName) && groups.get(groupName).remove(printerName) != null; }
                @Override public boolean deleteGroup(String groupName) { return groups.remove(groupName) != null; }
            };
        }
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SecurityInitializer.init();
        DesktopApp app = new DesktopApp();
        AppConfigDto dto = app.appConfig.loadToDto();

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("aSlicer - 3D model processing tool");
        config.setWindowedMode(dto.getWindowWidth(), dto.getWindowHeight());
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(app, config);
    }

    /**
     * Saves all current application and scene configurations.
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

    @Override
    public void create() {
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
        }, printerRepository);

        AppStageToolbar stageToolbar = new AppStageToolbar(desktopUI.getSkin(), new AppStageToolbar.StageToolbarListener() {
            @Override
            public void onSwitchStage(int index) {
                activeView = index;
                if (desktopUI != null) {
                    desktopUI.setActiveView(index);
                    updateInputProcessor();
                }
            }
        });

        AppSideToolbar sideToolbar = new AppSideToolbar(desktopUI.getSkin(), new AppSideToolbar.SideToolbarListener() {
            @Override
            public void onToggleModelList() {
                toggleModelListWindow();
            }
        });

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

    @Override
    public void render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (desktopUI != null) {
            desktopUI.render(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        saveAllConfig();
        if (sceneManager != null) sceneManager.dispose();
        if (modelManager != null) modelManager.dispose();
        if (desktopUI != null) desktopUI.dispose();
    }
}
