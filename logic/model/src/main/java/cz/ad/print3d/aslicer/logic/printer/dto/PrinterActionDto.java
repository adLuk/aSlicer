package cz.ad.print3d.aslicer.logic.printer.dto;

import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterAction;
import cz.ad.print3d.aslicer.logic.printer.system.action.PrinterActionType;

public class PrinterActionDto implements PrinterAction {

    private PrinterActionType type;
    private String actionCode;

    @Override
    public PrinterActionType getType() {
        return type;
    }

    public void setType(PrinterActionType type) {
        this.type = type;
    }

    @Override
    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }
}
