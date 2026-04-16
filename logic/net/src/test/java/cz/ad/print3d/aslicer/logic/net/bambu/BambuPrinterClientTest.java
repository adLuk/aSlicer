package cz.ad.print3d.aslicer.logic.net.bambu;

import cz.ad.print3d.aslicer.logic.printer.dto.Printer3DDto;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BambuPrinterClientTest {

    @Test
    void testConnectDoesNotThrowHandshakeInitializationError() {
        // Use a dummy IP that won't respond to speed up things or just test the future creation
        BambuPrinterClient client = new BambuPrinterClient("127.0.0.1", "SN123", "access");
        CompletableFuture<Void> future = client.connect();
        
        // At this point, the client should be built and connection initiated.
        // We don't wait for completion because it would fail on localhost,
        // but we've verified that the SSL/MQTT3 builder didn't crash.
        assertNotNull(future);
        client.disconnect();
    }

    @Test
    public void testFallbackToPrintRawWhenSystemMissing() throws Exception {
        BambuPrinterClient client = new BambuPrinterClient("127.0.0.1", "SERIAL123", "code");
        
        // Correctly initialize mqttClient using internal implementation
        java.net.URL url = java.net.URI.create("https://127.0.0.1:8883").toURL();
        BambuMqttPrinterNetConnection connection = new BambuMqttPrinterNetConnection(url, "SERIAL123", "code");
        BambuMqttClient realMqtt = new BambuMqttClient(connection);
        
        java.lang.reflect.Field field = BambuPrinterClient.class.getDeclaredField("mqttClient");
        field.setAccessible(true);
        field.set(client, realMqtt);

        // Simulate receiving a "print" message but no "system" message
        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("print_raw", "{\"print\": {\"nozzle_temper\": 200}}");
        
        // Use reflection to call private handleTelemetry
        java.lang.reflect.Method method = BambuPrinterClient.class.getDeclaredMethod("handleTelemetry", Map.class);
        method.setAccessible(true);
        method.invoke(client, telemetry);
        
        Printer3DDto details = client.getDetails().get(1, TimeUnit.SECONDS);
        assertNotNull(details.getPrinterSystem());
        assertEquals("{\"print\": {\"nozzle_temper\": 200}}", details.getPrinterSystem().getFullReport());
    }

    @Test
    public void testSystemReportTakesPrecedence() throws Exception {
        BambuPrinterClient client = new BambuPrinterClient("127.0.0.1", "SERIAL123", "code");
        
        java.net.URL url = java.net.URI.create("https://127.0.0.1:8883").toURL();
        BambuMqttPrinterNetConnection connection = new BambuMqttPrinterNetConnection(url, "SERIAL123", "code");
        BambuMqttClient realMqtt = new BambuMqttClient(connection);
        
        java.lang.reflect.Field field = BambuPrinterClient.class.getDeclaredField("mqttClient");
        field.setAccessible(true);
        field.set(client, realMqtt);

        // 1. Send print message
        Map<String, Object> telemetry1 = new HashMap<>();
        telemetry1.put("print_raw", "PRINT_RAW");
        
        java.lang.reflect.Method method = BambuPrinterClient.class.getDeclaredMethod("handleTelemetry", Map.class);
        method.setAccessible(true);
        method.invoke(client, telemetry1);
        
        // 2. Send system message
        Map<String, Object> telemetry2 = new HashMap<>();
        telemetry2.put("system", new BambuSystemStatus());
        telemetry2.put("system_raw", "SYSTEM_RAW");
        method.invoke(client, telemetry2);
        
        Printer3DDto details = client.getDetails().get(1, TimeUnit.SECONDS);
        assertEquals("SYSTEM_RAW", details.getPrinterSystem().getFullReport());
    }
}
