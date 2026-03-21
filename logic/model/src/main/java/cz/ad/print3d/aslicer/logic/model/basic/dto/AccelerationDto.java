package cz.ad.print3d.aslicer.logic.model.basic.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Acceleration;
import cz.ad.print3d.aslicer.logic.model.basic.AccelerationUnit;

import java.math.BigDecimal;

public class AccelerationDto implements Acceleration {

    private AccelerationUnit unit;
    private BigDecimal value;

    public AccelerationDto() {
    }

    public AccelerationDto(BigDecimal value, AccelerationUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    @Override
    public AccelerationUnit getUnit() {
        return unit;
    }

    public void setUnit(AccelerationUnit unit) {
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
