package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.model.basic.Acceleration;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.DirectionChangeLimit;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.MovementLimits;

public class MovementLimitsDto implements MovementLimits {

    private Acceleration maximumAcceleration;
    private Acceleration maximumDeceleration;
    private Speed maximumSpeed;
    private Speed minimumSpeed;
    private DirectionChangeLimit directionChangeSpeed;

    @Override
    public Acceleration getMaximumAcceleration() {
        return maximumAcceleration;
    }

    public void setMaximumAcceleration(Acceleration maximumAcceleration) {
        this.maximumAcceleration = maximumAcceleration;
    }

    @Override
    public Acceleration getMaximumDeceleration() {
        return maximumDeceleration;
    }

    public void setMaximumDeceleration(Acceleration maximumDeceleration) {
        this.maximumDeceleration = maximumDeceleration;
    }

    @Override
    public Speed getMaximumSpeed() {
        return maximumSpeed;
    }

    public void setMaximumSpeed(Speed maximumSpeed) {
        this.maximumSpeed = maximumSpeed;
    }

    @Override
    public Speed getMinimumSpeed() {
        return minimumSpeed;
    }

    public void setMinimumSpeed(Speed minimumSpeed) {
        this.minimumSpeed = minimumSpeed;
    }

    @Override
    public DirectionChangeLimit getDirectionChangeSpeed() {
        return directionChangeSpeed;
    }

    public void setDirectionChangeSpeed(DirectionChangeLimit directionChangeSpeed) {
        this.directionChangeSpeed = directionChangeSpeed;
    }
}
