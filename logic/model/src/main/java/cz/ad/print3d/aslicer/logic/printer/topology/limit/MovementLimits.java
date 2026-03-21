package cz.ad.print3d.aslicer.logic.printer.topology.limit;

import cz.ad.print3d.aslicer.logic.model.basic.Acceleration;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;

public interface MovementLimits {

    Acceleration getMaximumAcceleration();

    Acceleration getMaximumDeceleration();

    Speed getMaximumSpeed();

    Speed getMinimumSpeed();

    DirectionChangeLimit getDirectionChangeSpeed();
}
