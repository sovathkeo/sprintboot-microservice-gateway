package kh.com.cellcard.common.helper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

public abstract class JwtHelper {

    public static Map<String, String> decode(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null; // Invalid token format
        }
        String payload = parts[1]; // Payload is the second part
        try {
            // Decode the Base64url-encoded payload
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));

            // Parse the JSON payload into a Map
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(decodedPayload, Map.class);

        } catch (Exception e) {
            e.printStackTrace(); // Handle decoding/parsing errors
            return Map.of();
        }
    }
}
