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
package cz.ad.print3d.aslicer.ui.desktop.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import cz.ad.print3d.aslicer.ui.desktop.I18N;
import cz.ad.print3d.aslicer.ui.desktop.model.ModelManager;
import cz.ad.print3d.aslicer.ui.desktop.view.wizard.PrinterConnectionStep;
import cz.ad.print3d.aslicer.ui.desktop.view.wizard.PrinterDiscoveryStep;
import cz.ad.print3d.aslicer.ui.desktop.view.wizard.Wizard;

import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

/**
 * DesktopUI manages the application's user interface using LibGDX Stage.
 * It separates the UI into distinct stages:
 * <ul>
 *   <li><b>Menu Stage:</b> Contains permanent UI elements like the top toolbar and side toolbar.</li>
 *   <li><b>View Stages:</b> Multiple stages representing different views (e.g., Model View, Grid View).</li>
 *   <li><b>Dialog Stage:</b> Contains transient windows and dialogs (e.g., Settings, Model List).</li>
 * </ul>
 * This separation ensures that dialogs and menus are always rendered on top of the main content
 * and simplifies input handling and layout management, keeping the main 3D scene uncluttered.
 */
public class DesktopUI implements Disposable {

    private final Stage menuStage;
    private final Stage dialogStage;
    private final Stage view1Stage;
    private final Stage view2Stage;
    private int activeViewIndex = 0;
    private final Skin skin;
    private final Table rootTable;
    private SettingsWindow settingsWindow;
    private ModelListWindow modelListWindow;
    private Wizard printerWizard;
    private AppToolbar toolbar;

    /**
     * Creates a new DesktopUI instance.
     * Initializes the skin and all stages (menu, views, and dialog).
     */
    public DesktopUI() {
        this.skin = createSkin();
        this.menuStage = new Stage(new ScreenViewport());
        this.dialogStage = new Stage(new ScreenViewport());
        this.view1Stage = new Stage(new ScreenViewport());
        this.view2Stage = new Stage(new ScreenViewport());

        this.rootTable = new Table();
        this.rootTable.top().left();
        this.rootTable.setFillParent(true);
        this.rootTable.setTouchable(Touchable.childrenOnly);
        this.menuStage.addActor(rootTable);
    }

    /**
     * Initializes the UI layout with the provided toolbars and the 3D scene.
     *
     * @param toolbar       the top application toolbar
     * @param stageToolbar   the stage application toolbar
     * @param sideToolbar    the side application toolbar
     * @param sceneManager  the manager for 3D scene components
     * @param modelManager  the manager for loaded models
     */
    public void setupLayout(AppToolbar toolbar, AppStageToolbar stageToolbar, AppSideToolbar sideToolbar, SceneManager sceneManager, ModelManager modelManager) {
        this.toolbar = toolbar;
        rootTable.clear();
        rootTable.add(toolbar).expandX().fillX().colspan(2).row();
        rootTable.add(sideToolbar).expandY().fillY();
        rootTable.add(stageToolbar).expandX().fillX().top();

        // Setup View 1 (Models + Grid)
        view1Stage.clear();
        Table view1Table = new Table();
        view1Table.setFillParent(true);
        view1Table.add(new SceneActor(sceneManager, modelManager)).expand().fill();
        view1Stage.addActor(view1Table);

        // Setup View 2 (Just Grid)
        view2Stage.clear();
        Table view2Table = new Table();
        view2Table.setFillParent(true);
        view2Table.add(new SceneActor(sceneManager, null)).expand().fill();
        view2Stage.addActor(view2Table);
    }

    /**
     * Creates a font that supports all required character ranges.
     * @return the generated BitmapFont
     */
    public BitmapFont createFont() {
        if (Gdx.files.internal("fonts/NotoSans-Regular.ttf").exists()) {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSans-Regular.ttf"));
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = 16;
            
            Set<Character> charSet = new HashSet<>();
            for (char c : FreeTypeFontGenerator.DEFAULT_CHARS.toCharArray()) {
                charSet.add(c);
            }
            
            // Collect all unique characters from localization files
            String[] suffixes = {"", "_cs", "_de", "_en", "_es", "_sk", "_th", "_uk", "_zh"};
            for (String suffix : suffixes) {
                FileHandle file = Gdx.files.internal("i18n/messages" + suffix + ".properties");
                if (file.exists()) {
                    Properties props = new Properties();
                    try (InputStreamReader reader = new InputStreamReader(file.read(), "UTF-8")) {
                        props.load(reader);
                        for (Object value : props.values()) {
                            String s = value.toString();
                            for (int i = 0; i < s.length(); i++) {
                                char c = s.charAt(i);
                                if (!Character.isWhitespace(c) && !Character.isISOControl(c)) {
                                    charSet.add(c);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            StringBuilder sb = new StringBuilder();
            for (Character c : charSet) {
                sb.append(c);
            }
            parameter.characters = sb.toString();
            
            BitmapFont font = generator.generateFont(parameter);
            generator.dispose();
            return font;
        }
        // Fallback to default if font file is missing
        return new BitmapFont();
    }

    /**
     * Creates the default UI skin.
     *
     * @return the created Skin object
     */
    protected Skin createSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        
        BitmapFont font = createFont();
        skin.add("default", font);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.font = font;
        skin.add("default", textButtonStyle);

        Button.ButtonStyle buttonStyle = new Button.ButtonStyle();
        buttonStyle.up = skin.newDrawable("white", Color.LIGHT_GRAY);
        buttonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", buttonStyle);

        ImageButton.ImageButtonStyle imageButtonStyle = new ImageButton.ImageButtonStyle();
        imageButtonStyle.up = skin.newDrawable("white", Color.LIGHT_GRAY);
        imageButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        skin.add("default", imageButtonStyle);

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = Color.BLACK;
        listStyle.fontColorUnselected = Color.WHITE;
        listStyle.selection = skin.newDrawable("white", Color.LIGHT_GRAY);
        skin.add("default", listStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        Drawable scrollBackground = skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.5f));
        scrollPaneStyle.background = scrollBackground;
        
        Drawable scrollBar = skin.newDrawable("white", Color.GRAY);
        scrollBar.setMinWidth(10);
        scrollBar.setMinHeight(10);
        scrollPaneStyle.vScroll = scrollBar;
        scrollPaneStyle.hScroll = scrollBar;

        Drawable scrollKnob = skin.newDrawable("white", Color.LIGHT_GRAY);
        scrollKnob.setMinWidth(10);
        scrollKnob.setMinHeight(10);
        scrollPaneStyle.vScrollKnob = scrollKnob;
        scrollPaneStyle.hScrollKnob = scrollKnob;
        skin.add("default", scrollPaneStyle);

        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        windowStyle.titleFontColor = Color.WHITE;
        Drawable windowBackground = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.9f));
        windowBackground.setLeftWidth(10);
        windowBackground.setRightWidth(10);
        windowBackground.setTopHeight(30);
        windowBackground.setBottomHeight(10);
        windowStyle.background = windowBackground;
        skin.add("default", windowStyle);

        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        Drawable checkboxOn = skin.newDrawable("white", Color.GREEN);
        checkboxOn.setMinWidth(16);
        checkboxOn.setMinHeight(16);
        checkBoxStyle.checkboxOn = checkboxOn;
        Drawable checkboxOff = skin.newDrawable("white", Color.RED);
        checkboxOff.setMinWidth(16);
        checkboxOff.setMinHeight(16);
        checkBoxStyle.checkboxOff = checkboxOff;
        checkBoxStyle.font = font;
        skin.add("default", checkBoxStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        textFieldStyle.cursor = skin.newDrawable("white", Color.LIGHT_GRAY);
        textFieldStyle.selection = skin.newDrawable("white", Color.BLUE);
        skin.add("default", textFieldStyle);

        return skin;
    }

    /**
     * Toggles the visibility of the settings window.
     *
     * @param camController           the camera controller to configure
     * @param initialGridSize        the initial grid size from configuration
     * @param initialProtectedData   the initial protected data flag from configuration
     * @param gridSizeCallback       the callback to update the grid size
     * @param protectedDataCallback  the callback to update the protected data flag
     * @param saveCallback            the callback to save configuration
     */
    public void toggleSettingsWindow(CameraInputController camController, float initialGridSize, boolean initialProtectedData, Consumer<Float> gridSizeCallback, Consumer<Boolean> protectedDataCallback, Runnable saveCallback) {
        if (settingsWindow == null) {
            settingsWindow = new SettingsWindow(skin, camController, initialGridSize, initialProtectedData, gridSizeCallback, protectedDataCallback, saveCallback);
            addDialog(settingsWindow);
        } else {
            settingsWindow.setVisible(!settingsWindow.isVisible());
        }
    }

    /**
     * Toggles the visibility of the model list window.
     *
     * @param modelPaths   the current list of loaded model paths
     * @param logicModels  the collection of logic models with detailed data
     * @param listener     the listener for model list operations
     */
    public void toggleModelListWindow(Array<String> modelPaths, Array<cz.ad.print3d.aslicer.logic.model.Model> logicModels, ModelListWindow.ModelListListener listener) {
        if (modelListWindow == null) {
            modelListWindow = new ModelListWindow(skin, modelPaths, logicModels, listener);
            addDialog(modelListWindow);
        } else {
            modelListWindow.setVisible(!modelListWindow.isVisible());
        }
    }

    /**
     * Toggles the visibility of the printer discovery wizard.
     *
     * @param connectionPool    the application-scope connection pool
     * @param printerRepository the repository for storing printer configurations
     * @param initialWidth      the initial width of the wizard
     * @param initialHeight     the initial height of the wizard
     * @param sizeCallback      a callback to be invoked when the wizard size changes or it is closed
     */
    public void togglePrinterDiscoveryWindow(cz.ad.print3d.aslicer.logic.net.PrinterConnectionPool connectionPool, cz.ad.print3d.aslicer.logic.printer.PrinterRepository printerRepository, int initialWidth, int initialHeight, java.util.function.BiConsumer<Integer, Integer> sizeCallback) {
        if (printerWizard == null) {
            printerWizard = new Wizard(I18N.get("wizard.printer.title"), skin, initialWidth, initialHeight);
            PrinterDiscoveryStep discoveryStep = new PrinterDiscoveryStep(skin);
            PrinterConnectionStep connectionStep = new PrinterConnectionStep(skin, discoveryStep, connectionPool);
            cz.ad.print3d.aslicer.ui.desktop.view.wizard.PrinterSaveStep saveStep = new cz.ad.print3d.aslicer.ui.desktop.view.wizard.PrinterSaveStep(skin, connectionStep, printerRepository);
            
            printerWizard.addStep(discoveryStep);
            printerWizard.addStep(connectionStep);
            printerWizard.addStep(saveStep);
            
            printerWizard.setListener(new Wizard.WizardListener() {
                @Override
                public void onFinish(Wizard wizard) {
                    // Logic to handle finished wizard, e.g. adding printers to ModelManager
                    saveStep.savePrinters();
                    if (toolbar != null) {
                        toolbar.refreshPrinters();
                    }
                    System.out.println(I18N.get("wizard.printer.finished"));
                    if (sizeCallback != null) {
                        sizeCallback.accept((int) wizard.getWidth(), (int) wizard.getHeight());
                    }
                    printerWizard = null;
                }

                @Override
                public void onCancel(Wizard wizard) {
                    System.out.println(I18N.get("wizard.printer.cancelled"));
                    if (sizeCallback != null) {
                        sizeCallback.accept((int) wizard.getWidth(), (int) wizard.getHeight());
                    }
                    printerWizard = null;
                }
            });

            centerWindow(printerWizard);
            addDialog(printerWizard);
        } else {
            printerWizard.setVisible(!printerWizard.isVisible());
            if (printerWizard.isVisible()) {
                centerWindow(printerWizard);
            } else {
                if (sizeCallback != null) {
                    sizeCallback.accept((int) printerWizard.getWidth(), (int) printerWizard.getHeight());
                }
            }
        }
    }

    private void centerWindow(Window window) {
        window.setPosition(
            com.badlogic.gdx.Gdx.graphics.getWidth() / 2f - window.getWidth() / 2f,
            com.badlogic.gdx.Gdx.graphics.getHeight() / 2f - window.getHeight() / 2f
        );
    }

    /**
     * Returns the model list window, if initialized.
     *
     * @return the model list window, or null
     */
    public ModelListWindow getModelListWindow() {
        return modelListWindow;
    }

    /**
     * @return the printer discovery wizard
     */
    public Wizard getPrinterWizard() {
        return printerWizard;
    }

    /**
     * Adds a window or dialog to the dialog stage.
     *
     * @param window the window to add
     */
    public void addDialog(Window window) {
        dialogStage.addActor(window);
    }

    /**
     * Returns the menu stage.
     *
     * @return the menu stage
     */
    public Stage getMenuStage() {
        return menuStage;
    }

    /**
     * Returns the dialog stage.
     *
     * @return the dialog stage
     */
    public Stage getDialogStage() {
        return dialogStage;
    }

    /**
     * Returns the currently active view stage.
     *
     * @return the active view stage
     */
    public Stage getActiveViewStage() {
        return (activeViewIndex == 0) ? view1Stage : view2Stage;
    }

    /**
     * Switches the active view.
     *
     * @param index the index of the view to switch to (0 for Model, 1 for Grid)
     */
    public void setActiveView(int index) {
        this.activeViewIndex = index;
    }

    /**
     * Returns the UI skin.
     *
     * @return the skin
     */
    public Skin getSkin() {
        return skin;
    }

    /**
     * Updates and renders all active stages.
     *
     * @param delta the time in seconds since the last render
     */
    public void render(float delta) {
        getActiveViewStage().act(delta);
        getActiveViewStage().draw();

        menuStage.act(delta);
        menuStage.draw();

        dialogStage.act(delta);
        dialogStage.draw();
    }

    /**
     * Updates the viewports of all stages when the screen is resized.
     *
     * @param width  the new screen width
     * @param height the new screen height
     */
    public void resize(int width, int height) {
        view1Stage.getViewport().update(width, height, true);
        view2Stage.getViewport().update(width, height, true);
        menuStage.getViewport().update(width, height, true);
        dialogStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (printerWizard != null) {
            printerWizard.dispose();
            printerWizard.remove();
        }
        view1Stage.dispose();
        view2Stage.dispose();
        menuStage.dispose();
        dialogStage.dispose();
        skin.dispose();
    }
}
