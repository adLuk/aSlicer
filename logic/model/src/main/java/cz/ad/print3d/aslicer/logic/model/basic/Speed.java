package cz.ad.print3d.aslicer.logic.model.basic;

import java.math.BigDecimal;

/**
 * Interface representing a speed measurement with a value and a unit.
 */
public interface Speed {

    /**
     * @return the numerical value of the speed.
     */
    BigDecimal getValue();

    /**
     * @return the {@link SpeedUnit} used for this speed measurement.
     */
    SpeedUnit getUnit();
}
