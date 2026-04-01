package com.aireport.burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Preferences;
import com.aireport.burp.ai.AIProviderFactory;
import com.aireport.burp.ai.AISettings;
import com.google.gson.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.List;

/**
 * Settings panel — "Settings" sub-tab.
 *
 * Features:
 *  - All API key fields are masked (JPasswordField)
 *  - Model fields are editable JComboBox
 *  - "↻ List Models" button fetches available models from the API
 *    · OpenAI / Compatible : GET {baseUrl}/models
 *    · Anthropic            : hardcoded known models list
 *    · Ollama               : GET {ollamaUrl}/api/tags
 */
public class SettingsPanel extends JPanel {

    // ── Preference keys ────────────────────────────────────────────────
    private static final String PREF_PROVIDER        = "ai_chat.provider";
    private static final String PREF_OPENAI_KEY      = "ai_chat.openai.key";
    private static final String PREF_OPENAI_MODEL    = "ai_chat.openai.model";
    private static final String PREF_OPENAI_URL      = "ai_chat.openai.url";
    private static final String PREF_ANTHROPIC_KEY   = "ai_chat.anthropic.key";
    private static final String PREF_ANTHROPIC_MODEL = "ai_chat.anthropic.model";
    private static final String PREF_OLLAMA_URL      = "ai_chat.ollama.url";
    private static final String PREF_OLLAMA_MODEL    = "ai_chat.ollama.model";
    private static final String PREF_MAX_TOKENS      = "ai_chat.max_tokens";
    private static final String PREF_TEMPERATURE     = "ai_chat.temperature";
    private static final String PREF_SYSTEM_PROMPT   = "ai_chat.system_prompt";

    // ── Known Anthropic models (no public list endpoint) ───────────────
    private static final String[] ANTHROPIC_KNOWN_MODELS = {
        "claude-opus-4-6",
        "claude-3-5-sonnet-20241022",
        "claude-3-5-haiku-20241022",
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307"
    };

    private final MontoyaApi  api;
    private final Preferences prefs;

    // ── UI components ─────────────────────────────────────────────────
    private JComboBox<String>  providerCombo;

    // OpenAI
    private JPasswordField     openaiKeyField;
    private JComboBox<String>  openaiModelCombo;
    private JTextField         openaiUrlField;
    private JButton            openaiListBtn;

    // Anthropic
    private JPasswordField     anthropicKeyField;
    private JComboBox<String>  anthropicModelCombo;

    // Ollama
    private JTextField         ollamaUrlField;
    private JComboBox<String>  ollamaModelCombo;
    private JButton            ollamaListBtn;

    // General
    private JTextField         maxTokensField;
    private JTextField         temperatureField;
    private JTextArea          systemPromptArea;

    private JPanel      providerCards;
    private CardLayout  cardLayout;

    public SettingsPanel(MontoyaApi api) {
        this.api   = api;
        this.prefs = api.persistence().preferences();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildUI();
        loadPreferences();
    }

    // ──────────────────────────────────────────────────────────────────
    // Build UI
    // ──────────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        // ── Provider combo ────────────────────────────────────────────
        JPanel providerPanel = titledPanel("AI Provider");
        providerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));

        providerCombo = new JComboBox<>();
        for (Map.Entry<String, ?> e : AIProviderFactory.all().entrySet()) {
            providerCombo.addItem(e.getKey());
        }
        providerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, idx, sel, focus);
                if (value != null) setText(AIProviderFactory.get(value.toString()).getName());
                return this;
            }
        });
        providerPanel.add(new JLabel("Provider:"));
        providerPanel.add(providerCombo);

        // ── Provider cards ────────────────────────────────────────────
        cardLayout    = new CardLayout();
        providerCards = new JPanel(cardLayout);
        providerCards.add(buildOpenAICard(),    "openai");
        providerCards.add(buildAnthropicCard(), "anthropic");
        providerCards.add(buildOllamaCard(),    "ollama");

        providerCombo.addActionListener(e -> {
            String sel = (String) providerCombo.getSelectedItem();
            if (sel != null) cardLayout.show(providerCards, sel);
        });

        // ── General settings ──────────────────────────────────────────
        JPanel generalPanel = titledPanel("General");
        generalPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill   = GridBagConstraints.HORIZONTAL;

        maxTokensField   = new JTextField("4096", 8);
        temperatureField = new JTextField("0.7", 8);

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        generalPanel.add(new JLabel("Max Tokens:"),  gc);
        gc.gridx = 1; gc.weightx = 1;
        generalPanel.add(maxTokensField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        generalPanel.add(new JLabel("Temperature:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        generalPanel.add(temperatureField, gc);

        // ── System prompt ─────────────────────────────────────────────
        JPanel systemPanel = titledPanel("System Prompt");
        systemPanel.setLayout(new BorderLayout(4, 4));
        systemPromptArea = new JTextArea(AISettings.DEFAULT_SYSTEM_PROMPT, 6, 60);
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        systemPanel.add(new JScrollPane(systemPromptArea), BorderLayout.CENTER);

        JButton resetBtn = new JButton("Reset to Default");
        resetBtn.addActionListener(e -> systemPromptArea.setText(AISettings.DEFAULT_SYSTEM_PROMPT));
        JPanel resetWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resetWrap.add(resetBtn);
        systemPanel.add(resetWrap, BorderLayout.SOUTH);

        // ── Save button ───────────────────────────────────────────────
        JButton saveBtn = new JButton("Save Settings");
        saveBtn.setBackground(new Color(0x2196F3));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD));
        saveBtn.addActionListener(e -> savePreferences());
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnWrap.add(saveBtn);

        main.add(providerPanel);
        main.add(Box.createVerticalStrut(6));
        main.add(providerCards);
        main.add(Box.createVerticalStrut(6));
        main.add(generalPanel);
        main.add(Box.createVerticalStrut(6));
        main.add(systemPanel);

        add(new JScrollPane(main), BorderLayout.CENTER);
        add(btnWrap, BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────────────────
    // Provider cards
    // ──────────────────────────────────────────────────────────────────
    private JPanel buildOpenAICard() {
        JPanel p = titledPanel("OpenAI / Compatible Settings");
        p.setLayout(new GridBagLayout());
        GridBagConstraints gc = defaultGC();

        openaiKeyField   = new JPasswordField(36);
        openaiUrlField   = new JTextField("https://api.openai.com/v1", 36);
        openaiModelCombo = editableCombo("gpt-4o", "gpt-4o-mini", "gpt-4-turbo",
                                         "gpt-4", "gpt-3.5-turbo");
        openaiListBtn    = listButton("↻ List Models", this::fetchOpenAIModels);

        // Row 0: API Key
        addLabelField(p, gc, 0, "API Key:", openaiKeyField, "Your OpenAI secret key  (sk-...)");
        // Row 1: Base URL
        addLabelField(p, gc, 1, "Base URL:", openaiUrlField, "Change for Azure / LM Studio / Groq");
        // Row 2: Model (combo + list button)
        addLabelComboButton(p, gc, 2, "Model:", openaiModelCombo, openaiListBtn,
                            "Type or select from list");
        return p;
    }

    private JPanel buildAnthropicCard() {
        JPanel p = titledPanel("Anthropic Claude Settings");
        p.setLayout(new GridBagLayout());
        GridBagConstraints gc = defaultGC();

        anthropicKeyField   = new JPasswordField(36);
        anthropicModelCombo = editableCombo(ANTHROPIC_KNOWN_MODELS);

        addLabelField(p, gc, 0, "API Key:", anthropicKeyField, "Your Anthropic API key");
        addLabelComboButton(p, gc, 1, "Model:", anthropicModelCombo, null,
                            "Known models are pre-loaded");
        return p;
    }

    private JPanel buildOllamaCard() {
        JPanel p = titledPanel("Ollama (Local) Settings");
        p.setLayout(new GridBagLayout());
        GridBagConstraints gc = defaultGC();

        ollamaUrlField   = new JTextField("http://localhost:11434", 36);
        ollamaModelCombo = editableCombo("llama3", "mistral", "codellama", "deepseek-r1", "phi3");
        ollamaListBtn    = listButton("↻ List Models", this::fetchOllamaModels);

        addLabelField(p, gc, 0, "Ollama URL:", ollamaUrlField, "URL where Ollama is running");
        addLabelComboButton(p, gc, 1, "Model:", ollamaModelCombo, ollamaListBtn,
                            "Type or select; pull first with: ollama pull <model>");

        GridBagConstraints hc = defaultGC();
        hc.gridx = 0; hc.gridy = 2; hc.gridwidth = 4;
        JLabel hint = new JLabel(
            "<html><i style='color:gray'>Run: <b>ollama serve</b> then <b>ollama pull llama3</b></i></html>");
        p.add(hint, hc);
        return p;
    }

    // ──────────────────────────────────────────────────────────────────
    // Fetch models from APIs (run in background thread)
    // ──────────────────────────────────────────────────────────────────

    /** Fetch models from OpenAI /v1/models endpoint */
    private void fetchOpenAIModels() {
        String apiKey = new String(openaiKeyField.getPassword()).trim();
        String base   = openaiUrlField.getText().trim().replaceAll("/+$", "");

        if (apiKey.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter your API Key first.", "AI Chat", JOptionPane.WARNING_MESSAGE);
            return;
        }

        openaiListBtn.setEnabled(false);
        openaiListBtn.setText("Loading...");

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(base + "/models"))
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> resp = HttpClient.newHttpClient()
                        .send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() != 200) {
                    throw new RuntimeException("API error " + resp.statusCode() + ": " + resp.body());
                }

                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                JsonArray  data = json.getAsJsonArray("data");

                List<String> ids = new ArrayList<>();
                for (JsonElement el : data) {
                    ids.add(el.getAsJsonObject().get("id").getAsString());
                }
                // Show GPT / chat models first
                ids.sort((a, b) -> {
                    boolean ag = a.startsWith("gpt"), bg = b.startsWith("gpt");
                    if (ag && !bg) return -1;
                    if (!ag && bg) return 1;
                    return a.compareTo(b);
                });
                return ids;
            }

            @Override
            protected void done() {
                openaiListBtn.setEnabled(true);
                openaiListBtn.setText("↻ List Models");
                try {
                    populateCombo(openaiModelCombo, get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                            "Failed to fetch models:\n" + ex.getCause().getMessage(),
                            "AI Chat", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Fetch locally available models from Ollama /api/tags */
    private void fetchOllamaModels() {
        String base = ollamaUrlField.getText().trim().replaceAll("/+$", "");

        ollamaListBtn.setEnabled(false);
        ollamaListBtn.setText("Loading...");

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(base + "/api/tags"))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> resp = HttpClient.newHttpClient()
                        .send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() != 200) {
                    throw new RuntimeException("Ollama error " + resp.statusCode());
                }

                JsonObject json   = JsonParser.parseString(resp.body()).getAsJsonObject();
                JsonArray  models = json.getAsJsonArray("models");

                List<String> names = new ArrayList<>();
                for (JsonElement el : models) {
                    names.add(el.getAsJsonObject().get("name").getAsString());
                }
                Collections.sort(names);
                return names;
            }

            @Override
            protected void done() {
                ollamaListBtn.setEnabled(true);
                ollamaListBtn.setText("↻ List Models");
                try {
                    List<String> models = get();
                    if (models.isEmpty()) {
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                                "No models found. Pull a model first:\n  ollama pull llama3",
                                "AI Chat", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        populateCombo(ollamaModelCombo, models);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                            "Cannot connect to Ollama.\nMake sure 'ollama serve' is running at:\n" + base,
                            "AI Chat", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Replace combo items, keeping the currently selected value if it exists in new list. */
    private static void populateCombo(JComboBox<String> combo, List<String> items) {
        String current = selectedModel(combo);
        combo.removeAllItems();
        for (String item : items) combo.addItem(item);
        if (current != null && !current.isBlank()) {
            // Re-select previous value, or keep it editable
            boolean found = false;
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (current.equals(combo.getItemAt(i))) { combo.setSelectedIndex(i); found = true; break; }
            }
            if (!found) combo.setSelectedItem(current);
        }
        JOptionPane.showMessageDialog(null,
                items.size() + " model(s) loaded.", "AI Chat", JOptionPane.INFORMATION_MESSAGE);
    }

    // ──────────────────────────────────────────────────────────────────
    // Persistence
    // ──────────────────────────────────────────────────────────────────
    public void savePreferences() {
        String sel = (String) providerCombo.getSelectedItem();
        if (sel != null) prefs.setString(PREF_PROVIDER, sel);

        prefs.setString(PREF_OPENAI_KEY,      new String(openaiKeyField.getPassword()).trim());
        prefs.setString(PREF_OPENAI_MODEL,    selectedModel(openaiModelCombo));
        prefs.setString(PREF_OPENAI_URL,      openaiUrlField.getText().trim());
        prefs.setString(PREF_ANTHROPIC_KEY,   new String(anthropicKeyField.getPassword()).trim());
        prefs.setString(PREF_ANTHROPIC_MODEL, selectedModel(anthropicModelCombo));
        prefs.setString(PREF_OLLAMA_URL,      ollamaUrlField.getText().trim());
        prefs.setString(PREF_OLLAMA_MODEL,    selectedModel(ollamaModelCombo));
        prefs.setString(PREF_MAX_TOKENS,      maxTokensField.getText().trim());
        prefs.setString(PREF_TEMPERATURE,     temperatureField.getText().trim());
        prefs.setString(PREF_SYSTEM_PROMPT,   systemPromptArea.getText().trim());

        JOptionPane.showMessageDialog(this, "Settings saved!", "AI Chat", JOptionPane.INFORMATION_MESSAGE);
        api.logging().logToOutput("[AI Chat] Settings saved.");
    }

    private void loadPreferences() {
        String provider = prefs.getString(PREF_PROVIDER);
        if (provider != null) { providerCombo.setSelectedItem(provider); cardLayout.show(providerCards, provider); }

        setPassword(openaiKeyField,    PREF_OPENAI_KEY,      "");
        setCombo(openaiModelCombo,     PREF_OPENAI_MODEL,    "gpt-4o");
        setText(openaiUrlField,        PREF_OPENAI_URL,      "https://api.openai.com/v1");
        setPassword(anthropicKeyField, PREF_ANTHROPIC_KEY,   "");
        setCombo(anthropicModelCombo,  PREF_ANTHROPIC_MODEL, "claude-opus-4-6");
        setText(ollamaUrlField,        PREF_OLLAMA_URL,      "http://localhost:11434");
        setCombo(ollamaModelCombo,     PREF_OLLAMA_MODEL,    "llama3");
        setText(maxTokensField,        PREF_MAX_TOKENS,      "4096");
        setText(temperatureField,      PREF_TEMPERATURE,     "0.7");

        String sp = prefs.getString(PREF_SYSTEM_PROMPT);
        systemPromptArea.setText(sp != null && !sp.isBlank() ? sp : AISettings.DEFAULT_SYSTEM_PROMPT);
    }

    public AISettings getCurrentSettings() {
        AISettings s = new AISettings();
        String sel = (String) providerCombo.getSelectedItem();
        if (sel != null) s.setProvider(sel);

        s.setOpenaiApiKey(new String(openaiKeyField.getPassword()).trim());
        s.setOpenaiModel(selectedModel(openaiModelCombo));
        s.setOpenaiBaseUrl(openaiUrlField.getText().trim());
        s.setAnthropicApiKey(new String(anthropicKeyField.getPassword()).trim());
        s.setAnthropicModel(selectedModel(anthropicModelCombo));
        s.setOllamaBaseUrl(ollamaUrlField.getText().trim());
        s.setOllamaModel(selectedModel(ollamaModelCombo));
        s.setSystemPrompt(systemPromptArea.getText().trim());

        try { s.setMaxTokens(Integer.parseInt(maxTokensField.getText().trim())); }
        catch (NumberFormatException ignored) {}
        try { s.setTemperature(Double.parseDouble(temperatureField.getText().trim())); }
        catch (NumberFormatException ignored) {}

        return s;
    }

    // ──────────────────────────────────────────────────────────────────
    // UI helper factories
    // ──────────────────────────────────────────────────────────────────
    private static JComboBox<String> editableCombo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setEditable(true);
        c.setPreferredSize(new Dimension(260, c.getPreferredSize().height));
        return c;
    }

    private static JButton listButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setToolTipText("Fetch available models from the API");
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private static void addLabelField(JPanel p, GridBagConstraints gc, int row,
                                      String label, JComponent field, String hint) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; gc.gridwidth = 1;
        p.add(new JLabel(label), gc);

        gc.gridx = 1; gc.weightx = 1; gc.gridwidth = 2;
        p.add(field, gc);

        gc.gridx = 3; gc.weightx = 0; gc.gridwidth = 1;
        p.add(hintLabel(hint), gc);
    }

    private static void addLabelComboButton(JPanel p, GridBagConstraints gc, int row,
                                            String label, JComboBox<String> combo,
                                            JButton btn, String hint) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; gc.gridwidth = 1;
        p.add(new JLabel(label), gc);

        gc.gridx = 1; gc.weightx = 1; gc.gridwidth = 1;
        p.add(combo, gc);

        gc.gridx = 2; gc.weightx = 0; gc.gridwidth = 1;
        if (btn != null) {
            p.add(btn, gc);
        } else {
            p.add(new JLabel(""), gc);
        }

        gc.gridx = 3; gc.weightx = 0; gc.gridwidth = 1;
        p.add(hintLabel(hint), gc);
    }

    private static JLabel hintLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Color.GRAY);
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC, 11f));
        return lbl;
    }

    private static JPanel titledPanel(String title) {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title,
                TitledBorder.LEFT, TitledBorder.TOP));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private static GridBagConstraints defaultGC() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(4, 6, 4, 6);
        gc.anchor  = GridBagConstraints.WEST;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        return gc;
    }

    /** Get typed / selected value from an editable JComboBox. */
    private static String selectedModel(JComboBox<String> combo) {
        Object val = combo.getEditor().getItem();
        return val != null ? val.toString().trim() : "";
    }

    private void setText(JTextField f, String key, String def) {
        String v = prefs.getString(key);
        f.setText(v != null ? v : def);
    }

    private void setPassword(JPasswordField f, String key, String def) {
        String v = prefs.getString(key);
        f.setText(v != null ? v : def);
    }

    private void setCombo(JComboBox<String> combo, String key, String def) {
        String v = prefs.getString(key);
        combo.setSelectedItem(v != null && !v.isBlank() ? v : def);
    }
}
