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
    public void testDiscoveryTopics() throws Exception {
        BambuPrinterNetConnection conn = new BambuMqttPrinterNetConnection(URI.create("https://192.168.0.10:8883").toURL(), null, "code");
        BambuMqttClient client = new BambuMqttClient(conn);
        assertEquals("#", client.getTelemetryTopic());
        assertNull(client.getRequestTopic());
    }
}
