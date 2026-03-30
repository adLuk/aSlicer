package cz.ad.print3d.aslicer.logic.printer.system.net.dto;

import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnection;
import cz.ad.print3d.aslicer.logic.printer.system.net.PrinterNetConnectionType;

/**
 * Base DTO for {@link PrinterNetConnection}.
 */
public class PrinterNetConnectionDto implements PrinterNetConnection {

    private final PrinterNetConnectionType connectionType;

    public PrinterNetConnectionDto() {
        this(PrinterNetConnectionType.CUSTOM);
    }

    protected PrinterNetConnectionDto(PrinterNetConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    @Override
    public PrinterNetConnectionType getConnectionType() {
        return connectionType;
    }
}
