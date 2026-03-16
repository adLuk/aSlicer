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
package cz.ad.print3d.aslicer.logic.model.format.mf3.bambu;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Mf3BambuCustomGCode {
    @JsonProperty("custom_gcode_per_plate")
    private String[] customGCodePerPlate;

    public String[] getCustomGCodePerPlate() { return customGCodePerPlate; }
    public void setCustomGCodePerPlate(String[] customGCodePerPlate) { this.customGCodePerPlate = customGCodePerPlate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mf3BambuCustomGCode that = (Mf3BambuCustomGCode) o;
        return java.util.Arrays.equals(customGCodePerPlate, that.customGCodePerPlate);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(customGCodePerPlate);
    }
}
