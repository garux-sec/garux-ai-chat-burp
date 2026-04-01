package com.aireport.burp.ai;

/**
 * Runtime settings passed to each AIProvider on every request.
 * These values come from the SettingsPanel (persisted in Burp's preference store).
 */
public class AISettings {

    // --- Provider selection ---
    private String provider     = "openai";   // openai | anthropic | ollama

    // --- OpenAI ---
    private String openaiApiKey = "";
    private String openaiModel  = "gpt-4o";
    private String openaiBaseUrl = "https://api.openai.com/v1";

    // --- Anthropic ---
    private String anthropicApiKey = "";
    private String anthropicModel  = "claude-opus-4-6";

    // --- Ollama (local) ---
    private String ollamaBaseUrl = "http://localhost:11434";
    private String ollamaModel   = "llama3";

    // --- General ---
    private int    maxTokens  = 4096;
    private double temperature = 0.7;
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    // ---------------------------------------------------------------
    public static final String DEFAULT_SYSTEM_PROMPT =
        "You are an expert web application security researcher and penetration tester. " +
        "You help analyze HTTP requests/responses, identify vulnerabilities, explain security " +
        "concepts, and suggest exploitation techniques for authorized testing. " +
        "Be concise, technical, and accurate. When analyzing HTTP traffic, look for: " +
        "injection flaws (SQLi, XSS, XXE, SSTI), authentication/authorization issues, " +
        "business logic flaws, information disclosure, SSRF, path traversal, and other OWASP Top 10 issues. " +
        "Always remind the user that testing should only be performed on systems they are authorized to test.";
    // ---------------------------------------------------------------

    // Getters & setters
    public String getProvider()            { return provider; }
    public void   setProvider(String v)    { provider = v; }

    public String getOpenaiApiKey()        { return openaiApiKey; }
    public void   setOpenaiApiKey(String v){ openaiApiKey = v; }

    public String getOpenaiModel()         { return openaiModel; }
    public void   setOpenaiModel(String v) { openaiModel = v; }

    public String getOpenaiBaseUrl()           { return openaiBaseUrl; }
    public void   setOpenaiBaseUrl(String v)   { openaiBaseUrl = v; }

    public String getAnthropicApiKey()         { return anthropicApiKey; }
    public void   setAnthropicApiKey(String v) { anthropicApiKey = v; }

    public String getAnthropicModel()          { return anthropicModel; }
    public void   setAnthropicModel(String v)  { anthropicModel = v; }

    public String getOllamaBaseUrl()           { return ollamaBaseUrl; }
    public void   setOllamaBaseUrl(String v)   { ollamaBaseUrl = v; }

    public String getOllamaModel()             { return ollamaModel; }
    public void   setOllamaModel(String v)     { ollamaModel = v; }

    public int    getMaxTokens()               { return maxTokens; }
    public void   setMaxTokens(int v)          { maxTokens = v; }

    public double getTemperature()             { return temperature; }
    public void   setTemperature(double v)     { temperature = v; }

    public String getSystemPrompt()            { return systemPrompt; }
    public void   setSystemPrompt(String v)    { systemPrompt = v; }
}
