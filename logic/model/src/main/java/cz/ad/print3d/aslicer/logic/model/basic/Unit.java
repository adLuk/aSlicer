package cz.ad.print3d.aslicer.logic.model.basic;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of supported measurement units for 3MF (3D Manufacturing Format).
 * This enum defines the units used for coordinate values in the 3D model.
 */
public enum Unit {
    /**
     * Microns (1 micrometer).
     */
    MICRON("micron"),
    
    /**
     * Millimeters. This is the default unit for 3MF files if not specified.
     */
    MILLIMETER("millimeter"),
    
    /**
     * Centimeters.
     */
    CENTIMETER("centimeter"),
    
    /**
     * Inches.
     */
    INCH("inch"),
    
    /**
     * Feet.
     */
    FOOT("foot"),
    
    /**
     * Meters.
     */
    METER("meter");

    private final String value;
    private static final Map<String, Unit> LOOKUP = new HashMap<>();

    static {
        for (Unit unit : Unit.values()) {
            LOOKUP.put(unit.value.toLowerCase(), unit);
        }
    }

    /**
     * Constructs a Unit with its corresponding string value.
     *
     * @param value the string value used in 3MF files
     */
    Unit(String value) {
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
    public static Unit fromString(String value) {
        if (value == null) {
            return null;
        }
        return LOOKUP.get(value.toLowerCase());
    }
}
