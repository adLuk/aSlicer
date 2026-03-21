package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.DirectionChangeLimit;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.DirectionChangeLimitType;

public class DirectionChangeLimitDto implements DirectionChangeLimit {

    private Speed maximumSpeed;
    private DirectionChangeLimitType type;

    @Override
    public Speed getMaximumSpeed() {
        return maximumSpeed;
    }

    public void setMaximumSpeed(Speed maximumSpeed) {
        this.maximumSpeed = maximumSpeed;
    }

    @Override
    public DirectionChangeLimitType getType() {
        return type;
    }

    public void setType(DirectionChangeLimitType type) {
        this.type = type;
    }
}
