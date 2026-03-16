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

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for application configuration settings.
 * Holds all persistent settings for the desktop application.
 */
public class AppConfigDto {
    // Window settings
    private int windowWidth = 800;
    private int windowHeight = 600;

    // Last used files/dirs
    private String lastDir = "";
    private String lastFile = "";

    // Control settings
    private int rotateButton = 0; // com.badlogic.gdx.Input.Buttons.LEFT
    private int translateButton = 1; // com.badlogic.gdx.Input.Buttons.RIGHT
    private int forwardButton = 2; // com.badlogic.gdx.Input.Buttons.MIDDLE
    private int forwardKey = 51; // com.badlogic.gdx.Input.Keys.W
    private int backwardKey = 47; // com.badlogic.gdx.Input.Keys.S

    // Camera position
    private float cameraPosX = 10.0f;
    private float cameraPosY = 10.0f;
    private float cameraPosZ = 10.0f;

    // Camera direction
    private float cameraDirX = 0.0f;
    private float cameraDirY = 0.0f;
    private float cameraDirZ = -1.0f;

    // Camera up vector
    private float cameraUpX = 0.0f;
    private float cameraUpY = 1.0f;
    private float cameraUpZ = 0.0f;

    // Camera target
    private float cameraTargetX = 0.0f;
    private float cameraTargetY = 0.0f;
    private float cameraTargetZ = 0.0f;

    // Flags to check if camera state was loaded
    private boolean cameraStateLoaded = false;
    private boolean cameraTargetLoaded = false;

    // Distance between objects
    private float distance = 0.5f;

    // Grid settings
    private float gridSize = 5.0f;

    // List of loaded model paths in order they were added
    private List<String> loadedFiles = new ArrayList<>();

    /**
     * @return the width of the application window
     */
    public int getWindowWidth() {
        return windowWidth;
    }

    /**
     * @param windowWidth the width of the application window to set
     */
    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    /**
     * @return the height of the application window
     */
    public int getWindowHeight() {
        return windowHeight;
    }

    /**
     * @param windowHeight the height of the application window to set
     */
    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    /**
     * @return the last directory path used in file dialogs
     */
    public String getLastDir() {
        return lastDir;
    }

    /**
     * @param lastDir the last directory path to set
     */
    public void setLastDir(String lastDir) {
        this.lastDir = lastDir;
    }

    /**
     * @return the last 3D model file path opened
     */
    public String getLastFile() {
        return lastFile;
    }

    /**
     * @param lastFile the last model file path to set
     */
    public void setLastFile(String lastFile) {
        this.lastFile = lastFile;
    }

    /**
     * @return the GDX button constant for camera rotation
     */
    public int getRotateButton() {
        return rotateButton;
    }

    /**
     * @param rotateButton the GDX button constant for camera rotation to set
     */
    public void setRotateButton(int rotateButton) {
        this.rotateButton = rotateButton;
    }

    /**
     * @return the GDX button constant for camera translation
     */
    public int getTranslateButton() {
        return translateButton;
    }

    /**
     * @param translateButton the GDX button constant for camera translation to set
     */
    public void setTranslateButton(int translateButton) {
        this.translateButton = translateButton;
    }

    /**
     * @return the GDX button constant for camera forward movement
     */
    public int getForwardButton() {
        return forwardButton;
    }

    /**
     * @param forwardButton the GDX button constant for camera forward movement to set
     */
    public void setForwardButton(int forwardButton) {
        this.forwardButton = forwardButton;
    }

    /**
     * @return the GDX key constant for moving forward
     */
    public int getForwardKey() {
        return forwardKey;
    }

    /**
     * @param forwardKey the GDX key constant for moving forward to set
     */
    public void setForwardKey(int forwardKey) {
        this.forwardKey = forwardKey;
    }

    /**
     * @return the GDX key constant for moving backward
     */
    public int getBackwardKey() {
        return backwardKey;
    }

    /**
     * @param backwardKey the GDX key constant for moving backward to set
     */
    public void setBackwardKey(int backwardKey) {
        this.backwardKey = backwardKey;
    }

    /**
     * @return X-coordinate of the camera position
     */
    public float getCameraPosX() {
        return cameraPosX;
    }

    /**
     * @param cameraPosX X-coordinate of the camera position to set
     */
    public void setCameraPosX(float cameraPosX) {
        this.cameraPosX = cameraPosX;
    }

    /**
     * @return Y-coordinate of the camera position
     */
    public float getCameraPosY() {
        return cameraPosY;
    }

    /**
     * @param cameraPosY Y-coordinate of the camera position to set
     */
    public void setCameraPosY(float cameraPosY) {
        this.cameraPosY = cameraPosY;
    }

    /**
     * @return Z-coordinate of the camera position
     */
    public float getCameraPosZ() {
        return cameraPosZ;
    }

    /**
     * @param cameraPosZ Z-coordinate of the camera position to set
     */
    public void setCameraPosZ(float cameraPosZ) {
        this.cameraPosZ = cameraPosZ;
    }

    /**
     * @return X-coordinate of the camera direction vector
     */
    public float getCameraDirX() {
        return cameraDirX;
    }

    /**
     * @param cameraDirX X-coordinate of the camera direction vector to set
     */
    public void setCameraDirX(float cameraDirX) {
        this.cameraDirX = cameraDirX;
    }

    /**
     * @return Y-coordinate of the camera direction vector
     */
    public float getCameraDirY() {
        return cameraDirY;
    }

    /**
     * @param cameraDirY Y-coordinate of the camera direction vector to set
     */
    public void setCameraDirY(float cameraDirY) {
        this.cameraDirY = cameraDirY;
    }

    /**
     * @return Z-coordinate of the camera direction vector
     */
    public float getCameraDirZ() {
        return cameraDirZ;
    }

    /**
     * @param cameraDirZ Z-coordinate of the camera direction vector to set
     */
    public void setCameraDirZ(float cameraDirZ) {
        this.cameraDirZ = cameraDirZ;
    }

    /**
     * @return X-coordinate of the camera up vector
     */
    public float getCameraUpX() {
        return cameraUpX;
    }

    /**
     * @param cameraUpX X-coordinate of the camera up vector to set
     */
    public void setCameraUpX(float cameraUpX) {
        this.cameraUpX = cameraUpX;
    }

    /**
     * @return Y-coordinate of the camera up vector
     */
    public float getCameraUpY() {
        return cameraUpY;
    }

    /**
     * @param cameraUpY Y-coordinate of the camera up vector to set
     */
    public void setCameraUpY(float cameraUpY) {
        this.cameraUpY = cameraUpY;
    }

    /**
     * @return Z-coordinate of the camera up vector
     */
    public float getCameraUpZ() {
        return cameraUpZ;
    }

    /**
     * @param cameraUpZ Z-coordinate of the camera up vector to set
     */
    public void setCameraUpZ(float cameraUpZ) {
        this.cameraUpZ = cameraUpZ;
    }

    /**
     * @return X-coordinate of the camera target position
     */
    public float getCameraTargetX() {
        return cameraTargetX;
    }

    /**
     * @param cameraTargetX X-coordinate of the camera target position to set
     */
    public void setCameraTargetX(float cameraTargetX) {
        this.cameraTargetX = cameraTargetX;
    }

    /**
     * @return Y-coordinate of the camera target position
     */
    public float getCameraTargetY() {
        return cameraTargetY;
    }

    /**
     * @param cameraTargetY Y-coordinate of the camera target position to set
     */
    public void setCameraTargetY(float cameraTargetY) {
        this.cameraTargetY = cameraTargetY;
    }

    /**
     * @return Z-coordinate of the camera target position
     */
    public float getCameraTargetZ() {
        return cameraTargetZ;
    }

    /**
     * @param cameraTargetZ Z-coordinate of the camera target position to set
     */
    public void setCameraTargetZ(float cameraTargetZ) {
        this.cameraTargetZ = cameraTargetZ;
    }

    /**
     * @return true if camera position/direction/up state was successfully loaded from properties
     */
    public boolean isCameraStateLoaded() {
        return cameraStateLoaded;
    }

    /**
     * @param cameraStateLoaded true if camera state should be marked as loaded
     */
    public void setCameraStateLoaded(boolean cameraStateLoaded) {
        this.cameraStateLoaded = cameraStateLoaded;
    }

    /**
     * @return true if camera target position was successfully loaded from properties
     */
    public boolean isCameraTargetLoaded() {
        return cameraTargetLoaded;
    }

    /**
     * @param cameraTargetLoaded true if camera target should be marked as loaded
     */
    public void setCameraTargetLoaded(boolean cameraTargetLoaded) {
        this.cameraTargetLoaded = cameraTargetLoaded;
    }

    /**
     * @return the distance between objects when placing them automatically
     */
    public float getDistance() {
        return distance;
    }

    /**
     * @param distance the distance between objects to set
     */
    public void setDistance(float distance) {
        this.distance = distance;
    }

    /**
     * @return the size of the grid cells
     */
    public float getGridSize() {
        return gridSize;
    }

    /**
     * @param gridSize the grid size to set
     */
    public void setGridSize(float gridSize) {
        this.gridSize = gridSize;
    }

    /**
     * @return the list of loaded model paths in the order they were added
     */
    public List<String> getLoadedFiles() {
        return loadedFiles;
    }

    /**
     * @param loadedFiles the list of loaded model paths to set
     */
    public void setLoadedFiles(List<String> loadedFiles) {
        this.loadedFiles = loadedFiles != null ? loadedFiles : new ArrayList<>();
    }
}
