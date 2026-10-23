package org.usf.inspect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * 
 * @author u$f
 *
 */
class InspectCoreModuleTest {

    @Test
    void testRegisterSubtypes() throws Exception {
        
    	var mapper = assertDoesNotThrow(InspectCoreModule::jsonMapperBuilder, "Failed to create ObjectMapper").build();
        
        var stream = InspectCoreModuleTest.class.getResourceAsStream("/org/usf/inspect/traces-register-subtypes.json");
        assertNotNull(stream);
        var originalJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        var traces = assertDoesNotThrow(()-> mapper.readValue(originalJson, EventTrace[].class), "Failed to deserialize JSON into EventTrace[]");
        var reserializedJson = assertDoesNotThrow(()-> mapper.writeValueAsString(traces), "Failed to serialize EventTrace[] back to JSON");

        assertEquals(mapper.readTree(originalJson), mapper.readTree(reserializedJson), "Reserialized JSON does not match the original");
    }
}
