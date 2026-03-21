package cz.ad.print3d.aslicer.logic.printer.topology;

import cz.ad.print3d.aslicer.logic.printer.topology.geometry.Geometry;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.PrintArea;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.WorkArea;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.MovementLimits;

import java.nio.file.Path;

public interface Topology {

    Geometry getGeometry();

    PrintArea getPrintArea();

    WorkArea getWorkArea();

    Path getSchematicModelPath();

    MovementLimits getMovementLimits();
}
