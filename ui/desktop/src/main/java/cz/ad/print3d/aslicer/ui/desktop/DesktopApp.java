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
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cz.ad.print3d.aslicer.logic.core.ModelGdxConverter;
import cz.ad.print3d.aslicer.logic.model.parser.ModelParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main application class for the desktop user interface.
 * Handles the 3D visualization, user interactions, and configuration management.
 */
public class DesktopApp implements ApplicationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DesktopApp.class);
    final AppConfig appConfig;
    private final ModelFileDialog fileDialog;

    PerspectiveCamera cam;
    CameraInputController camController;
    ModelBatch modelBatch;
    final Array<Model> models = new Array<>();
    final Array<ModelInstance> instances = new Array<>();
    private final Vector3 tempVector = new Vector3();
    private final BoundingBox tempBounds = new BoundingBox();
    Environment environment;
    Stage stage;
    Skin skin;
    SettingsWindow settingsWindow;
    int currentWidth;
    int currentHeight;
    String lastDir = "";
    String currentModelPath;

    public DesktopApp() {
        this.appConfig = new AppConfig();
        this.fileDialog = new ModelFileDialog();
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
    void saveAllConfig() {
        AppConfigDto dto = appConfig.loadToDto();
        dto.setWindowWidth(currentWidth);
        dto.setWindowHeight(currentHeight);
        dto.setLastDir(lastDir);
        dto.setLastFile(currentModelPath);

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
    }

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
        cam.far = 300f;
        cam.update();

        camController = new CameraInputController(cam);
        if (dto.isCameraTargetLoaded()) {
            camController.target.set(dto.getCameraTargetX(), dto.getCameraTargetY(), dto.getCameraTargetZ());
        }
        camController.rotateButton = dto.getRotateButton();
        camController.translateButton = dto.getTranslateButton();
        camController.forwardButton = dto.getForwardButton();
        camController.forwardKey = dto.getForwardKey();
        camController.backwardKey = dto.getBackwardKey();
        lastDir = dto.getLastDir();
        String lastFile = dto.getLastFile();

        setupUI();

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(camController);
        Gdx.input.setInputProcessor(multiplexer);

        String stlPath = "logic/model/src/test/resources/stl/test-binary.stl";
        if (lastFile != null && !lastFile.isEmpty() && Files.exists(Paths.get(lastFile))) {
            stlPath = lastFile;
        }
        loadModel(stlPath);
    }

    private void setupUI() {
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
                currentModelPath = null;
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

        root.add(menuBar).expandX().fillX();

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

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        windowStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.9f));
        skin.add("default", windowStyle);

        return skin;
    }

    private void toggleSettingsWindow() {
        if (settingsWindow == null) {
            settingsWindow = new SettingsWindow(skin, camController, this::saveAllConfig);
            stage.addActor(settingsWindow);
        } else {
            settingsWindow.setVisible(!settingsWindow.isVisible());
            if (settingsWindow.isVisible()) {
                settingsWindow.toFront();
            }
        }
    }

    /**
     * Loads a 3D model from the specified file path and adds it to the current scene.
     * The model is automatically placed near existing objects to avoid collision.
     *
     * @param filePath the path to the 3D model file to load
     */
    private void loadModel(String filePath) {
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

                Model gdxModel = ModelGdxConverter.convertToGdxModel(modelData);
                ModelInstance gdxInstance = new ModelInstance(gdxModel);

                placeNearExisting(gdxInstance);

                models.add(gdxModel);
                instances.add(gdxInstance);
                LOGGER.info("Successfully loaded and placed model from {}", filePath);
            }
        } catch (IOException e) {
            LOGGER.error("Error parsing model file: {}", filePath, e);
        }
    }

    /**
     * Places the new model instance near already existing objects to avoid collision.
     * Uses the configured distance from settings.
     *
     * @param newInstance the model instance to position
     */
    void placeNearExisting(ModelInstance newInstance) {
        if (instances.isEmpty()) {
            LOGGER.debug("First model, placing at origin");
            newInstance.transform.idt();
            return;
        }

        float distance = appConfig.loadToDto().getDistance();

        // Calculate total bounding box of all existing instances
        BoundingBox totalBounds = new BoundingBox();
        totalBounds.inf();

        for (ModelInstance inst : instances) {
            BoundingBox currentBounds = new BoundingBox();
            inst.calculateBoundingBox(currentBounds);
            currentBounds.mul(inst.transform);
            totalBounds.ext(currentBounds);
        }

        // Calculate bounding box of the new instance
        BoundingBox newBounds = new BoundingBox();
        newInstance.calculateBoundingBox(newBounds);

        // Place along X axis
        float currentMaxX = totalBounds.max.x;
        float newMinX = newBounds.min.x;

        float offsetX = currentMaxX - newMinX + distance;
        newInstance.transform.setToTranslation(offsetX, 0, 0);

        LOGGER.info("Placed new model near existing ones with offset X: {}. Configured distance: {}", offsetX, distance);
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

        if (instances.size > 0) {
            modelBatch.begin(cam);
            modelBatch.render(instances, environment);
            modelBatch.end();
        }

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
