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
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.utils.Disposable;
import cz.ad.print3d.aslicer.ui.desktop.config.AppConfigDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SceneManager handles the 3D scene components including the camera, lighting, 
 * rendering batch, and the background grid.
 */
public class SceneManager implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SceneManager.class);

    private final PerspectiveCamera cam;
    private final CameraInputController camController;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private AppGrid appGrid;

    /**
     * Creates a new SceneManager and initializes its components based on the provided configuration.
     *
     * @param dto the application configuration DTO
     */
    public SceneManager(AppConfigDto dto) {
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        modelBatch = new ModelBatch();

        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (dto.isCameraStateLoaded()) {
            cam.position.set(dto.getCameraPosX(), dto.getCameraPosY(), dto.getCameraPosZ());
            cam.direction.set(dto.getCameraDirX(), dto.getCameraDirY(), dto.getCameraDirZ());
            cam.up.set(dto.getCameraUpX(), dto.getCameraUpY(), dto.getCameraUpZ());
        } else {
            cam.position.set(10f, 10f, 10f);
            cam.lookAt(0, 0, 0);
        }
        cam.near = 1f;
        cam.far = 1500f;
        cam.update();

        camController = new CameraInputController(cam);
        if (dto.isCameraTargetLoaded()) {
            camController.target.set(dto.getCameraTargetX(), dto.getCameraTargetY(), dto.getCameraTargetZ());
        }
        camController.rotateButton = dto.getRotateButton();
        camController.translateButton = dto.getTranslateButton();
        camController.forwardButton = dto.getForwardButton();
        camController.forwardKey = dto.getForwardKey();
        camController.backwardKey = dto.getBackwardKey();

        appGrid = new AppGrid(dto.getGridSize());
    }

    /**
     * Updates the grid with a new step size.
     *
     * @param newStep the new distance between grid lines
     */
    public void updateGrid(float newStep) {
        if (appGrid != null) {
            if (Math.abs(appGrid.getStep() - newStep) < 0.0001f) return;
            appGrid.dispose();
        }
        appGrid = new AppGrid(newStep);
        LOGGER.info("Grid updated to step size: {}", newStep);
    }

    /**
     * Updates the camera viewport when the screen is resized.
     *
     * @param width  the new screen width
     * @param height the new screen height
     */
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    /**
     * Returns the perspective camera.
     *
     * @return the camera
     */
    public PerspectiveCamera getCamera() {
        return cam;
    }

    /**
     * Returns the camera input controller.
     *
     * @return the camera controller
     */
    public CameraInputController getCameraController() {
        return camController;
    }

    /**
     * Returns the model batch.
     *
     * @return the model batch
     */
    public ModelBatch getModelBatch() {
        return modelBatch;
    }

    /**
     * Returns the 3D environment.
     *
     * @return the environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Returns the background grid.
     *
     * @return the grid
     */
    public AppGrid getAppGrid() {
        return appGrid;
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        if (appGrid != null) {
            appGrid.dispose();
        }
    }
}
