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
import cz.ad.print3d.aslicer.ui.desktop.DesktopApp;
import cz.ad.print3d.aslicer.ui.desktop.config.*;
import cz.ad.print3d.aslicer.ui.desktop.persistence.*;
import cz.ad.print3d.aslicer.ui.desktop.view.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.PointerBuffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the ModelFileDialog class.
 * Uses a subclass to mock the native dialog interaction.
 */
public class ModelFileDialogTest {

    @TempDir
    Path tempDir;

    @Test
    void testExtractDirectory() throws IOException {
        ModelFileDialog dialog = new ModelFileDialog();
        
        Path file = tempDir.resolve("test.stl");
        Files.createFile(file);
        
        String extracted = dialog.extractDirectory(file.toString());
        assertEquals(tempDir.toAbsolutePath().toString(), extracted);
        
        // Negative cases
        assertNull(dialog.extractDirectory(null));
        assertNull(dialog.extractDirectory(""));
        assertNull(dialog.extractDirectory(tempDir.resolve("non-existent.stl").toString()));
    }

    @Test
    void testShowOpenDialogSelection() {
        final String expectedFile = "/path/to/model.stl";
        ModelFileDialog dialog = new ModelFileDialog() {
            @Override
            protected String openNativeDialog(String title, String defaultPath, PointerBuffer filters, String description) {
                return expectedFile;
            }
        };

        String[] result = dialog.showOpenDialog("/some/dir");
        assertEquals(1, result.length);
        assertEquals(expectedFile, result[0]);
    }

    @Test
    void testShowOpenDialogMultipleSelection() {
        final String expectedFiles = "/path/to/model1.stl|/path/to/model2.stl";
        ModelFileDialog dialog = new ModelFileDialog() {
            @Override
            protected String openNativeDialog(String title, String defaultPath, PointerBuffer filters, String description) {
                return expectedFiles;
            }
        };

        String[] result = dialog.showOpenDialog("/some/dir");
        assertEquals(2, result.length);
        assertEquals("/path/to/model1.stl", result[0]);
        assertEquals("/path/to/model2.stl", result[1]);
    }

    @Test
    void testShowOpenDialogCancellation() {
        ModelFileDialog dialog = new ModelFileDialog() {
            @Override
            protected String openNativeDialog(String title, String defaultPath, PointerBuffer filters, String description) {
                return null;
            }
        };

        String[] result = dialog.showOpenDialog("/some/dir");
        assertNull(result);
    }
}
