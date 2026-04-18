package cz.ad.print3d.aslicer.logic.printer;

import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnection;
import cz.ad.print3d.aslicer.logic.printer.toolhead.Toolhead;
import cz.ad.print3d.aslicer.logic.printer.topology.Topology;

import java.util.List;
import java.util.Map;

/**
 * Interface representing a 3D printer and its capabilities.
 * This interface provides access to the printer's system information,
 * physical topology, available toolheads, and network connection profiles.
 * It serves as the primary model for any supported 3D printer in the application.
 *
 * @author Senior Architect
 * @since 1.0.0
 */
public interface Printer3D {

    /**
     * Retrieves the system information of the printer.
     *
     * @return the {@link PrinterSystem} defining general properties like manufacturer, model, and firmware.
     */
    PrinterSystem getPrinterSystem();

    /**
     * Retrieves the physical topology of the printer.
     *
     * @return the {@link Topology} describing the build volume, axis limits, and coordinate system.
     */
    Topology getTopology();

    /**
     * Retrieves the list of toolheads installed on the printer.
     *
     * @return a list of {@link Toolhead} objects, each representing an extruder or other tool.
     */
    List<Toolhead> getToolhead();

    /**
     * Returns a map of network connection configurations defined for this printer.
     * Connection names can be used to distinguish between different access methods
     * (e.g., "Local LAN", "Cloud", "OctoPrint").
     *
     * @return a map where keys are connection identifiers and values are {@link PrinterNetConnection} profiles.
     */
    Map<String, PrinterNetConnection> getNetConnections();
}
