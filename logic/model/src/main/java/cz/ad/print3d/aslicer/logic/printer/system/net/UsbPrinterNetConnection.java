package cz.ad.print3d.aslicer.logic.printer.system.net;

/**
 * Interface representing a USB connection to a 3D printer.
 *
 * <p>Extends {@link PrinterNetConnection} for USB-specific functionality.</p>
 */
public interface UsbPrinterNetConnection extends PrinterNetConnection {

    /**
     * {@inheritDoc}
     */
    @Override
    default PrinterNetConnectionType getConnectionType() {
        return PrinterNetConnectionType.USB;
    }
}
