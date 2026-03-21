package cz.ad.print3d.aslicer.logic.printer.system;

import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterAction;

import java.util.List;

public interface PrinterSystem {

    String getPrinterManufacturer();

    String getPrinterName();

    String getPrinterModel();

    String getGCodeInterpreter();

    String getFirmwareVersion();

    List<PrinterAction> getPrinterActions();
}
