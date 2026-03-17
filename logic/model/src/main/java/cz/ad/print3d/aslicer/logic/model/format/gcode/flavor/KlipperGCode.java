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
 * Klipper flavor-specific G-code DTO.
 * This class stores G-code and metadata specifically for the Klipper firmware.
 * Klipper allows for complex macros and can be controlled via extended G-codes.
 */
public class KlipperGCode extends GCode {

    /**
     * Initializes a new Klipper G-code object.
     */
    public KlipperGCode() {
        super(GCodeFlavor.KLIPPER);
    }
}
