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

import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;

import java.nio.file.Paths;

/**
 * Window that displays a list of currently loaded models in the scene.
 * It shows only the file name and extension of each loaded file.
 * The window includes padding to ensure the list content is properly aligned
 * and does not overlap with the window title.
 */
public class ModelListWindow extends Window {

    private final List<String> list;
    private final Array<String> modelPaths;

    /**
     * Creates a new model list window.
     *
     * @param skin       the skin to use for styling
     * @param modelPaths reference to the array containing paths of loaded models
     */
    public ModelListWindow(Skin skin, Array<String> modelPaths) {
        super("Loaded Models", skin);
        this.modelPaths = modelPaths;

        setMovable(true);
        setResizable(true);
        setSize(250, 400);

        // Adjust padding to make room for title and have some margin around content
        padTop(30);
        padBottom(10);
        padLeft(10);
        padRight(10);

        list = new List<>(skin);
        updateList();

        ScrollPane scrollPane = new ScrollPane(list, skin);
        add(scrollPane).expand().fill();
    }

    /**
     * Updates the displayed list of models from the application state.
     * Extracts only the filename and extension for display.
     */
    public void updateList() {
        Array<String> displayNames = new Array<>();
        for (String path : modelPaths) {
            displayNames.add(Paths.get(path).getFileName().toString());
        }
        list.setItems(displayNames);
    }
}
