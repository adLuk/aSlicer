package cz.ad.print3d.aslicer.logic.net;

import cz.ad.print3d.aslicer.logic.net.scanner.dto.DiscoveredDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application-scope pool for managing active printer connections.
 * This class ensures that once a connection is established, it can be
 * reused across different parts of the application (e.g., from wizard steps
 * to the main dashboard). It also handles the lifecycle of these connections.
 */
public class PrinterConnectionPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterConnectionPool.class);

    private final Map<String, PrinterClient> activeClients = new ConcurrentHashMap<>();

    /**
     * Gets an existing client for the specified IP or creates a new one if it doesn't exist.
     *
     * @param device      the discovered device
     * @param credentials the connection credentials (access code, api key, etc.)
     * @return a {@link PrinterClient} instance, or null if it cannot be created
     */
    public PrinterClient getOrCreateClient(DiscoveredDevice device, Map<String, String> credentials) {
        String ip = device.getIpAddress();
        
        // If a client already exists, check if it's the same vendor and if it's connected
        // For simplicity, we create a new one if requested with credentials, 
        // but normally we'd want to reuse or update.
        // If we have an active client, we might want to keep it.
        
        return activeClients.compute(ip, (key, existingClient) -> {
            if (existingClient != null) {
                // Check if we should reuse existing client
                // For now, if we have a client, we reuse it to keep the connection open.
                // If credentials changed, we might need to recreate it.
                // But for the wizard, they are usually entered once.
                return existingClient;
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
    public void ensureAllConnected() {
        activeClients.values().forEach(client -> {
            if (!client.isConnected()) {
                LOGGER.info("Ensuring client for printer is connected...");
                client.connect().exceptionally(ex -> {
                    LOGGER.warn("Failed to reconnect pooled client: {}", ex.getMessage());
                    return null;
                });
            }
        });
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
     * Disconnects all active clients in the pool.
     */
    public void clear() {
        LOGGER.info("Clearing printer connection pool ({} clients)", activeClients.size());
        activeClients.values().forEach(PrinterClient::disconnect);
        activeClients.clear();
    }
}
