package cz.ad.print3d.aslicer.logic.model.basic;

import java.util.concurrent.TimeUnit;

/**
 * Interface representing the unit of a speed measurement.
 *
 * <p>A speed unit is defined by a {@link LengthUnit} and a {@link TimeUnit}.</p>
 */
public interface SpeedUnit {

    /**
     * @return the length component of the speed unit.
     */
    LengthUnit getUnit();

    /**
     * @return the time component of the speed unit.
     */
    TimeUnit getTimeUnit();
}
