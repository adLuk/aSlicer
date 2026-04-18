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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a green wired grid plane at Z=0.
 * The grid expands on X and Y axes with a configurable step size.
 */
public class AppGrid implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppGrid.class);
    /**
     * The underlying LibGDX model for the grid lines.
     */
    private final Model model;

    /**
     * The model instance used for rendering the grid in the 3D scene.
     */
    private final ModelInstance instance;

    /**
     * The distance between individual grid lines.
     */
    private final float step;

    /**
     * Creates a new AppGrid with the specified step size.
     *
     * @param step the distance between grid lines
     */
    public AppGrid(float step) {
        this.step = step;
        LOGGER.info("Creating grid with step size: {}", step);
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("grid", GL20.GL_LINES, VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorUnpacked, new Material());
        builder.setColor(Color.GREEN);

        // Grid range: -500 to 500 on X and Y (Total 1000 units)
        float range = 1000f;
        float halfRange = range / 2f;
        
        // Use a small epsilon to avoid floating point issues at the end of the loop
        float epsilon = step / 10f;

        LOGGER.debug("Drawing grid lines from -{} to {} with step {}", halfRange, halfRange, step);
        for (float i = -halfRange; i <= halfRange + epsilon; i += step) {
            builder.line(i, -halfRange, 0, i, halfRange, 0);
            builder.line(-halfRange, i, 0, halfRange, i, 0);
        }

        model = modelBuilder.end();
        instance = new ModelInstance(model);
    }

    /**
     * Returns the ModelInstance representing the grid.
     *
     * @return the grid instance
     */
    public ModelInstance getInstance() {
        return instance;
    }

    /**
     * Returns the step size of the grid.
     *
     * @return the distance between grid lines
     */
    public float getStep() {
        return step;
    }

    @Override
    public void dispose() {
        if (model != null) {
            LOGGER.debug("Disposing AppGrid model");
            model.dispose();
        }
    }
}
