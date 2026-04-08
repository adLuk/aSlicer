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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AppConfigDto and its binding in AppConfig.
 */
public class AppConfigDtoTest {
    @TempDir
    Path tempDir;

    private Path originalConfigPath;

    @BeforeEach
    void setUp() {
        originalConfigPath = AppConfig.CONFIG_PATH;
        AppConfig.CONFIG_PATH = tempDir.resolve(".aslicer").resolve("aslicer-test.properties");
    }

    @AfterEach
    void tearDown() {
        AppConfig.CONFIG_PATH = originalConfigPath;
    }

    @Test
    void testDtoDefaults() {
        AppConfigDto dto = new AppConfigDto();
        assertEquals(800, dto.getWindowWidth());
        assertEquals(600, dto.getWindowHeight());
        assertEquals(800, dto.getWizardWidth());
        assertEquals(600, dto.getWizardHeight());
        assertEquals("", dto.getLastDir());
        assertEquals("", dto.getLastFile());
        assertEquals(0.5f, dto.getDistance());
        assertEquals(5.0f, dto.getGridSize());
        assertNotNull(dto.getLoadedFiles());
        assertTrue(dto.getLoadedFiles().isEmpty());
        assertFalse(dto.isCameraStateLoaded());
        assertFalse(dto.isCameraTargetLoaded());
    }

    @Test
    void testBindingSaveAndLoad() {
        AppConfig config = new AppConfig();
        AppConfigDto dto = new AppConfigDto();
        
        dto.setWindowWidth(1024);
        dto.setWindowHeight(768);
        dto.setWizardWidth(900);
        dto.setWizardHeight(700);
        dto.setLastDir("/test/dir");
        dto.setLastFile("/test/file.stl");
        dto.setCameraPosX(1.0f);
        dto.setCameraPosY(2.0f);
        dto.setCameraPosZ(3.0f);
        dto.setCameraDirX(0.0f);
        dto.setCameraDirY(0.0f);
        dto.setCameraDirZ(-1.0f);
        dto.setCameraUpX(0.0f);
        dto.setCameraUpY(1.0f);
        dto.setCameraUpZ(0.0f);
        dto.setCameraTargetX(5.0f);
        dto.setCameraTargetY(6.0f);
        dto.setCameraTargetZ(7.0f);
        dto.setDistance(1.5f);
        dto.setGridSize(2.0f);
        dto.setLoadedFiles(Arrays.asList("file1.stl", "file2.stl", "file1.stl"));

        // Save DTO to properties
        config.saveFromDto(dto);

        // Load DTO from properties
        AppConfig config2 = new AppConfig();
        AppConfigDto loadedDto = config2.loadToDto();

        assertEquals(1024, loadedDto.getWindowWidth());
        assertEquals(768, loadedDto.getWindowHeight());
        assertEquals(900, loadedDto.getWizardWidth());
        assertEquals(700, loadedDto.getWizardHeight());
        assertEquals("/test/dir", loadedDto.getLastDir());
        assertEquals("/test/file.stl", loadedDto.getLastFile());
        assertEquals(1.0f, loadedDto.getCameraPosX());
        assertEquals(2.0f, loadedDto.getCameraPosY());
        assertEquals(3.0f, loadedDto.getCameraPosZ());
        assertEquals(5.0f, loadedDto.getCameraTargetX());
        assertEquals(6.0f, loadedDto.getCameraTargetY());
        assertEquals(7.0f, loadedDto.getCameraTargetZ());
        assertEquals(1.5f, loadedDto.getDistance());
        assertEquals(2.0f, loadedDto.getGridSize());
        
        List<String> files = loadedDto.getLoadedFiles();
        assertEquals(3, files.size());
        assertEquals("file1.stl", files.get(0));
        assertEquals("file2.stl", files.get(1));
        assertEquals("file1.stl", files.get(2));

        assertTrue(loadedDto.isCameraStateLoaded());
        assertTrue(loadedDto.isCameraTargetLoaded());
    }

    @Test
    void testCameraStateNotLoadedWhenMissing() {
        AppConfig config = new AppConfig();
        // Just save something else
        config.setProperty("window.width", 800);
        config.save();

        AppConfig config2 = new AppConfig();
        AppConfigDto loadedDto = config2.loadToDto();
        assertFalse(loadedDto.isCameraStateLoaded());
        assertFalse(loadedDto.isCameraTargetLoaded());
    }
}
