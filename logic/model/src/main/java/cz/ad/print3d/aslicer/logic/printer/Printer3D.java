package cz.ad.print3d.aslicer.logic.printer;

import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import cz.ad.print3d.aslicer.logic.printer.toolhead.Toolhead;
import cz.ad.print3d.aslicer.logic.printer.topology.Topology;

import java.util.List;

public interface Printer3D {

    PrinterSystem getPrinterSystem();

    Topology getTopology();

    List<Toolhead> getToolhead();

}
