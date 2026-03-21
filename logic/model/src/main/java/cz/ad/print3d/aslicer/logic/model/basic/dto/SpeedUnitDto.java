package cz.ad.print3d.aslicer.logic.model.basic.dto;

import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;
import cz.ad.print3d.aslicer.logic.model.basic.SpeedUnit;

import java.util.concurrent.TimeUnit;

public class SpeedUnitDto implements SpeedUnit {

    public static final SpeedUnit MILLIMETERS_PER_SECOND = new SpeedUnitDto(LengthUnit.MILLIMETER, TimeUnit.SECONDS);

    private LengthUnit lengthUnit;
    private TimeUnit timeUnit;

    public SpeedUnitDto() {
    }

    public SpeedUnitDto(LengthUnit lengthUnit, TimeUnit timeUnit) {
        this.lengthUnit = lengthUnit;
        this.timeUnit = timeUnit;
    }

    @Override
    public LengthUnit getUnit() {
        return lengthUnit;
    }

    public void setLengthUnit(LengthUnit lengthUnit) {
        this.lengthUnit = lengthUnit;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
