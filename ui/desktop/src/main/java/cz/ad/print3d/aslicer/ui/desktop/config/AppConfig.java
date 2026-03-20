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
package cz.ad.print3d.aslicer.ui.desktop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Handles application configuration in a property file.
 */
public class AppConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    public static Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), ".aslicer", "aslicer.properties");

    private final Properties props;

    public AppConfig() {
        this.props = loadConfig();
    }

    private Properties loadConfig() {
        Properties properties = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) {
                properties.load(is);
            } catch (IOException e) {
                LOGGER.debug("Could not load config: {}", e.getMessage());
            }
        }
        return properties;
    }

    /**
     * Saves the current properties to the configuration file.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream os = Files.newOutputStream(CONFIG_PATH)) {
                props.store(os, "aSlicer Desktop Settings");
            }
        } catch (IOException e) {
            LOGGER.error("Could not save config: {}", e.getMessage());
        }
    }

    /**
     * Gets a property value by key.
     *
     * @param key the property key
     * @return the property value, or null if not found
     */
    public String getProperty(String key) {
        return props.getProperty(key);
    }

    /**
     * Gets a property value by key with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return the property value, or defaultValue if not found
     */
    public String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Gets an integer property value by key with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return the integer property value, or defaultValue if not found or not a valid integer
     */
    public int getInt(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a float property value by key with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value
     * @return the float property value, or defaultValue if not found or not a valid float
     */
    public float getFloat(String key, float defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets a property value.
     *
     * @param key   the property key
     * @param value the property value. If null, the key is removed.
     */
    public void setProperty(String key, String value) {
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
    }

    /**
     * Sets an integer property value.
     *
     * @param key   the property key
     * @param value the integer value
     */
    public void setProperty(String key, int value) {
        props.setProperty(key, String.valueOf(value));
    }

    /**
     * Sets a float property value.
     *
     * @param key   the property key
     * @param value the float value
     */
    public void setProperty(String key, float value) {
        props.setProperty(key, String.valueOf(value));
    }

    /**
     * Checks if a property key exists.
     *
     * @param key the property key
     * @return true if the key exists, false otherwise
     */
    public boolean containsKey(String key) {
        return props.containsKey(key);
    }

    /**
     * Removes a property by key.
     *
     * @param key the property key
     */
    public void remove(String key) {
        props.remove(key);
    }

    /**
     * Loads configuration into a DTO.
     *
     * @return a populated AppConfigDto instance
     */
    public AppConfigDto loadToDto() {
        AppConfigDto dto = new AppConfigDto();
        dto.setWindowWidth(getInt("window.width", 1280));
        dto.setWindowHeight(getInt("window.height", 720));
        dto.setLastDir(getProperty("last.dir", ""));
        dto.setLastFile(getProperty("last.file", ""));
        dto.setDistance(getFloat("model.distance", 0.5f));
        dto.setGridSize(getFloat("grid.size", 5.0f));

        int loadedFileCount = getInt("loaded.file.count", 0);
        List<String> loadedFiles = new ArrayList<>();
        for (int i = 0; i < loadedFileCount; i++) {
            String path = getProperty("loaded.file." + i);
            if (path != null) {
                loadedFiles.add(path);
            }
        }
        dto.setLoadedFiles(loadedFiles);

        dto.setRotateButton(getInt("control.rotateButton", 0));
        dto.setTranslateButton(getInt("control.translateButton", 1));
        dto.setForwardButton(getInt("control.forwardButton", 2));
        dto.setForwardKey(getInt("control.forwardKey", 51));
        dto.setBackwardKey(getInt("control.backwardKey", 47));

        if (containsKey("camera.pos.x")) {
            dto.setCameraPosX(getFloat("camera.pos.x", 10.0f));
            dto.setCameraPosY(getFloat("camera.pos.y", 10.0f));
            dto.setCameraPosZ(getFloat("camera.pos.z", 10.0f));

            dto.setCameraDirX(getFloat("camera.dir.x", 0.0f));
            dto.setCameraDirY(getFloat("camera.dir.y", 0.0f));
            dto.setCameraDirZ(getFloat("camera.dir.z", -1.0f));

            dto.setCameraUpX(getFloat("camera.up.x", 0.0f));
            dto.setCameraUpY(getFloat("camera.up.y", 1.0f));
            dto.setCameraUpZ(getFloat("camera.up.z", 0.0f));

            dto.setCameraStateLoaded(true);
        }

        if (containsKey("camera.target.x")) {
            dto.setCameraTargetX(getFloat("camera.target.x", 0.0f));
            dto.setCameraTargetY(getFloat("camera.target.y", 0.0f));
            dto.setCameraTargetZ(getFloat("camera.target.z", 0.0f));
            dto.setCameraTargetLoaded(true);
        }

        return dto;
    }

    /**
     * Saves configuration from a DTO.
     *
     * @param dto the DTO instance to save
     */
    public void saveFromDto(AppConfigDto dto) {
        if (dto == null) return;

        setProperty("window.width", dto.getWindowWidth());
        setProperty("window.height", dto.getWindowHeight());
        setProperty("last.dir", dto.getLastDir());
        setProperty("last.file", dto.getLastFile());
        setProperty("model.distance", dto.getDistance());
        setProperty("grid.size", dto.getGridSize());

        List<String> loadedFiles = dto.getLoadedFiles();
        setProperty("loaded.file.count", loadedFiles.size());
        for (int i = 0; i < loadedFiles.size(); i++) {
            props.setProperty("loaded.file." + i, loadedFiles.get(i));
        }

        setProperty("control.rotateButton", dto.getRotateButton());
        setProperty("control.translateButton", dto.getTranslateButton());
        setProperty("control.forwardButton", dto.getForwardButton());
        setProperty("control.forwardKey", dto.getForwardKey());
        setProperty("control.backwardKey", dto.getBackwardKey());

        setProperty("camera.pos.x", dto.getCameraPosX());
        setProperty("camera.pos.y", dto.getCameraPosY());
        setProperty("camera.pos.z", dto.getCameraPosZ());

        setProperty("camera.dir.x", dto.getCameraDirX());
        setProperty("camera.dir.y", dto.getCameraDirY());
        setProperty("camera.dir.z", dto.getCameraDirZ());

        setProperty("camera.up.x", dto.getCameraUpX());
        setProperty("camera.up.y", dto.getCameraUpY());
        setProperty("camera.up.z", dto.getCameraUpZ());

        setProperty("camera.target.x", dto.getCameraTargetX());
        setProperty("camera.target.y", dto.getCameraTargetY());
        setProperty("camera.target.z", dto.getCameraTargetZ());

        save();
    }
}
