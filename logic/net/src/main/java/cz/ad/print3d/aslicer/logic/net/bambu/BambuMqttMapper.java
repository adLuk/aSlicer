package cz.ad.print3d.aslicer.logic.net.bambu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for parsing Bambu Lab MQTT telemetry topics.
 *
 * <p>The mapper maintains a mapping between the JSON root keys in the MQTT payload
 * and the corresponding Java DTO classes used for deserialization.</p>
 */
public class BambuMqttMapper {

    private final ObjectMapper objectMapper;
    private final Map<String, Class<?>> sourceMapping;

    /**
     * Constructs a new BambuMqttMapper with default mappings.
     *
     * <p>Default mappings include:
     * <ul>
     *     <li>"print" -> {@link BambuTelemetry.BambuPrintStatus}</li>
     *     <li>"system" -> {@link BambuSystemStatus}</li>
     * </ul>
     * </p>
     */
    public BambuMqttMapper() {
        this.objectMapper = new ObjectMapper();
        this.sourceMapping = new HashMap<>();
        // Default mapping
        sourceMapping.put("print", BambuTelemetry.BambuPrintStatus.class);
        sourceMapping.put("system", BambuSystemStatus.class);
    }

    /**
     * Parses the MQTT JSON payload and maps recognized source keys to their corresponding DTOs.
     *
     * <p>The mapper iterates through the registered source keys and checks if the root JSON node
     * contains that key. If present, the corresponding node is deserialized into the mapped class.</p>
     *
     * @param payload the MQTT JSON payload string.
     * @return a map where keys are the source names and values are the deserialized DTO objects.
     * @throws JsonProcessingException if the payload is not valid JSON or if deserialization fails.
     */
    public Map<String, Object> parse(String payload) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(payload);
        Map<String, Object> result = new HashMap<>();

        java.util.Iterator<String> fieldNames = root.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode node = root.get(fieldName);
            
            if (sourceMapping.containsKey(fieldName)) {
                Object data = objectMapper.treeToValue(node, sourceMapping.get(fieldName));
                result.put(fieldName, data);
            } else {
                // For unknown sources, put the raw node as a generic map or string
                result.put(fieldName, node);
            }
            // Always include raw payload for each root key discovered
            result.put(fieldName + "_raw", payload);
        }
        return result;
    }

    /**
     * Registers a new source key and its corresponding mapping class.
     *
     * <p>This allows extending the mapper to handle new telemetry sources or to
     * override existing ones.</p>
     *
     * @param source  the root JSON key to map (e.g., "print").
     * @param clazz   the class to use for deserializing the value of that key.
     */
    public void registerSource(String source, Class<?> clazz) {
        sourceMapping.put(source, clazz);
    }
}
