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
 * Side toolbar for the application, providing access to tool windows like the model list.
 * This toolbar is inspired by the IDE layout and is permanently located on the side.
 */
public class AppSideToolbar extends Table {

    /**
     * Interface for listening to side toolbar events.
     */
    public interface SideToolbarListener {
        /**
         * Called when the project structure/model list button is clicked.
         */
        void onToggleModelList();
    }

    private final Skin skin;
    private final SideToolbarListener listener;

    /**
     * Creates a new side toolbar with the specified skin and listener.
     *
     * @param skin     the skin to use for styling
     * @param listener the listener for side toolbar events
     */
    public AppSideToolbar(Skin skin, SideToolbarListener listener) {
        this.skin = skin;
        this.listener = listener;
        setupUI();
    }

    private void setupUI() {
        setBackground(skin.newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 0.8f)));
        top();

        ImageButton.ImageButtonStyle projectStyle = createButtonStyle(createProjectIcon());
        ImageButton projectButton = new ImageButton(projectStyle);
        projectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (listener != null) listener.onToggleModelList();
            }
        });

        add(projectButton).pad(5).row();
        add().expandY();
    }

    private ImageButton.ImageButtonStyle createButtonStyle(Drawable icon) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = skin.newDrawable("white", Color.GRAY);
        style.down = skin.newDrawable("white", Color.DARK_GRAY);
        style.imageUp = icon;
        return style;
    }

    /**
     * Creates a project structure icon and adds it to the skin.
     *
     * @return the created icon drawable
     */
    private Drawable createProjectIcon() {
        if (skin.has("projectIcon", Texture.class)) {
            return skin.getDrawable("projectIcon");
        }
        Pixmap pixmap = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.LIGHT_GRAY);
        pixmap.fill();
        pixmap.setColor(Color.DARK_GRAY);
        // Visual representation of project structure (folders/files)
        pixmap.fillRectangle(4, 4, 10, 8);
        pixmap.fillRectangle(18, 4, 10, 8);
        pixmap.fillRectangle(4, 16, 24, 12);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add("projectIcon", texture);
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}
