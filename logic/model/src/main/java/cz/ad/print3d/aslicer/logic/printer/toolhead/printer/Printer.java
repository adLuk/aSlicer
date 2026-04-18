package cz.ad.print3d.aslicer.logic.printer.toolhead.printer;

import cz.ad.print3d.aslicer.logic.model.basic.Dimension;
import cz.ad.print3d.aslicer.logic.model.basic.Speed;
import cz.ad.print3d.aslicer.logic.model.basic.Temperature;

import java.util.List;

/**
 * Interface representing the physical capabilities of a 3D printer's extrusion system.
 * This includes supported nozzle diameters, temperature limits, and mechanical
 * properties like retraction speed.
 */
public interface Printer {

    /**
     * Retrieves a list of supported nozzle diameters for this printer.
     *
     * @return a list of supported dimensions
     */
    List<Dimension> getPrintDiameters();

    /**
     * Retrieves a list of maximum operating temperatures for the printer's heaters.
     *
     * @return a list of maximum temperatures
     */
    List<Temperature> getMaxPrintTemperature();

    /**
     * Retrieves the default or maximum retraction speed supported by the printer's extruder.
     *
     * @return the retraction speed
     */
    Speed getRetractionSpeed();
}
