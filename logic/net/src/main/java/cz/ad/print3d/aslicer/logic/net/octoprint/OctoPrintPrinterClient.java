package cz.ad.print3d.aslicer.logic.net.octoprint;

import cz.ad.print3d.aslicer.logic.net.AbstractPrinterClient;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.logic.printer.dto.PrinterSystemDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with OctoPrint.
 */
public class OctoPrintPrinterClient extends AbstractPrinterClient {
    private final String apiKey;
    private HttpClient httpClient;

    /**
     * Constructs an OctoPrintPrinterClient.
     *
     * @param ipAddress the IP address of the printer.
     * @param apiKey    the API key for authentication.
     */
    public OctoPrintPrinterClient(String ipAddress, String apiKey) {
        super(ipAddress);
        this.apiKey = apiKey;
    }

    private synchronized HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = createHttpClient();
        }
        return httpClient;
    }

    @Override
    public CompletableFuture<Void> connect() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ipAddress + "/api/version"))
                .header("X-Api-Key", apiKey)
                .GET()
                .build();

        return getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return CompletableFuture.failedFuture(new RuntimeException("Failed to connect to OctoPrint: " + response.statusCode()));
                    }
                });
    }

    @Override
    public CompletableFuture<Printer3DDto> getDetails() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ipAddress + "/api/version"))
                .header("X-Api-Key", apiKey)
                .GET()
                .build();

        return getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Printer3DDto dto = new Printer3DDto();
                        PrinterSystemDto system = new PrinterSystemDto();
                        system.setPrinterManufacturer("OctoPrint");
                        system.setPrinterName("OctoPrint on " + ipAddress);
                        // In real implementation, parse JSON response for version, etc.
                        dto.setPrinterSystem(system);
                        return dto;
                    } else {
                        throw new RuntimeException("Failed to get OctoPrint details: " + response.statusCode());
                    }
                });
    }

    @Override
    public java.util.Map<String, String> getCredentials() {
        java.util.Map<String, String> credentials = new java.util.HashMap<>();
        credentials.put("apiKey", apiKey);
        return credentials;
    }

    @Override
    public boolean isConnected() {
        return httpClient != null;
    }

    @Override
    public void disconnect() {
        // No persistent connection to close for REST
    }
}
