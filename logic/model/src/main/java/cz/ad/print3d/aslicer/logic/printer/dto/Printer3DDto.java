package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.Printer3D;
import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import cz.ad.print3d.aslicer.logic.printer.toolhead.Toolhead;
import cz.ad.print3d.aslicer.logic.printer.topology.Topology;

import java.util.ArrayList;
import java.util.List;

public class Printer3DDto implements Printer3D {

    private PrinterSystem printerSystem;
    private Topology topology;
    private List<Toolhead> toolhead = new ArrayList<>();

    @Override
    public PrinterSystem getPrinterSystem() {
        return printerSystem;
    }

    public void setPrinterSystem(PrinterSystem printerSystem) {
        this.printerSystem = printerSystem;
    }

    @Override
    public Topology getTopology() {
        return topology;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    @Override
    public List<Toolhead> getToolhead() {
        return toolhead;
    }

    public void setToolhead(List<Toolhead> toolhead) {
        this.toolhead = toolhead;
    }

    public void addToolhead(Toolhead toolhead) {
        if (toolhead != null) {
            this.toolhead.add(toolhead);
        }
    }
}
