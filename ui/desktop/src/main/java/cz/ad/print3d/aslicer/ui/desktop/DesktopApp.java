package cz.ad.print3d.aslicer.ui.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cz.ad.print3d.aslicer.logic.model.format.mf3.core.Mf3Model;
import cz.ad.print3d.aslicer.logic.model.format.stl.StlModel;
import cz.ad.print3d.aslicer.logic.model.parser.mf3.Mf3Parser;
import cz.ad.print3d.aslicer.logic.model.parser.stl.StlParser;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class DesktopApp implements ApplicationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DesktopApp.class);
    static Path configPath = Paths.get(System.getProperty("user.home"), ".aSlicer-desktop.properties");

    PerspectiveCamera cam;
    CameraInputController camController;
    ModelBatch modelBatch;
    Model model;
    ModelInstance instance;
    Environment environment;
    Stage stage;
    Skin skin;
    Window settingsWindow;
    int currentWidth;
    int currentHeight;
    String lastDir = "";
    String currentModelPath;

    public static void main(String[] args) {
        LOGGER.info("aSlicer Desktop application starting");
        Properties props = loadConfig();
        int width = getIntProperty(props, "window.width", 800);
        int height = getIntProperty(props, "window.height", 600);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("aSlicer Desktop");
        config.setWindowedMode(width, height);
        new Lwjgl3Application(new DesktopApp(), config);
    }

    static Properties loadConfig() {
        Properties props = new Properties();
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                props.load(is);
            } catch (IOException e) {
                LOGGER.debug("Could not load config: {}", e.getMessage());
            }
        }
        return props;
    }

    static int getIntProperty(Properties props, String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static float getFloatProperty(Properties props, String key, float defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static void saveConfig(int width, int height) {
        if (width <= 0 || height <= 0) return;
        Properties props = loadConfig();
        props.setProperty("window.width", String.valueOf(width));
        props.setProperty("window.height", String.valueOf(height));
        saveConfig(props);
    }

    static void saveConfig(Properties props) {
        try (OutputStream os = Files.newOutputStream(configPath)) {
            props.store(os, "aSlicer Desktop Settings");
        } catch (IOException e) {
            LOGGER.error("Could not save config: {}", e.getMessage());
        }
    }

    void saveAllConfig() {
        Properties props = loadConfig();
        props.setProperty("window.width", String.valueOf(currentWidth));
        props.setProperty("window.height", String.valueOf(currentHeight));
        if (lastDir != null && !lastDir.isEmpty()) {
            props.setProperty("last.dir", lastDir);
        }
        if (currentModelPath != null && !currentModelPath.isEmpty()) {
            props.setProperty("last.file", currentModelPath);
        } else {
            props.remove("last.file");
        }
        if (camController != null) {
            props.setProperty("control.rotateButton", String.valueOf(camController.rotateButton));
            props.setProperty("control.translateButton", String.valueOf(camController.translateButton));
            props.setProperty("control.forwardButton", String.valueOf(camController.forwardButton));
            props.setProperty("control.forwardKey", String.valueOf(camController.forwardKey));
            props.setProperty("control.backwardKey", String.valueOf(camController.backwardKey));

            // Camera state
            props.setProperty("camera.pos.x", String.valueOf(cam.position.x));
            props.setProperty("camera.pos.y", String.valueOf(cam.position.y));
            props.setProperty("camera.pos.z", String.valueOf(cam.position.z));

            props.setProperty("camera.dir.x", String.valueOf(cam.direction.x));
            props.setProperty("camera.dir.y", String.valueOf(cam.direction.y));
            props.setProperty("camera.dir.z", String.valueOf(cam.direction.z));

            props.setProperty("camera.up.x", String.valueOf(cam.up.x));
            props.setProperty("camera.up.y", String.valueOf(cam.up.y));
            props.setProperty("camera.up.z", String.valueOf(cam.up.z));

            props.setProperty("camera.target.x", String.valueOf(camController.target.x));
            props.setProperty("camera.target.y", String.valueOf(camController.target.y));
            props.setProperty("camera.target.z", String.valueOf(camController.target.z));
        }
        saveConfig(props);
    }

    @Override
    public void create() {
        Properties props = loadConfig();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        modelBatch = new ModelBatch();

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (props.containsKey("camera.pos.x")) {
            cam.position.set(
                getFloatProperty(props, "camera.pos.x", 10f),
                getFloatProperty(props, "camera.pos.y", 10f),
                getFloatProperty(props, "camera.pos.z", 10f)
            );
            cam.direction.set(
                getFloatProperty(props, "camera.dir.x", 0f),
                getFloatProperty(props, "camera.dir.y", 0f),
                getFloatProperty(props, "camera.dir.z", -1f)
            );
            cam.up.set(
                getFloatProperty(props, "camera.up.x", 0f),
                getFloatProperty(props, "camera.up.y", 1f),
                getFloatProperty(props, "camera.up.z", 0f)
            );
        } else {
            cam.position.set(10f, 10f, 10f);
            cam.lookAt(0, 0, 0);
        }
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        camController = new CameraInputController(cam);
        if (props.containsKey("camera.target.x")) {
            camController.target.set(
                getFloatProperty(props, "camera.target.x", 0f),
                getFloatProperty(props, "camera.target.y", 0f),
                getFloatProperty(props, "camera.target.z", 0f)
            );
        }
        camController.rotateButton = getIntProperty(props, "control.rotateButton", Input.Buttons.LEFT);
        camController.translateButton = getIntProperty(props, "control.translateButton", Input.Buttons.RIGHT);
        camController.forwardButton = getIntProperty(props, "control.forwardButton", Input.Buttons.MIDDLE);
        camController.forwardKey = getIntProperty(props, "control.forwardKey", Input.Keys.W);
        camController.backwardKey = getIntProperty(props, "control.backwardKey", Input.Keys.S);
        lastDir = props.getProperty("last.dir", "");
        String lastFile = props.getProperty("last.file");

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

        Table menuBar = new Table();
        menuBar.setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.7f)));

        ImageButton.ImageButtonStyle clearStyle = new ImageButton.ImageButtonStyle();
        clearStyle.up = skin.newDrawable("white", Color.GRAY);
        clearStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        clearStyle.imageUp = createClearIcon();

        ImageButton clearButton = new ImageButton(clearStyle);
        clearButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                instance = null;
                currentModelPath = null;
            }
        });

        ImageButton.ImageButtonStyle openStyle = new ImageButton.ImageButtonStyle();
        openStyle.up = skin.newDrawable("white", Color.GRAY);
        openStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        openStyle.imageUp = createOpenIcon();

        ImageButton openButton = new ImageButton(openStyle);
        openButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer filters = stack.mallocPointer(3);
                    filters.put(stack.UTF8("*.stl"));
                    filters.put(stack.UTF8("*.ast"));
                    filters.put(stack.UTF8("*.3mf"));
                    filters.flip();

                    String result = TinyFileDialogs.tinyfd_openFileDialog("Open 3D Model File", lastDir, filters, "3D Model Files", false);
                    if (result != null) {
                        Path path = Paths.get(result);
                        if (Files.exists(path)) {
                            Path parent = path.getParent();
                            if (parent != null) {
                                lastDir = parent.toAbsolutePath().toString();
                            }
                        }
                        loadModel(result);
                    }
                }
            }
        });

        ImageButton.ImageButtonStyle settingsStyle = new ImageButton.ImageButtonStyle();
        settingsStyle.up = skin.newDrawable("white", Color.GRAY);
        settingsStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        settingsStyle.imageUp = createSettingsIcon();

        ImageButton settingsButton = new ImageButton(settingsStyle);
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleSettingsWindow();
            }
        });

        menuBar.add(clearButton).pad(5);
        menuBar.add(openButton).pad(5);
        menuBar.add().expandX();
        menuBar.add(settingsButton).pad(5);
        menuBar.left();

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

    private Drawable createSettingsIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.LIGHT_GRAY);
        pixmap.fillCircle(16, 16, 8);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            int x = 16 + (int) (Math.cos(angle) * 11);
            int y = 16 + (int) (Math.sin(angle) * 11);
            pixmap.fillCircle(x, y, 4);
        }
        pixmap.setColor(Color.BLACK);
        pixmap.fillCircle(16, 16, 4);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("settingsIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private void toggleSettingsWindow() {
        if (settingsWindow == null) {
            settingsWindow = createSettingsWindow();
            stage.addActor(settingsWindow);
        } else {
            settingsWindow.setVisible(!settingsWindow.isVisible());
            if (settingsWindow.isVisible()) {
                settingsWindow.toFront();
            }
        }
    }

    private Window createSettingsWindow() {
        Window window = new Window("Settings", skin);
        window.setMovable(true);
        window.setResizable(true);
        window.setSize(300, 250);
        window.setPosition(Gdx.graphics.getWidth() / 2f - 150, Gdx.graphics.getHeight() / 2f - 125);

        Table content = new Table();
        content.pad(10);

        content.add(new Label("Rotate Button (0=L, 1=R, 2=M):", skin)).left();
        TextButton rotateBtn = new TextButton(String.valueOf(camController.rotateButton), skin);
        rotateBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                camController.rotateButton = (camController.rotateButton + 1) % 3;
                rotateBtn.setText(String.valueOf(camController.rotateButton));
            }
        });
        content.add(rotateBtn).width(50).padLeft(10).row();

        content.add(new Label("Pan Button (0=L, 1=R, 2=M):", skin)).left();
        TextButton panBtn = new TextButton(String.valueOf(camController.translateButton), skin);
        panBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                camController.translateButton = (camController.translateButton + 1) % 3;
                panBtn.setText(String.valueOf(camController.translateButton));
            }
        });
        content.add(panBtn).width(50).padLeft(10).row();

        content.add(new Label("Zoom Button (0=L, 1=R, 2=M):", skin)).left();
        TextButton zoomBtn = new TextButton(String.valueOf(camController.forwardButton), skin);
        zoomBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                camController.forwardButton = (camController.forwardButton + 1) % 3;
                zoomBtn.setText(String.valueOf(camController.forwardButton));
            }
        });
        content.add(zoomBtn).width(50).padLeft(10).row();

        content.add(new Label("Forward Key:", skin)).left();
        TextButton forwardBtn = new TextButton(Input.Keys.toString(camController.forwardKey), skin);
        forwardBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Simple toggle for demonstration
                camController.forwardKey = (camController.forwardKey == Input.Keys.W) ? Input.Keys.UP : Input.Keys.W;
                forwardBtn.setText(Input.Keys.toString(camController.forwardKey));
            }
        });
        content.add(forwardBtn).width(80).padLeft(10).row();

        content.add(new Label("Backward Key:", skin)).left();
        TextButton backwardBtn = new TextButton(Input.Keys.toString(camController.backwardKey), skin);
        backwardBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                camController.backwardKey = (camController.backwardKey == Input.Keys.S) ? Input.Keys.DOWN : Input.Keys.S;
                backwardBtn.setText(Input.Keys.toString(camController.backwardKey));
            }
        });
        content.add(backwardBtn).width(80).padLeft(10).row();

        TextButton saveButton = new TextButton("Save", skin);
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveAllConfig();
                window.setVisible(false);
            }
        });

        content.add(saveButton).colspan(2).padTop(20);

        window.add(content).expand().fill();
        return window;
    }

    private Drawable createClearIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.drawRectangle(0, 0, 32, 32);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("clearIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Drawable createOpenIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.LIGHT_GRAY);
        pixmap.fill();
        pixmap.setColor(Color.BLACK);
        pixmap.drawRectangle(0, 0, 32, 32);
        pixmap.fillRectangle(4, 8, 24, 16);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("openIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private void loadModel(String filePath) {
        if (filePath == null) return;
        if (filePath.toLowerCase().endsWith(".3mf")) {
            Mf3Model mf3Model = loadMf3Model(filePath);
            if (mf3Model != null) {
                LOGGER.info("Loaded 3MF model with {} objects", mf3Model.objects().size());
                currentModelPath = filePath;
                if (model != null) {
                    model.dispose();
                }
                model = Mf3GdxConverter.convertToGdxModel(mf3Model);
                instance = new ModelInstance(model);
            } else {
                LOGGER.warn("Failed to load 3MF: {}", filePath);
            }
        } else {
            StlModel stlModel = loadStlModel(filePath);
            if (stlModel != null) {
                LOGGER.info("Loaded STL model with {} facets", stlModel.facetCount());
                currentModelPath = filePath;
                if (model != null) {
                    model.dispose();
                }
                model = StlGdxConverter.convertToGdxModel(stlModel);
                instance = new ModelInstance(model);
            } else {
                LOGGER.warn("Failed to load STL: {}", filePath);
            }
        }
    }

    private Mf3Model loadMf3Model(String pathStr) {
        Path path = Paths.get(pathStr);
        if (!java.nio.file.Files.exists(path)) {
            LOGGER.error("3MF file not found at: {}", path.toAbsolutePath());
            return null;
        }
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            Mf3Parser parser = new Mf3Parser();
            return parser.parse(channel);
        } catch (IOException e) {
            LOGGER.error("Error parsing 3MF file: {}", pathStr, e);
            return null;
        }
    }

    private StlModel loadStlModel(String pathStr) {
        Path path = Paths.get(pathStr);
        if (!java.nio.file.Files.exists(path)) {
            LOGGER.error("STL file not found at: {}", path.toAbsolutePath());
            return null;
        }
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            StlParser parser = new StlParser();
            return parser.parse(channel);
        } catch (IOException e) {
            LOGGER.error("Error parsing STL file: {}", pathStr, e);
            return null;
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

        if (instance != null) {
            modelBatch.begin(cam);
            modelBatch.render(instance, environment);
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
        if (model != null) {
            model.dispose();
        }
        stage.dispose();
        skin.dispose();
    }
}
