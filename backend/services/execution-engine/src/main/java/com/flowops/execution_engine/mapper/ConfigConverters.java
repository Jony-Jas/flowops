package com.flowops.execution_engine.mapper;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Helper converters for MapStruct.
 * The key method is protoToMap which attempts to convert common config types to Map<String,Object>.
 *
 * Supported inputs:
 *  - com.google.protobuf.Struct
 *  - java.util.Map
 *  - JSON String
 *  - fallback: stores raw.toString() under "raw"
 */
@Component
public class ConfigConverters {

    private static final Logger log = LoggerFactory.getLogger(ConfigConverters.class);
    private final ObjectMapper om = new ObjectMapper();

    @Named("protoToMap")
    public Map<String, Object> protoToMap(Object config) {
        if (config == null) return Collections.emptyMap();

        try {
            // If it's a protobuf Struct
            if (config instanceof Struct) {
                String json = JsonFormat.printer().includingDefaultValueFields().print((Struct) config);
                return om.readValue(json, new TypeReference<Map<String, Object>>() {});
            }

            // If it's already a Map
            if (config instanceof Map) {
                // unchecked cast safe-ish here for dynamic config maps
                return (Map<String, Object>) config;
            }

            // If it's a JSON string
            if (config instanceof String) {
                String s = (String) config;
                // detect simple JSON object
                if (s.trim().startsWith("{")) {
                    return om.readValue(s, new TypeReference<Map<String, Object>>() {});
                } else {
                    // not JSON — return raw representation
                    return Map.of("raw", s);
                }
            }

            // If it's a protobuf Message but not Struct (safe generic handling)
            if (config instanceof com.google.protobuf.Message) {
                String json = JsonFormat.printer().includingDefaultValueFields().print((com.google.protobuf.Message) config);
                return om.readValue(json, new TypeReference<Map<String, Object>>() {});
            }

            // fallback: return stringified raw
            return Map.of("raw", config.toString());
        } catch (Exception ex) {
            log.warn("Failed to convert config to Map: {}", ex.getMessage(), ex);
            // return something useful rather than failing the mapping
            return Map.of("raw", config.toString());
        }
    }
}
