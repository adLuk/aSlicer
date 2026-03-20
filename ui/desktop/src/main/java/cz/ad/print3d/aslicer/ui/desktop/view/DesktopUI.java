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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.function.Consumer;

/**
 * DesktopUI manages the application's user interface using LibGDX Stage.
 * It separates the UI into two distinct stages:
 * <ul>
 *   <li><b>Menu Stage:</b> Contains permanent UI elements like the top toolbar and side toolbar.</li>
 *   <li><b>Dialog Stage:</b> Contains transient windows and dialogs (e.g., Settings, Model List).</li>
 * </ul>
 * This separation ensures that dialogs are always rendered on top of the main menu and simplifies
 * input handling and layout management, keeping the main 3D scene uncluttered.
 */
public class DesktopUI implements Disposable {

    private final Stage menuStage;
    private final Stage dialogStage;
    private final Skin skin;
    private final Table rootTable;
    private SettingsWindow settingsWindow;
    private ModelListWindow modelListWindow;

    /**
     * Creates a new DesktopUI instance.
     * Initializes the skin and both stages (menu and dialog).
     */
    public DesktopUI() {
        this.skin = createSkin();
        this.menuStage = new Stage(new ScreenViewport());
        this.dialogStage = new Stage(new ScreenViewport());

        this.rootTable = new Table();
        this.rootTable.top().left();
        this.rootTable.setFillParent(true);
        this.menuStage.addActor(rootTable);
    }

    /**
     * Initializes the UI layout with the provided toolbars.
     *
     * @param toolbar     the top application toolbar
     * @param sideToolbar the side application toolbar
     */
    public void setupLayout(AppToolbar toolbar, AppSideToolbar sideToolbar) {
        rootTable.clear();
        rootTable.add(toolbar).expandX().fillX().colspan(2).row();
        rootTable.add(sideToolbar).expandY().fillY().left();
        rootTable.add().expand().fill();
    }

    /**
     * Creates the default UI skin.
     *
     * @return the created Skin object
     */
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
     *
     * @param camController    the camera controller to configure
     * @param initialGridSize the initial grid size from configuration
     * @param gridSizeCallback the callback to update the grid size
     * @param saveCallback     the callback to save configuration
     */
    public void toggleSettingsWindow(CameraInputController camController, float initialGridSize, Consumer<Float> gridSizeCallback, Runnable saveCallback) {
        if (settingsWindow == null) {
            settingsWindow = new SettingsWindow(skin, camController, initialGridSize, gridSizeCallback, saveCallback);
            addDialog(settingsWindow);
        } else {
            settingsWindow.setVisible(!settingsWindow.isVisible());
        }
    }

    /**
     * Toggles the visibility of the model list window.
     *
     * @param modelPaths the current list of loaded model paths
     * @param listener   the listener for model list operations
     */
    public void toggleModelListWindow(Array<String> modelPaths, ModelListWindow.ModelListListener listener) {
        if (modelListWindow == null) {
            modelListWindow = new ModelListWindow(skin, modelPaths, listener);
            addDialog(modelListWindow);
        } else {
            modelListWindow.setVisible(!modelListWindow.isVisible());
        }
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
     * Returns the UI skin.
     *
     * @return the skin
     */
    public Skin getSkin() {
        return skin;
    }

    /**
     * Updates and renders both UI stages.
     *
     * @param delta the time in seconds since the last render
     */
    public void render(float delta) {
        menuStage.act(delta);
        menuStage.draw();
        
        dialogStage.act(delta);
        dialogStage.draw();
    }

    /**
     * Updates the viewports of both stages when the screen is resized.
     *
     * @param width  the new screen width
     * @param height the new screen height
     */
    public void resize(int width, int height) {
        menuStage.getViewport().update(width, height, true);
        dialogStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        menuStage.dispose();
        dialogStage.dispose();
        skin.dispose();
    }
}
