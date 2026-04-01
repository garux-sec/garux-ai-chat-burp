package com.aireport.burp.ai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory / registry for AI providers.
 * Add new providers here — they automatically appear in the Settings dropdown.
 */
public class AIProviderFactory {

    /** key = provider id used in AISettings.getProvider() */
    private static final Map<String, AIProvider> PROVIDERS = new LinkedHashMap<>();

    static {
        register("openai",    new OpenAIProvider());
        register("anthropic", new AnthropicProvider());
        register("ollama",    new OllamaProvider());
    }

    private static void register(String id, AIProvider provider) {
        PROVIDERS.put(id, provider);
    }

    /** Returns all registered providers (id → provider). */
    public static Map<String, AIProvider> all() {
        return PROVIDERS;
    }

    /**
     * Returns the provider for the given id.
     * Falls back to OpenAI if id is unknown.
     */
    public static AIProvider get(String id) {
        return PROVIDERS.getOrDefault(id, PROVIDERS.get("openai"));
    }
}
