package com.aireport.burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContextMenuProvider implements ContextMenuItemsProvider {

    private final MontoyaApi api;
    private final ChatPanel  chatPanel;

    public ContextMenuProvider(MontoyaApi api, ChatPanel chatPanel) {
        this.api       = api;
        this.chatPanel = chatPanel;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<HttpRequestResponse> items = event.selectedRequestResponses();
        if (items == null || items.isEmpty()) {
            var editorItem = event.messageEditorRequestResponse();
            if (editorItem.isEmpty()) return List.of();
            items = List.of(editorItem.get().requestResponse());
        }

        List<HttpRequestResponse> finalItems = items;
        List<Component> menu = new ArrayList<>();

        JMenu aiMenu = new JMenu("Garux AI Chat");

        // 1. Analyze Request
        JMenuItem analyzeReq = new JMenuItem("Analyze Request");
        analyzeReq.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() != null) {
                    sb.append("Analyze this HTTP request for security vulnerabilities:\n\n");
                    sb.append("```http\n").append(rr.request().toString()).append("\n```\n\n");
                }
            }
            if (!sb.isEmpty()) { chatPanel.sendDirect(sb.toString().trim()); focusChatTab(); }
        });

        // 2. Analyze Response
        JMenuItem analyzeResp = new JMenuItem("Analyze Response");
        analyzeResp.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (HttpRequestResponse rr : finalItems) {
                if (rr.response() != null) {
                    sb.append("Analyze this HTTP response for security issues, sensitive data exposure, and misconfigurations:\n\n");
                    sb.append("```http\n").append(rr.response().toString()).append("\n```\n\n");
                }
            }
            if (!sb.isEmpty()) { chatPanel.sendDirect(sb.toString().trim()); focusChatTab(); }
        });

        // 3. Analyze Request + Response
        JMenuItem analyzeBoth = new JMenuItem("Analyze Request + Response");
        analyzeBoth.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Analyze this HTTP request/response pair for security vulnerabilities (SQLi, XSS, IDOR, auth bypass, etc.):\n\n");
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() != null) {
                    sb.append("### Request:\n```http\n").append(rr.request().toString()).append("\n```\n\n");
                }
                if (rr.response() != null) {
                    String resp = rr.response().toString();
                    if (resp.length() > 8000) resp = resp.substring(0, 8000) + "\n... [truncated]";
                    sb.append("### Response:\n```http\n").append(resp).append("\n```\n\n");
                }
            }
            chatPanel.sendDirect(sb.toString().trim());
            focusChatTab();
        });

        // 4. Find Injection Points
        JMenuItem findInjection = new JMenuItem("Find Injection Points");
        findInjection.addActionListener(e -> {
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() == null) continue;
                chatPanel.sendDirect(
                    "List all injection points in this request. " +
                    "For each parameter, suggest the type of injection to test (SQLi, XSS, SSTI, XXE, SSRF, etc.) " +
                    "and provide a basic test payload:\n\n```http\n" + rr.request().toString() + "\n```"
                );
                focusChatTab();
                break;
            }
        });

        // 5. Generate curl Command
        JMenuItem genCurl = new JMenuItem("Generate curl Command");
        genCurl.addActionListener(e -> {
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() == null) continue;
                chatPanel.sendDirect(
                    "Convert this HTTP request to a curl command:\n\n```http\n" +
                    rr.request().toString() + "\n```"
                );
                focusChatTab();
                break;
            }
        });

        // 6. Send to Garux AI Chat Input (append, don't auto-send)
        JMenuItem appendToInput = new JMenuItem("Send to Garux AI Chat");
        appendToInput.addActionListener(e -> {
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() != null) {
                    chatPanel.appendToInput("```http\n" + rr.request().toString() + "\n```");
                }
            }
            focusChatTab();
        });

        aiMenu.add(analyzeReq);
        aiMenu.add(analyzeResp);
        aiMenu.add(analyzeBoth);
        aiMenu.addSeparator();
        aiMenu.add(findInjection);
        aiMenu.add(genCurl);
        aiMenu.addSeparator();
        aiMenu.add(appendToInput);

        menu.add(aiMenu);
        return menu;
    }

    private void focusChatTab() {
        api.logging().logToOutput("[Garux AI Chat] Request sent to chat panel.");
    }
}
