package cz.ad.print3d.aslicer.logic.model.basic;

import java.math.BigDecimal;

public interface Acceleration {

    BigDecimal getValue();

    AccelerationUnit getUnit();
}
