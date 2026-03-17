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
package cz.ad.print3d.aslicer.logic.model.parser;

import cz.ad.print3d.aslicer.logic.model.Model;
import cz.ad.print3d.aslicer.logic.model.parser.mf3.Mf3Parser;
import cz.ad.print3d.aslicer.logic.model.parser.stl.StlParser;
import cz.ad.print3d.aslicer.logic.model.parser.gcode.GCodeModelParser;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ModelParserFactory {
    public static Model parse(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            if (fileName.endsWith(".stl")) {
                return new StlParser().parse(channel);
            } else if (fileName.endsWith(".3mf")) {
                return new Mf3Parser().parse(channel);
            } else if (fileName.endsWith(".gcode")) {
                return new GCodeModelParser().parse(channel);
            }
        }
        throw new IOException("Unsupported file format: " + fileName);
    }
}
