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
    public void testParseAmsTelemetry() throws JsonProcessingException {
        String json = "{\"print\":{\"ams\":{\"ams\":[{\"id\":\"0\",\"temp\":\"25.5\",\"humidity\":\"18\"}]}}}";
        BambuMqttMapper mapper = new BambuMqttMapper();
        Map<String, Object> result = mapper.parse(json);

        assertTrue(result.containsKey("print"));
        BambuTelemetry.BambuPrintStatus status = (BambuTelemetry.BambuPrintStatus) result.get("print");
        assertNotNull(status.getAms());
        assertEquals(1, status.getAms().getAmsDevices().size());
        assertEquals("25.5", status.getAms().getAmsDevices().get(0).getTemp());
        assertEquals("18", status.getAms().getAmsDevices().get(0).getHumidity());
    }

    @Test
    public void testParseGetVersion() throws JsonProcessingException {
        String json = "{\"system\":{\"get_version\":{\"module\":[{\"name\":\"ota\",\"sw_ver\":\"01.05.02.00\",\"hw_ver\":\"0.0.0.0\"}]}}}";
        BambuMqttMapper mapper = new BambuMqttMapper();
        Map<String, Object> result = mapper.parse(json);

        assertTrue(result.containsKey("system"));
        assertTrue(result.containsKey("system_raw"));
        assertEquals(json, result.get("system_raw"));
        
        BambuSystemStatus status = (BambuSystemStatus) result.get("system");
        assertNotNull(status.getGetVersion());
        assertEquals(1, status.getGetVersion().getModules().size());
        assertEquals("ota", status.getGetVersion().getModules().get(0).getName());
        assertEquals("01.05.02.00", status.getGetVersion().getModules().get(0).getSwVer());
        assertEquals("0.0.0.0", status.getGetVersion().getModules().get(0).getHwVer());
    }

    @Test
    public void testParseUnknownSource() throws JsonProcessingException {
        String json = "{\"unknown_source\":{\"some_data\":1}}";
        BambuMqttMapper mapper = new BambuMqttMapper();
        Map<String, Object> result = mapper.parse(json);

        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("unknown_source"));
        assertTrue(result.containsKey("unknown_source_raw"));
        assertEquals(json, result.get("unknown_source_raw"));
    }
}
