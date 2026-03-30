package cz.ad.print3d.aslicer.logic.net.bambu;

import cz.ad.print3d.aslicer.logic.printer.system.net.BambuPrinterNetConnection;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class BambuMqttClientTest {

    @Test
    public void testTopicBuilders() throws Exception {
        BambuPrinterNetConnection conn = new BambuMqttPrinterNetConnection(URI.create("https://192.168.0.10:8883").toURL(), "SN999", "code");
        BambuMqttClient client = new BambuMqttClient(conn);
        assertEquals("device/SN999/report", client.getTelemetryTopic());
        assertEquals("device/SN999/request", client.getRequestTopic());
    }

    @Test
    public void testTelemetryDispatch() throws Exception {
        BambuPrinterNetConnection conn = new BambuMqttPrinterNetConnection(URI.create("https://192.168.0.10:8883").toURL(), "SN999", "code");
        BambuMqttClient client = new BambuMqttClient(conn);
        AtomicReference<Map<String, Object>> ref = new AtomicReference<>();
        client.setTelemetryConsumer(ref::set);
        String payload = "{\"print\":{\"gcode_state\":\"RUNNING\",\"mc_percent\":10,\"mc_remaining_time\":5,\"nozzle_temper\":200,\"bed_temper\":60}}";
        client.handleTelemetry(payload);
        assertNotNull(ref.get());
        assertTrue(ref.get().containsKey("print"));
    }
}
