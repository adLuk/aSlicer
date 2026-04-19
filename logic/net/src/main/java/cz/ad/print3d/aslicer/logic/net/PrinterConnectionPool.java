package cz.ad.print3d.aslicer.logic.net;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application-scope pool for managing active printer connections.
 * This class ensures that once a connection is established, it can be
 * reused across different parts of the application (e.g., from wizard steps
 * to the main dashboard). It also handles the lifecycle of these connections.
 */
public final class PrinterConnectionPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterConnectionPool.class);
    private static final long CHECK_INTERVAL_SEC = 30;

    private final Map<String, PrinterClient> activeClients = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    public PrinterConnectionPool() {
        start();
    }

    /**
     * Starts the background maintenance task.
     */
    public synchronized void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "PrinterPool-Maintenance");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::ensureAllConnected, CHECK_INTERVAL_SEC, CHECK_INTERVAL_SEC, TimeUnit.SECONDS);
        LOGGER.info("Printer connection pool maintenance started");
    }

    /**
     * Stops the background maintenance task.
     */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
            LOGGER.info("Printer connection pool maintenance stopped");
        }
    }

    /**
     * Gets an existing client for the specified IP or creates a new one if it doesn't exist.
     *
     * @param device      the discovered device
     * @param credentials the connection credentials (access code, api key, etc.)
     * @return a {@link PrinterClient} instance, or null if it cannot be created
     */
    public PrinterClient getOrCreateClient(DiscoveredDevice device, Map<String, String> credentials) {
        String ip = device.getIpAddress();
        
        return activeClients.compute(ip, (key, existingClient) -> {
            if (existingClient != null) {
                // Check if credentials changed
                Map<String, String> existingCreds = existingClient.getCredentials();
                if (existingCreds.equals(credentials)) {
                    LOGGER.debug("Reusing existing printer client for {}", ip);
                    return existingClient;
                }
                
                LOGGER.info("Credentials changed for {}, recreating client", ip);
                existingClient.disconnect();
            }
            
            PrinterClient newClient = PrinterClientFactory.createClient(device, credentials);
            if (newClient != null) {
                LOGGER.info("Created new printer client for {}", ip);
            }
            return newClient;
        });
    }

    /**
     * Gets an active client by IP address.
     *
     * @param ip the IP address of the printer
     * @return the {@link PrinterClient} if found, null otherwise
     */
    public PrinterClient getClient(String ip) {
        return activeClients.get(ip);
    }

    /**
     * Ensures all clients in the pool are connected.
     * This can be called periodically or when certain UI events occur.
     */
    public synchronized void ensureAllConnected() {
        activeClients.values().forEach(client -> {
            try {
                if (!client.isConnected()) {
                    LOGGER.info("Ensuring client for printer is connected...");
                    client.connect().exceptionally(ex -> {
                        LOGGER.warn("Failed to reconnect pooled client: {}", ex.getMessage());
                        return null;
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Error during connection maintenance for client: {}", e.getMessage());
            }
        });
    }

    /**
     * @return a map of all currently active clients in the pool.
     */
    public Map<String, PrinterClient> getAllClients() {
        return java.util.Collections.unmodifiableMap(activeClients);
    }

    /**
     * Removes and disconnects a client from the pool.
     *
     * @param ip the IP address of the printer to remove
     */
    public void removeClient(String ip) {
        PrinterClient client = activeClients.remove(ip);
        if (client != null) {
            LOGGER.info("Disconnecting and removing printer client for {}", ip);
            client.disconnect();
        }
    }

    /**
     * Disconnects all active clients in the pool and stops the maintenance task.
     */
    public void clear() {
        stop();
        LOGGER.info("Clearing printer connection pool ({} clients)", activeClients.size());
        activeClients.values().forEach(PrinterClient::disconnect);
        activeClients.clear();
    }
}
