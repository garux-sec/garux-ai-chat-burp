package com.aireport.burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Injects "Garux AI Chat" directly into Burp's top-level right-click menu
 * (not under Extensions) by using an AWTEventListener to detect Burp's
 * JPopupMenu and inject items before it becomes visible.
 *
 * The Montoya ContextMenuItemsProvider is still registered so Burp passes us
 * the selected request/response; we store it and use it in the injected items.
 */
public class ContextMenuProvider implements ContextMenuItemsProvider {

    private final MontoyaApi api;
    private final ChatPanel  chatPanel;

    /** Last prepared menu — updated every time Burp calls provideMenuItems(). */
    private volatile JMenu lastMenu = null;

    public ContextMenuProvider(MontoyaApi api, ChatPanel chatPanel) {
        this.api       = api;
        this.chatPanel = chatPanel;
        registerAwtInjector();
    }

    // ──────────────────────────────────────────────────────────────────
    // Montoya API: called by Burp to build the Extensions sub-menu.
    // We still build the menu here (to capture request/response data),
    // save it, and return empty so nothing appears under Extensions.
    // ──────────────────────────────────────────────────────────────────
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<HttpRequestResponse> items = event.selectedRequestResponses();
        if (items == null || items.isEmpty()) {
            var editorItem = event.messageEditorRequestResponse();
            if (editorItem.isEmpty()) {
                lastMenu = null;
                return List.of();
            }
            items = List.of(editorItem.get().requestResponse());
        }

        lastMenu = buildMenu(items);
        return List.of(); // Don't show under Extensions
    }

    // ──────────────────────────────────────────────────────────────────
    // AWTEventListener: watches for Burp's JPopupMenu to appear,
    // then injects our menu as a top-level item.
    // ──────────────────────────────────────────────────────────────────
    private void registerAwtInjector() {
        AWTEventListener listener = awtEvent -> {
            if (!(awtEvent instanceof ComponentEvent ce)) return;
            if (ce.getID() != ComponentEvent.COMPONENT_SHOWN) return;
            if (!(ce.getComponent() instanceof JPopupMenu popup)) return;

            // Verify this is Burp's HTTP context menu by checking for "Send to Repeater"
            boolean isBurpContextMenu = false;
            for (Component c : popup.getComponents()) {
                if (c instanceof JMenuItem mi && mi.getText() != null
                        && mi.getText().contains("Send to Repeater")) {
                    isBurpContextMenu = true;
                    break;
                }
            }
            if (!isBurpContextMenu) return;

            JMenu menuToInject = lastMenu;
            if (menuToInject == null) return;

            // Inject on the EDT, right before the popup is painted
            SwingUtilities.invokeLater(() -> {
                // Avoid duplicates if listener fires twice
                for (Component c : popup.getComponents()) {
                    if (c instanceof JMenu m && "Garux AI Chat".equals(m.getText())) return;
                }
                popup.addSeparator();
                popup.add(menuToInject);
                popup.revalidate();
                popup.repaint();
            });
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(
                listener,
                AWTEvent.COMPONENT_EVENT_MASK
        );
    }

    // ──────────────────────────────────────────────────────────────────
    // Build the JMenu with all sub-items
    // ──────────────────────────────────────────────────────────────────
    private JMenu buildMenu(List<HttpRequestResponse> finalItems) {
        JMenu aiMenu = new JMenu("Garux AI Chat");

        // 1. Analyze Request
        JMenuItem analyzeReq = new JMenuItem("Analyze Request");
        analyzeReq.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() != null) {
                    sb.append("Analyze this HTTP request for security vulnerabilities:\n\n")
                      .append("```http\n").append(rr.request().toString()).append("\n```\n\n");
                }
            }
            if (!sb.isEmpty()) { chatPanel.sendDirect(sb.toString().trim()); log(); }
        });

        // 2. Analyze Response
        JMenuItem analyzeResp = new JMenuItem("Analyze Response");
        analyzeResp.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (HttpRequestResponse rr : finalItems) {
                if (rr.response() != null) {
                    sb.append("Analyze this HTTP response for security issues, sensitive data exposure, and misconfigurations:\n\n")
                      .append("```http\n").append(rr.response().toString()).append("\n```\n\n");
                }
            }
            if (!sb.isEmpty()) { chatPanel.sendDirect(sb.toString().trim()); log(); }
        });

        // 3. Analyze Request + Response
        JMenuItem analyzeBoth = new JMenuItem("Analyze Request + Response");
        analyzeBoth.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Analyze this HTTP request/response pair for security vulnerabilities (SQLi, XSS, IDOR, auth bypass, etc.):\n\n");
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() != null)
                    sb.append("### Request:\n```http\n").append(rr.request().toString()).append("\n```\n\n");
                if (rr.response() != null) {
                    String resp = rr.response().toString();
                    if (resp.length() > 8000) resp = resp.substring(0, 8000) + "\n... [truncated]";
                    sb.append("### Response:\n```http\n").append(resp).append("\n```\n\n");
                }
            }
            chatPanel.sendDirect(sb.toString().trim());
            log();
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
                log(); break;
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
                log(); break;
            }
        });

        // 6. Send to Garux AI Chat Input
        JMenuItem appendToInput = new JMenuItem("Send to Garux AI Chat");
        appendToInput.addActionListener(e -> {
            for (HttpRequestResponse rr : finalItems) {
                if (rr.request() != null)
                    chatPanel.appendToInput("```http\n" + rr.request().toString() + "\n```");
            }
            log();
        });

        aiMenu.add(analyzeReq);
        aiMenu.add(analyzeResp);
        aiMenu.add(analyzeBoth);
        aiMenu.addSeparator();
        aiMenu.add(findInjection);
        aiMenu.add(genCurl);
        aiMenu.addSeparator();
        aiMenu.add(appendToInput);

        return aiMenu;
    }

    private void log() {
        api.logging().logToOutput("[Garux AI Chat] Request sent to chat panel.");
    }
}
