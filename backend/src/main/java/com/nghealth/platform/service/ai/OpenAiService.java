package com.nghealth.platform.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nghealth.platform.config.OpenAiProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final OpenAiProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public OpenAiService(OpenAiProperties properties) {
        this.properties = properties;
    }

    public String chatJson(String system, List<Map<String, String>> messages) {
        if (!properties.enabled()) {
            return null;
        }
        try {
            var body = mapper.createObjectNode();
            body.put("model", properties.model());
            body.put("temperature", 0.3);
            body.putObject("response_format").put("type", "json_object");
            var msgArray = body.putArray("messages");
            msgArray.addObject().put("role", "system").put("content", system);
            for (var m : messages) {
                msgArray.addObject().put("role", m.get("role")).put("content", m.get("content"));
            }
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                return null;
            }
            JsonNode root = mapper.readTree(resp.body());
            return root.path("choices").path(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    public String chatText(String system, String userText) {
        if (!properties.enabled()) {
            return null;
        }
        try {
            var body = mapper.createObjectNode();
            body.put("model", properties.model());
            body.put("temperature", 0.2);
            var msgArray = body.putArray("messages");
            msgArray.addObject().put("role", "system").put("content", system);
            msgArray.addObject().put("role", "user").put("content", userText);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                return null;
            }
            JsonNode root = mapper.readTree(resp.body());
            return root.path("choices").path(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
