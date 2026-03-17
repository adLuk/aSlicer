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
package cz.ad.print3d.aslicer.logic.model.format.gcode.flavor;

import cz.ad.print3d.aslicer.logic.model.format.gcode.core.*;

/**
 * Griffin flavor-specific G-code DTO.
 * This class stores G-code and metadata specifically for the Griffin firmware (used by Ultimaker).
 * Griffin G-code is used by Ultimaker printers and follows standard ISO 6983 conventions
 * with specific headers and comments for printer management.
 */
public class GriffinGCode extends GCode {

    /**
     * Initializes a new Griffin G-code object.
     */
    public GriffinGCode() {
        super(GCodeFlavor.GRIFFIN);
    }
}
