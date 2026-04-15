package cz.ad.print3d.aslicer.logic.printer.system;

import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterAction;

import java.util.List;

public interface PrinterSystem {

    String getPrinterManufacturer();

    String getPrinterName();

    String getPrinterModel();

    String getGCodeInterpreter();

    String getFirmwareVersion();
    
    /**
     * @return the printer's serial number, if available.
     */
    default String getSerialNumber() { return null; }

    List<PrinterAction> getPrinterActions();
}
