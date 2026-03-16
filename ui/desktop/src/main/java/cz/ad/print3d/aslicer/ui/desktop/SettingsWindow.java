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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Settings window for configuring application controls and other options.
 */
public class SettingsWindow extends Window {

    private final CameraInputController camController;
    private final Runnable saveCallback;

    public SettingsWindow(Skin skin, CameraInputController camController, Runnable saveCallback) {
        super("Settings", skin);
        this.camController = camController;
        this.saveCallback = saveCallback;

        setMovable(true);
        setResizable(true);
        setSize(300, 250);
        setPosition(Gdx.graphics.getWidth() / 2f - 150, Gdx.graphics.getHeight() / 2f - 125);

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
                if (saveCallback != null) {
                    saveCallback.run();
                }
                setVisible(false);
            }
        });

        content.add(saveButton).colspan(2).padTop(20);

        add(content).expand().fill();
    }

    /**
     * Creates a settings icon drawable and adds it to the skin.
     *
     * @param skin the skin to add the icon texture to
     * @return the created icon drawable
     */
    public static Drawable createSettingsIcon(Skin skin) {
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
}
