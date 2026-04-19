package cz.ad.print3d.aslicer.logic.net.prusa;

import cz.ad.print3d.aslicer.logic.net.AbstractPrinterClient;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.logic.printer.dto.PrinterSystemDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with Prusa printers via PrusaLink.
 */
public class PrusaPrinterClient extends AbstractPrinterClient {
    private final String apiKey;
    private HttpClient httpClient;

    /**
     * Constructs a PrusaPrinterClient.
     *
     * @param ipAddress the IP address of the printer.
     * @param apiKey    the API key for authentication.
     */
    public PrusaPrinterClient(String ipAddress, String apiKey) {
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
                .uri(URI.create("http://" + ipAddress + "/api/v1/status"))
                .header("X-Api-Key", apiKey)
                .GET()
                .build();

        return getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return CompletableFuture.failedFuture(new RuntimeException("Failed to connect to PrusaLink: " + response.statusCode()));
                    }
                });
    }

    @Override
    public CompletableFuture<Printer3DDto> getDetails() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ipAddress + "/api/v1/status"))
                .header("X-Api-Key", apiKey)
                .GET()
                .build();

        return getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Printer3DDto dto = new Printer3DDto();
                        PrinterSystemDto system = new PrinterSystemDto();
                        system.setPrinterManufacturer("Prusa Research");
                        system.setPrinterName("Prusa Printer at " + ipAddress);
                        dto.setPrinterSystem(system);
                        return dto;
                    } else {
                        throw new RuntimeException("Failed to get PrusaLink details: " + response.statusCode());
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
    }
}
