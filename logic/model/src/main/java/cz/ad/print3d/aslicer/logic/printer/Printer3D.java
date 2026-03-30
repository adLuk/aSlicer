package cz.ad.print3d.aslicer.logic.printer;

import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnection;
import cz.ad.print3d.aslicer.logic.printer.toolhead.Toolhead;
import cz.ad.print3d.aslicer.logic.printer.topology.Topology;

import java.util.List;
import java.util.Map;

/**
 * Interface representing a 3D printer and its capabilities.
 *
 * @author Senior Architect
 * @since 1.0.0
 */
public interface Printer3D {

    /**
     * @return the {@link PrinterSystem} defining general printer properties.
     */
    PrinterSystem getPrinterSystem();

    /**
     * @return the {@link Topology} describing the printer's physical layout and limits.
     */
    Topology getTopology();

    /**
     * @return a list of {@link Toolhead}s available on the printer.
     */
    List<Toolhead> getToolhead();

    /**
     * Returns a map of defined network connections for this printer.
     *
     * @return a map where keys are connection names and values are {@link PrinterNetConnection} objects.
     */
    Map<String, PrinterNetConnection> getNetConnections();
}
