package com.team4.core.notifications;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class TelegramTextSender {
    public record Result(boolean sent, long retryAfterSeconds) { }
    private final TelegramSettings settings;
    private final ObjectMapper json;
    private final HttpClient client;

    @Autowired
    public TelegramTextSender(TelegramSettings settings, ObjectMapper json) {
        this(settings, json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }
    TelegramTextSender(TelegramSettings settings, ObjectMapper json, HttpClient client) {
        this.settings = settings; this.json = json; this.client = client;
    }

    public Result send(String text) {
        if (!settings.enabled()) return new Result(false, 0);
        try {
            String body = json.writeValueAsString(Map.of("chat_id", settings.chatId(), "text", text));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + settings.token() + "/sendMessage"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> parsed = json.readValue(response.body(), Map.class);
            boolean sent = response.statusCode() == 200 && Boolean.TRUE.equals(parsed.get("ok"));
            long retryAfter = 0;
            if (response.statusCode() == 429 && parsed.get("parameters") instanceof Map<?, ?> parameters
                    && parameters.get("retry_after") instanceof Number number) {
                double value = number.doubleValue();
                if (Double.isFinite(value) && value > 0 && value == Math.rint(value)) retryAfter = (long) Math.min(value, 86400);
            }
            return new Result(sent, retryAfter);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Result(false, 0);
        } catch (Exception exception) {
            // URL HTTP chứa bot token. Không log request, response hoặc exception gốc.
            return new Result(false, 0);
        }
    }
}
