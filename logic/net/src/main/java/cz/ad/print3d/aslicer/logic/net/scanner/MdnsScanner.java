package cz.ad.print3d.aslicer.logic.net.scanner;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.MdnsServiceInfo;

import java.net.NetworkInterface;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for discovering devices on the local network using mDNS (Multicast DNS).
 */
public interface MdnsScanner extends AutoCloseable {
    /**
     * Interface for listening to discovered mDNS services in real-time.
     */
    interface MdnsDiscoveryListener {
        /**
         * Called when a new mDNS service is discovered.
         *
         * @param service the discovered service information
         */
        void onServiceDiscovered(MdnsServiceInfo service);
    }

    /**
     * Discovers devices on the local network using mDNS.
     *
     * @param timeoutMillis the timeout for the discovery process in milliseconds
     * @return a CompletableFuture that completes with a set of discovered services
     */
    default CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis) {
        return discoverDevices(timeoutMillis, null, null);
    }

    /**
     * Discovers devices on the local network using mDNS with a listener for real-time updates.
     *
     * @param timeoutMillis the timeout for the discovery process in milliseconds
     * @param listener      the listener to receive discovery events
     * @return a CompletableFuture that completes with a set of discovered services
     */
    default CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener) {
        return discoverDevices(timeoutMillis, listener, null);
    }

    /**
     * Discovers devices on the local network using mDNS with a listener for real-time updates and a specific network interface.
     *
     * @param timeoutMillis the timeout for the discovery process in milliseconds
     * @param listener      the listener to receive discovery events
     * @param networkInterface the network interface to use for mDNS discovery, or null for all interfaces
     * @return a CompletableFuture that completes with a set of discovered services
     */
    CompletableFuture<Set<MdnsServiceInfo>> discoverDevices(long timeoutMillis, MdnsDiscoveryListener listener, NetworkInterface networkInterface);

    /**
     * Stops any currently ongoing mDNS discovery.
     */
    void stopScan();

    @Override
    void close();
}
