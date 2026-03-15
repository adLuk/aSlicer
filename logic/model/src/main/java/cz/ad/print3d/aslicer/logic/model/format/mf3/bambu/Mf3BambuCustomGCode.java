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
