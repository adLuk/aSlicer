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

import java.util.Objects;

/**
 * Represents a single G-code word consisting of an address character followed by a numerical value.
 * In accordance with ISO 6983, a word is the basic building block of a G-code block.
 * Each word begins with an 'address character' (like G, M, X, Y, Z, F, S, T) that defines its purpose.
 *
 * @param address the address character (e.g., 'G', 'M', 'X', 'Y', 'Z', 'F', 'S', 'T')
 * @param value   the numerical value associated with the address
 */
public record GCodeWord(char address, double value) {

    /**
     * Returns a string representation of the G-code word.
     * For integers, it omits the decimal part.
     *
     * @return the string representation of the word
     */
    @Override
    public String toString() {
        if (value == (long) value) {
            return String.format("%c%d", Character.toUpperCase(address), (long) value);
        } else {
            return String.format("%c%s", Character.toUpperCase(address), Double.toString(value));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GCodeWord that)) return false;
        return Character.toUpperCase(address) == Character.toUpperCase(that.address) &&
                Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Character.toUpperCase(address), value);
    }
}
