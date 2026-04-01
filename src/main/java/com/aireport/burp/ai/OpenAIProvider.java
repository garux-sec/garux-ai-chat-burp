package com.aireport.burp.ai;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI-compatible provider.
 * Works with:
 *  - OpenAI API (api.openai.com)
 *  - Azure OpenAI (set base URL in settings)
 *  - Any OpenAI-compatible endpoint (LM Studio, groq, etc.)
 */
public class OpenAIProvider implements AIProvider {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String getName() { return "OpenAI / Compatible"; }

    @Override
    public String sendMessage(List<ChatMessage> messages, AISettings settings) throws Exception {
        String baseUrl = settings.getOpenaiBaseUrl().replaceAll("/+$", "");
        String url     = baseUrl + "/chat/completions";
        String apiKey  = settings.getOpenaiApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not set. Go to Settings tab.");
        }

        // Build request body
        JsonObject body = new JsonObject();
        body.addProperty("model",       settings.getOpenaiModel());
        body.addProperty("max_tokens",  settings.getMaxTokens());
        body.addProperty("temperature", settings.getTemperature());

        JsonArray msgs = new JsonArray();
        for (ChatMessage m : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("role",    m.roleString());
            obj.addProperty("content", m.getContent());
            msgs.add(obj);
        }
        body.add("messages", msgs);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API error " + response.statusCode() + ": " + response.body());
        }

        JsonObject json  = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("No choices returned from OpenAI.");
        }
        return choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
