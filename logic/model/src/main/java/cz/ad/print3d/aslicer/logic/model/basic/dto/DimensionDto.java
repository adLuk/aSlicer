package cz.ad.print3d.aslicer.logic.model.basic.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;

import java.math.BigDecimal;

public class DimensionDto implements Dimension {

    private BigDecimal value;
    private LengthUnit units;

    public DimensionDto() {
    }

    public DimensionDto(BigDecimal value, LengthUnit units) {
        this.value = value;
        this.units = units;
    }

    @Override
    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }
}
