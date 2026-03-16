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

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles the native file open dialog for 3D model files.
 * This class encapsulates the TinyFileDialogs native calls and provides helper methods
 * for processing the selected file path.
 */
public class ModelFileDialog {

    /**
     * Shows a native open file dialog for 3D model files (*.stl, *.ast, *.3mf).
     *
     * @param lastDir the initial directory to show in the dialog
     * @return an array of absolute paths of the selected files, or null if canceled
     */
    public String[] showOpenDialog(String lastDir) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(3);
            filters.put(stack.UTF8("*.stl"));
            filters.put(stack.UTF8("*.ast"));
            filters.put(stack.UTF8("*.3mf"));
            filters.flip();

            String result = openNativeDialog("Open 3D Model Files", lastDir, filters, "3D Model Files");
            if (result == null) {
                return null;
            }
            return result.split("\\|");
        }
    }

    /**
     * Wrapper for the native file dialog call, allowing for easier unit testing.
     *
     * @param title       the dialog title
     * @param defaultPath the default path or directory
     * @param filters     the file filter patterns
     * @param description the file filter description
     * @return the selected file path(s), or null if canceled
     */
    protected String openNativeDialog(String title, String defaultPath, PointerBuffer filters, String description) {
        return TinyFileDialogs.tinyfd_openFileDialog(title, defaultPath, filters, description, true);
    }

    /**
     * Extracts the parent directory from a file path.
     *
     * @param filePath the path to the file
     * @return the absolute path of the parent directory, or null if it cannot be determined or path is null
     */
    public String extractDirectory(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Path parent = path.getParent();
            if (parent != null) {
                return parent.toAbsolutePath().toString();
            }
        }
        return null;
    }
}
