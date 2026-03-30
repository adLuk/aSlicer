package cz.ad.print3d.aslicer.logic.net.bambu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BambuTopicsTest {

    @Test
    public void testTopics() {
        String serial = "SN123";
        assertEquals("device/SN123/report", BambuTopics.telemetry(serial));
        assertEquals("device/SN123/request", BambuTopics.request(serial));
        assertEquals("device/SN123/notify", BambuTopics.notify(serial));
    }
}
