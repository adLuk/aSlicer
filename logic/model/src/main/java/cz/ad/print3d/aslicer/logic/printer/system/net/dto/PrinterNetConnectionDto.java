package cz.ad.print3d.aslicer.logic.printer.system.net.dto;

import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnection;
import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnectionType;

/**
 * Base DTO for {@link PrinterNetConnection}.
 */
public class PrinterNetConnectionDto implements PrinterNetConnection {

    private PrinterNetConnectionType connectionType;

    @Override
    public PrinterNetConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(PrinterNetConnectionType connectionType) {
        this.connectionType = connectionType;
    }
}
