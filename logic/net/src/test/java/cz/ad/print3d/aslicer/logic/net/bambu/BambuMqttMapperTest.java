package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class BambuMqttMapperTest {

    @Test
    public void testParsePrintTelemetry() throws JsonProcessingException {
        String json = "{\"print\":{\"gcode_state\":\"RUNNING\",\"mc_percent\":50,\"mc_remaining_time\":120,\"nozzle_temper\":210.5,\"bed_temper\":60.0}}";
        BambuMqttMapper mapper = new BambuMqttMapper();
        Map<String, Object> result = mapper.parse(json);

        assertTrue(result.containsKey("print"));
        Object printData = result.get("print");
        assertTrue(printData instanceof BambuTelemetry.BambuPrintStatus);

        BambuTelemetry.BambuPrintStatus status = (BambuTelemetry.BambuPrintStatus) printData;
        assertEquals("RUNNING", status.getGcodeState());
        assertEquals(50, status.getPercent());
        assertEquals(120, status.getRemainingTime());
        assertEquals(210.5, status.getNozzleTemperature());
        assertEquals(60.0, status.getBedTemperature());
    }

    @Test
    public void testParseUnknownSource() throws JsonProcessingException {
        String json = "{\"unknown_source\":{\"some_data\":1}}";
        BambuMqttMapper mapper = new BambuMqttMapper();
        Map<String, Object> result = mapper.parse(json);

        assertTrue(result.isEmpty());
    }
}
