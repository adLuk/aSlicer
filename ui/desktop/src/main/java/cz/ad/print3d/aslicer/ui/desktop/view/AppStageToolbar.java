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
 * Stage toolbar for the application, providing access to scene stage switching actions.
 * This toolbar is located under the main toolbar and is aligned with other toolbars.
 */
public final class AppStageToolbar extends Table {

    /**
     * Interface for listening to stage toolbar events.
     */
    public interface StageToolbarListener {
        /**
         * Called when the stage switch button is clicked.
         *
         * @param index the index of the stage to switch to (0 for Model, 1 for Grid)
         */
        void onSwitchStage(int index);
    }

    private final Skin skin;
    private final StageToolbarListener listener;

    /**
     * Creates a new stage toolbar with the specified skin and listener.
     *
     * @param skin     the skin to use for styling
     * @param listener the listener for stage toolbar events
     */
    public AppStageToolbar(Skin skin, StageToolbarListener listener) {
        this.skin = skin;
        this.listener = listener;
        setupUI();
    }

    /**
     * Sets up the UI components of the stage toolbar, including buttons and their listeners.
     */
    private void setupUI() {
        setBackground(skin.newDrawable("white", new Color(0.12f, 0.12f, 0.12f, 0.75f)));

        ImageButton.ImageButtonStyle modelViewStyle = createButtonStyle(createModelIcon());
        ImageButton modelViewButton = new ImageButton(modelViewStyle);
        modelViewButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onSwitchStage(0);
            }
        });

        ImageButton.ImageButtonStyle gridViewStyle = createButtonStyle(createGridIcon());
        ImageButton gridViewButton = new ImageButton(gridViewStyle);
        gridViewButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onSwitchStage(1);
            }
        });

        add(modelViewButton).pad(5);
        add(gridViewButton).pad(5);
        add().expandX();
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
