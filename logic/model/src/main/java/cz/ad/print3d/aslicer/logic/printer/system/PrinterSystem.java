package cz.ad.print3d.aslicer.logic.printer.system;

import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterAction;

import java.util.List;

public interface PrinterSystem {

    /**
     * @return the name of the printer manufacturer (e.g., "Bambu Lab", "Prusa Research").
     */
    String getPrinterManufacturer();

    /**
     * @return the human-readable name of the printer.
     */
    String getPrinterName();

    /**
     * @return the specific model name of the printer (e.g., "P1P", "X1C").
     */
    String getPrinterModel();

    /**
     * @return the flavor of G-code interpreter used by the printer (e.g., "Marlin", "RepRap").
     */
    String getGCodeInterpreter();

    /**
     * @return the current firmware version (software) installed on the printer.
     */
    String getFirmwareVersion();

    /**
     * @return the printer's hardware version, if available.
     */
    default String getHardwareVersion() { return null; }

    /**
     * @return the full report content from the printer, if available.
     */
    default String getFullReport() { return null; }

    /**
     * @return the printer's serial number, if available.
     */
    default String getSerialNumber() { return null; }

    /**
     * @return a list of available actions or commands supported by the printer system.
     */
    List<PrinterAction> getPrinterActions();
}
