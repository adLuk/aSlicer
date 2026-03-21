package cz.ad.print3d.aslicer.logic.model.basic.dto;

import cz.ad.print3d.aslicer.logic.model.basic.AccelerationUnit;
import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;

import java.util.concurrent.TimeUnit;

public class AccelerationUnitDto extends SpeedUnitDto implements AccelerationUnit {

    public static final AccelerationUnit MILLIMETERS_PER_SECOND_SQUARED = new AccelerationUnitDto(LengthUnit.MILLIMETER, TimeUnit.SECONDS);

    public AccelerationUnitDto() {
    }

    public AccelerationUnitDto(LengthUnit lengthUnit, TimeUnit timeUnit) {
        super(lengthUnit, timeUnit);
    }
}
