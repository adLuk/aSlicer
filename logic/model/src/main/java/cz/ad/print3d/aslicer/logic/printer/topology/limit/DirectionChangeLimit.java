package cz.ad.print3d.aslicer.logic.printer.topology.limit;

import cz.ad.print3d.aslicer.logic.model.basic.Speed;

public interface DirectionChangeLimit {

    Speed getMaximumSpeed();

    DirectionChangeLimitType getType();
}
