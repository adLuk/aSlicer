package cz.ad.print3d.aslicer.logic.model.basic;

import java.util.concurrent.TimeUnit;

public interface SpeedUnit {

    LengthUnit getUnit();

    TimeUnit getTimeUnit();
}
