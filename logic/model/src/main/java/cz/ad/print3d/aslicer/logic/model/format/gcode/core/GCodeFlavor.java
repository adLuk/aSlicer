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
package cz.ad.print3d.aslicer.logic.model.format.gcode.core;

/**
 * Enumeration of supported G-code flavors.
 * Different manufacturers and firmware implementations (flavors) may extend or modify
 * the standard ISO 6983 G-code blocks.
 */
public enum GCodeFlavor {
    /**
     * Marlin G-code flavor. Marlin is a popular open-source firmware for 3D printers.
     */
    MARLIN("Marlin"),

    /**
     * RepRap G-code flavor. The original open-source 3D printer project.
     */
    REPRAP("RepRap"),

    /**
     * Klipper G-code flavor. Klipper uses a host computer to process G-code and control the printer.
     */
    KLIPPER("Klipper"),

    /**
     * Prusa G-code flavor (fork of Marlin). Specific to Prusa Research printers.
     */
    PRUSA("Prusa"),

    /**
     * Bambu G-code flavor. Used by Bambu Lab printers.
     */
    BAMBU("Bambu"),

    /**
     * Griffin G-code flavor. Used by Ultimaker printers.
     */
    GRIFFIN("Griffin");

    private final String name;

    GCodeFlavor(String name) {
        this.name = name;
    }

    /**
     * Gets the human-readable name of the flavor.
     *
     * @return the flavor name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
