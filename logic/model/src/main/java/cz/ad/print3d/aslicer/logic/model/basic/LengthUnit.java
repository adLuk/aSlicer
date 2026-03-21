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
package cz.ad.print3d.aslicer.logic.model.basic;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of supported measurement units for 3MF (3D Manufacturing Format).
 * This enum defines the units used for coordinate values in the 3D model.
 */
@XmlType(name = "unit", namespace = "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel")
@XmlEnum
public enum LengthUnit {
    /**
     * Microns (1 micrometer).
     */
    @XmlEnumValue("micron")
    MICRON("micron"),
    
    /**
     * Millimeters. This is the default unit for 3MF files if not specified.
     */
    @XmlEnumValue("millimeter")
    MILLIMETER("millimeter"),
    
    /**
     * Centimeters.
     */
    @XmlEnumValue("centimeter")
    CENTIMETER("centimeter"),
    
    /**
     * Inches.
     */
    @XmlEnumValue("inch")
    INCH("inch"),
    
    /**
     * Feet.
     */
    @XmlEnumValue("foot")
    FOOT("foot"),
    
    /**
     * Meters.
     */
    @XmlEnumValue("meter")
    METER("meter");

    private final String value;
    private static final Map<String, LengthUnit> LOOKUP = new HashMap<>();

    static {
        for (LengthUnit lengthUnit : LengthUnit.values()) {
            LOOKUP.put(lengthUnit.value.toLowerCase(), lengthUnit);
        }
    }

    /**
     * Constructs a Unit with its corresponding string value.
     *
     * @param value the string value used in 3MF files
     */
    LengthUnit(String value) {
        this.value = value;
    }

    /**
     * Returns the string representation of the unit as used in 3MF files.
     *
     * @return the string value
     */
    public String getValue() {
        return value;
    }

    /**
     * Finds the Unit corresponding to the given string value.
     *
     * @param value the string value to lookup
     * @return the Unit, or null if no match is found
     */
    public static LengthUnit fromString(String value) {
        if (value == null) {
            return null;
        }
        return LOOKUP.get(value.toLowerCase());
    }
}
