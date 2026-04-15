package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.system.PrinterSystem;
import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterAction;

import java.util.ArrayList;
import java.util.List;

public class PrinterSystemDto implements PrinterSystem {

    private String printerManufacturer;
    private String printerName;
    private String printerModel;
    private String gCodeInterpreter;
    private String firmwareVersion;
    private String serialNumber;

    @Override
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    private List<PrinterAction> printerActions = new ArrayList<>();

    @Override
    public String getPrinterManufacturer() {
        return printerManufacturer;
    }

    public void setPrinterManufacturer(String printerManufacturer) {
        this.printerManufacturer = printerManufacturer;
    }

    @Override
    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    @Override
    public String getPrinterModel() {
        return printerModel;
    }

    public void setPrinterModel(String printerModel) {
        this.printerModel = printerModel;
    }

    @Override
    public String getGCodeInterpreter() {
        return gCodeInterpreter;
    }

    public void setGCodeInterpreter(String gCodeInterpreter) {
        this.gCodeInterpreter = gCodeInterpreter;
    }

    @Override
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    @Override
    public List<PrinterAction> getPrinterActions() {
        return printerActions;
    }

    public void setPrinterActions(List<PrinterAction> printerActions) {
        this.printerActions = printerActions;
    }

    public void addPrinterAction(PrinterAction action) {
        if (action != null) {
            this.printerActions.add(action);
        }
    }
}
