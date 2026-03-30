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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Toolbar for the application, providing access to main actions like clearing the model,
 * opening a new model, and accessing settings.
 */
public final class AppToolbar extends Table {

    /**
     * Interface for listening to toolbar events.
     */
    public interface ToolbarListener {
        /**
         * Called when the clear model button is clicked.
         */
        void onClear();

        /**
         * Called when the open model button is clicked.
         */
        void onOpen();

        /**
         * Called when the settings button is clicked.
         */
        void onSettings();

        /**
         * Called when the view switch button is clicked.
         *
         * @param index the index of the view to switch to (0 for Model, 1 for Grid)
         */
        void onSwitchView(int index);
    }

    private final Skin skin;
    private final ToolbarListener listener;

    /**
     * Creates a new toolbar with the specified skin and listener.
     *
     * @param skin     the skin to use for styling
     * @param listener the listener for toolbar events
     */
    public AppToolbar(Skin skin, ToolbarListener listener) {
        this.skin = skin;
        this.listener = listener;
        setupUI();
    }

    /**
     * Sets up the UI components of the toolbar, including buttons and their listeners.
     */
    private void setupUI() {
        setBackground(skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 0.7f)));

        ImageButton.ImageButtonStyle clearStyle = createButtonStyle(createClearIcon());
        ImageButton clearButton = new ImageButton(clearStyle);
        clearButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onClear();
            }
        });

        ImageButton.ImageButtonStyle openStyle = createButtonStyle(createOpenIcon());
        ImageButton openButton = new ImageButton(openStyle);
        openButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onOpen();
            }
        });

        ImageButton.ImageButtonStyle settingsStyle = createButtonStyle(SettingsWindow.createSettingsIcon(skin));
        ImageButton settingsButton = new ImageButton(settingsStyle);
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onSettings();
            }
        });

        ImageButton.ImageButtonStyle modelViewStyle = createButtonStyle(createModelIcon());
        ImageButton modelViewButton = new ImageButton(modelViewStyle);
        modelViewButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onSwitchView(0);
            }
        });

        ImageButton.ImageButtonStyle gridViewStyle = createButtonStyle(createGridIcon());
        ImageButton gridViewButton = new ImageButton(gridViewStyle);
        gridViewButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onSwitchView(1);
            }
        });

        add(clearButton).pad(5);
        add(openButton).pad(5);
        add(modelViewButton).pad(5);
        add(gridViewButton).pad(5);
        add().expandX();
        add(settingsButton).pad(5);
        left();
    }

    /**
     * Creates a style for an image button with the specified icon.
     *
     * @param icon the icon to use for the button
     * @return the created ImageButtonStyle
     */
    private ImageButton.ImageButtonStyle createButtonStyle(Drawable icon) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = skin.newDrawable("white", Color.GRAY);
        style.down = skin.newDrawable("white", Color.DARK_GRAY);
        style.imageUp = icon;
        return style;
    }

    /**
     * Creates the icon for the clear model action.
     *
     * @return the clear icon drawable
     */
    private Drawable createClearIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.LIGHT_GRAY);
        // Clear icon represented as a '-' sign to match the '+' and gear style
        pixmap.fillRectangle(4, 14, 24, 4);
        pixmap.setColor(Color.BLACK);
        pixmap.fillCircle(16, 16, 4);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("clearIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Creates the icon for the open model action.
     *
     * @return the open icon drawable
     */
    private Drawable createOpenIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.LIGHT_GRAY);
        // Open icon represented as a '+' sign to match the gear wheel style
        pixmap.fillRectangle(14, 4, 4, 24); // vertical
        pixmap.fillRectangle(4, 14, 24, 4); // horizontal
        pixmap.setColor(Color.BLACK);
        pixmap.fillCircle(16, 16, 4);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("openIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Creates the icon for the model view switch action.
     *
     * @return the model icon drawable
     */
    private Drawable createModelIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.CYAN);
        pixmap.fillRectangle(4, 4, 24, 24);
        pixmap.setColor(Color.BLACK);
        pixmap.drawRectangle(4, 4, 24, 24);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("modelIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    /**
     * Creates the icon for the grid view switch action.
     *
     * @return the grid icon drawable
     */
    private Drawable createGridIcon() {
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 32; i += 8) {
            pixmap.drawLine(i, 0, i, 31);
            pixmap.drawLine(0, i, 31, i);
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("gridIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}
