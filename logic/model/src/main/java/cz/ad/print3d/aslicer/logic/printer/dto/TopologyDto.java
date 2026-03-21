package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.topology.Topology;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.Geometry;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.PrintArea;
import cz.ad.print3d.aslicer.logic.printer.topology.geometry.WorkArea;
import cz.ad.print3d.aslicer.logic.printer.topology.limit.MovementLimits;

import java.nio.file.Path;

public class TopologyDto implements Topology {

    private Geometry geometry;
    private PrintArea printArea;
    private WorkArea workArea;
    private Path schematicModelPath;
    private MovementLimits movementLimits;

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    @Override
    public PrintArea getPrintArea() {
        return printArea;
    }

    public void setPrintArea(PrintArea printArea) {
        this.printArea = printArea;
    }

    @Override
    public WorkArea getWorkArea() {
        return workArea;
    }

    public void setWorkArea(WorkArea workArea) {
        this.workArea = workArea;
    }

    @Override
    public Path getSchematicModelPath() {
        return schematicModelPath;
    }

    public void setSchematicModelPath(Path schematicModelPath) {
        this.schematicModelPath = schematicModelPath;
    }

    @Override
    public MovementLimits getMovementLimits() {
        return movementLimits;
    }

    public void setMovementLimits(MovementLimits movementLimits) {
        this.movementLimits = movementLimits;
    }
}
