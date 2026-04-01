package com.aireport.burp.ai;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;

/**
 * Ollama provider — runs AI models locally (100% offline, free).
 *
 * Install Ollama: https://ollama.com
 * Pull a model:   ollama pull llama3
 * Default URL:    http://localhost:11434
 *
 * Uses the OpenAI-compatible /v1/chat/completions endpoint
 * (available in Ollama >= 0.1.24).
 */
public class OllamaProvider implements AIProvider {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public String getName() { return "Ollama (Local)"; }

    @Override
    public String sendMessage(List<ChatMessage> messages, AISettings settings) throws Exception {
        String baseUrl = settings.getOllamaBaseUrl().replaceAll("/+$", "");
        // Use the OpenAI-compatible endpoint
        String url = baseUrl + "/v1/chat/completions";

        // Build request body (OpenAI-compatible format)
        JsonObject body = new JsonObject();
        body.addProperty("model",       settings.getOllamaModel());
        body.addProperty("temperature", settings.getTemperature());
        body.addProperty("stream",      false);

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
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(300)) // local models can be slow
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IOException(
                "Cannot connect to Ollama at " + baseUrl + ". " +
                "Make sure Ollama is running (ollama serve) and the model is pulled.", e
            );
        }

        if (response.statusCode() != 200) {
            throw new IOException("Ollama error " + response.statusCode() + ": " + response.body());
        }

        JsonObject json    = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray  choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("No choices returned from Ollama.");
        }
        return choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}
