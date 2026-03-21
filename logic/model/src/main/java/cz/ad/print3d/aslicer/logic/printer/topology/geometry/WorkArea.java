package cz.ad.print3d.aslicer.logic.printer.topology.geometry;

import cz.ad.print3d.aslicer.logic.printer.topology.area.GenericArea;

import java.util.List;

public interface WorkArea extends GenericArea {

    List<MovementArea> getMovementAreas();
}
