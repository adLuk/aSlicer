package cz.ad.print3d.aslicer.logic.model.basic.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Temperature;
import cz.ad.print3d.aslicer.logic.model.basic.TemperatureUnit;

import java.math.BigDecimal;

public class TemperatureDto implements Temperature {

    private TemperatureUnit unit;
    private BigDecimal value;

    public TemperatureDto() {
    }

    public TemperatureDto(BigDecimal value, TemperatureUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    @Override
    public TemperatureUnit getUnit() {
        return unit;
    }

    public void setUnit(TemperatureUnit unit) {
        this.unit = unit;
    }

    @Override
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
