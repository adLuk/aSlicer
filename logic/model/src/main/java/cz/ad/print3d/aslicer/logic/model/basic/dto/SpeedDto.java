package cz.ad.print3d.aslicer.logic.model.basic.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.model.basic.SpeedUnit;

import java.math.BigDecimal;

public class SpeedDto implements Speed {

    private SpeedUnit unit;
    private BigDecimal value;

    public SpeedDto() {
    }

    public SpeedDto(BigDecimal value, SpeedUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    @Override
    public SpeedUnit getUnit() {
        return unit;
    }

    public void setUnit(SpeedUnit unit) {
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
