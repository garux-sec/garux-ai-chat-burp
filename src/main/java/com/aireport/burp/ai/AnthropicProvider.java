package com.aireport.burp.ai;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

/**
 * Anthropic Claude provider.
 * Uses the Messages API: https://api.anthropic.com/v1/messages
 *
 * Note: Anthropic separates the "system" prompt from the messages array,
 * so we handle that split here.
 */
public class AnthropicProvider implements AIProvider {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    @Override
    public String getName() { return "Anthropic Claude"; }

    @Override
    public String sendMessage(List<ChatMessage> messages, AISettings settings) throws Exception {
        String apiKey = settings.getAnthropicApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key is not set. Go to Settings tab.");
        }

        // Extract system prompt (first system message, or from settings)
        String systemPrompt = settings.getSystemPrompt();
        JsonArray msgs = new JsonArray();

        for (ChatMessage m : messages) {
            if (m.getRole() == ChatMessage.Role.system) {
                // Use the last system message as the system prompt
                systemPrompt = m.getContent();
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("role",    m.roleString());   // "user" or "assistant"
                obj.addProperty("content", m.getContent());
                msgs.add(obj);
            }
        }

        // Build request body
        JsonObject body = new JsonObject();
        body.addProperty("model",      settings.getAnthropicModel());
        body.addProperty("max_tokens", settings.getMaxTokens());
        body.addProperty("system",     systemPrompt);
        body.add("messages", msgs);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",      "application/json")
                .header("x-api-key",         apiKey)
                .header("anthropic-version", API_VERSION)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Anthropic API error " + response.statusCode() + ": " + response.body());
        }

        JsonObject json    = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray  content = json.getAsJsonArray("content");
        if (content == null || content.isEmpty()) {
            throw new IOException("No content returned from Anthropic.");
        }
        // content[0].text
        return content.get(0).getAsJsonObject().get("text").getAsString();
    }
}
