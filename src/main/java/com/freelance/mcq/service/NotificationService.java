package com.freelance.mcq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.mcq.repository.DevicePushTokenRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class NotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final DevicePushTokenRepository devicePushTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationService(DevicePushTokenRepository devicePushTokenRepository) {
        this.devicePushTokenRepository = devicePushTokenRepository;
    }

    public void sendToTokens(List<String> tokens, String title, String body) {
        if (tokens.isEmpty()) return;

        // Expo recommends batching in chunks of 100
        int batchSize = 100;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            sendBatch(batch, title, body);
        }
    }

    private void sendBatch(List<String> tokens, String title, String body) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (String token : tokens) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("to", token);
            msg.put("title", title);
            msg.put("body", body);
            msg.put("sound", "default");
            messages.add(msg);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        try {
            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);
            String response = restTemplate.postForObject(EXPO_PUSH_URL, request, String.class);
            handleResponse(response, tokens);
        } catch (Exception e) {
            System.err.println("Push notification batch failed: " + e.getMessage());
        }
    }

    private void handleResponse(String response, List<String> sentTokens) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return;

            for (int i = 0; i < data.size() && i < sentTokens.size(); i++) {
                JsonNode result = data.get(i);
                String status = result.path("status").asText();
                if ("error".equals(status)) {
                    String errorCode = result.path("details").path("error").asText();
                    // DeviceNotRegistered means the token is permanently dead — clean it up
                    if ("DeviceNotRegistered".equals(errorCode)) {
                        devicePushTokenRepository.deleteByPushToken(sentTokens.get(i));
                        System.out.println("Removed dead token: " + sentTokens.get(i));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse push response: " + e.getMessage());
        }
    }
}