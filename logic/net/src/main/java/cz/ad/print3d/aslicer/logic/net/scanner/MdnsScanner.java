package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for discovering devices on the local network using mDNS (Multicast DNS).
 */
public interface MdnsScanner extends AutoCloseable {
    /**
     * Discovers devices on the local network using mDNS.
     *
     * @param timeoutMillis the timeout for the discovery process in milliseconds
     * @return a CompletableFuture that completes with a set of discovered services
     */
    CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis);

    @Override
    void close();
}
