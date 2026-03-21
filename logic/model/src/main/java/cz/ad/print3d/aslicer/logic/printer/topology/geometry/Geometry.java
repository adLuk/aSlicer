package cz.ad.print3d.aslicer.logic.printer.topology.geometry;

import cz.ad.print3d.aslicer.logic.model.basic.Coordinates;
import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.LengthUnit;

import java.util.List;

public interface Geometry {

    Coordinates getCoordinateType();

    List<Dimension> getMinStepSize();

    List<Dimension> getMaxStepNumber();

    LengthUnit getUnits();

    WorkArea getWorkArea();

}
