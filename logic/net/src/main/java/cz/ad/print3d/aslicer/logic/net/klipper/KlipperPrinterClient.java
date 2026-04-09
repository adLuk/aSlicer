package cz.ad.print3d.aslicer.logic.net.klipper;

import cz.ad.print3d.aslicer.logic.net.AbstractPrinterClient;
import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import cz.ad.print3d.aslicer.logic.printer.dto.PrinterSystemDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with Klipper via Moonraker API.
 */
public class KlipperPrinterClient extends AbstractPrinterClient {
    private final String apiKey;
    private HttpClient httpClient;

    /**
     * Constructs a KlipperPrinterClient.
     *
     * @param ipAddress the IP address of the printer.
     * @param apiKey    the API key for authentication (optional for Moonraker).
     */
    public KlipperPrinterClient(String ipAddress, String apiKey) {
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
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ipAddress + "/printer/info"))
                .GET();
        
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("X-Api-Key", apiKey);
        }

        return getHttpClient().sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        return CompletableFuture.completedFuture(null);
                    } else {
                        return CompletableFuture.failedFuture(new RuntimeException("Failed to connect to Moonraker: " + response.statusCode()));
                    }
                });
    }

    @Override
    public CompletableFuture<Printer3DDto> getDetails() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("http://" + ipAddress + "/printer/info"))
                .GET();

        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("X-Api-Key", apiKey);
        }

        return getHttpClient().sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Printer3DDto dto = new Printer3DDto();
                        PrinterSystemDto system = new PrinterSystemDto();
                        system.setPrinterManufacturer("Klipper");
                        system.setPrinterName("Klipper at " + ipAddress);
                        dto.setPrinterSystem(system);
                        return dto;
                    } else {
                        throw new RuntimeException("Failed to get Moonraker details: " + response.statusCode());
                    }
                });
    }

    @Override
    public void disconnect() {
    }
}
