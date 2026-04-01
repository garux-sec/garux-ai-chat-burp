package com.aireport.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.EditorOptions;
import com.aireport.burp.ui.ChatPanel;
import com.aireport.burp.ui.SettingsPanel;
import com.aireport.burp.ui.ContextMenuProvider;

import javax.swing.*;
import java.awt.*;

/**
 * Garux AI Chat — Burp Suite AI Assistant Extension
 *
 * Adds an AI chatbot tab to Burp Suite that can:
 *  - Chat with OpenAI / Anthropic Claude / Ollama
 *  - Analyze selected HTTP requests & responses
 *  - Answer web security / pentest questions in context
 *
 * Load this jar via Burp > Extensions > Add
 */
public class AIChatExtension implements BurpExtension {

    static final String NAME = "Garux AI Chat";

    private MontoyaApi  api;
    private SettingsPanel settingsPanel;
    private ChatPanel     chatPanel;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName(NAME);

        settingsPanel = new SettingsPanel(api);
        chatPanel     = new ChatPanel(api, settingsPanel);

        api.userInterface().registerSuiteTab(NAME, buildMainTab());

        api.userInterface().registerContextMenuItemsProvider(
                new ContextMenuProvider(api, chatPanel)
        );

        api.logging().logToOutput("[" + NAME + "] Extension loaded successfully.");
    }

    private JPanel buildMainTab() {
        JPanel root = new JPanel(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("💬 Chat",     chatPanel);
        tabs.addTab("⚙ Settings", settingsPanel);

        root.add(tabs, BorderLayout.CENTER);
        return root;
    }
}
