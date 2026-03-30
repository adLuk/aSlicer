package cz.ad.print3d.aslicer.logic.printer.system.net;

/**
 * Base interface for representing a network connection to a 3D printer.
 *
 * <p>Provides access to the type of connection being used.</p>
 */
public interface PrinterNetConnection {

    /**
     * @return the type of connection (e.g., USB, NETWORK).
     */
    PrinterNetConnectionType getConnectionType();
}
