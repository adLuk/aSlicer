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

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import cz.ad.print3d.aslicer.ui.desktop.model.ModelManager;

/**
 * An Actor that renders the 3D scene within the GDX Stage.
 * <p>
 * This actor encapsulates the 3D rendering logic previously handled in the main application loop,
 * allowing the 3D scene to be managed as part of the UI hierarchy.
 * </p>
 */
public class SceneActor extends Actor {
    private final SceneManager sceneManager;
    private final ModelManager modelManager;

    /**
     * Creates a new SceneActor.
     *
     * @param sceneManager the manager for 3D scene components
     * @param modelManager the manager for loaded models
     */
    public SceneActor(SceneManager sceneManager, ModelManager modelManager) {
        this.sceneManager = sceneManager;
        this.modelManager = modelManager;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // We need to end the 2D batch before starting 3D rendering to avoid state conflicts
        batch.end();

        sceneManager.getCameraController().update();
        sceneManager.getModelBatch().begin(sceneManager.getCamera());
        sceneManager.getModelBatch().render(sceneManager.getAppGrid().getInstance());
        if (modelManager != null) {
            sceneManager.getModelBatch().render(modelManager.getInstances(), sceneManager.getEnvironment());
        }
        sceneManager.getModelBatch().end();

        // Resume the 2D batch for subsequent UI elements
        batch.begin();
    }
}
