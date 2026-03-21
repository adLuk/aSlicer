package cz.ad.print3d.aslicer.logic.printer.system.action;

public interface PrinterAction {

    PrinterActionType getType();

    String getActionCode();
}
