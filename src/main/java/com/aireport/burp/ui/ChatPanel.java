package com.aireport.burp.ui;

import burp.api.montoya.MontoyaApi;
import com.aireport.burp.ai.*;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main chat panel — "Chat" sub-tab.
 *
 *  - Renders AI replies as Markdown (via MarkdownRenderer → HTML)
 *  - Enter = Send,  Shift+Enter = newline
 *  - API key fields are masked (JPasswordField)
 */
public class ChatPanel extends JPanel {

    // ── Colours ────────────────────────────────────────────────────────
    private static final String HEX_USER      = "#1565C0";
    private static final String HEX_ASSISTANT = "#2E7D32";
    private static final String HEX_SYSTEM    = "#757575";
    private static final String HEX_ERROR     = "#C62828";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MontoyaApi    api;
    private final SettingsPanel settings;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Conversation history sent to AI on every turn. */
    private final List<ChatMessage> history = new ArrayList<>();

    /** Rendered HTML blocks — one per message bubble. */
    private final List<String> htmlBlocks = new ArrayList<>();

    // UI
    private JTextPane chatPane;
    private JTextArea inputArea;
    private JButton   sendButton;
    private JLabel    statusLabel;
    private JLabel    modelLabel;

    public ChatPanel(MontoyaApi api, SettingsPanel settings) {
        this.api      = api;
        this.settings = settings;
        setLayout(new BorderLayout(6, 6));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        buildUI();
        appendSystemMessage("Garux AI Chat ready. เลือก Provider ใน Settings แล้วเริ่มแชทได้เลย 🔥");
    }

    // ──────────────────────────────────────────────────────────────────
    // Build UI
    // ──────────────────────────────────────────────────────────────────
    private void buildUI() {
        // ── Toolbar ──────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JButton clearBtn = new JButton("Clear Chat");
        clearBtn.addActionListener(e -> clearChat());

        modelLabel = new JLabel("Provider: (not saved yet)");
        modelLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        modelLabel.setForeground(Color.GRAY);

        toolbar.add(clearBtn,   BorderLayout.WEST);
        toolbar.add(modelLabel, BorderLayout.EAST);

        // ── Chat pane (HTML) ──────────────────────────────────────────
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setContentType("text/html");

        // Set up global stylesheet
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = kit.getStyleSheet();
        css.addRule("body { font-family: 'SansSerif'; font-size: 13px; margin: 6px; background: #ffffff; }");
        css.addRule("pre  { background: #f5f5f5; padding: 8px; border-left: 3px solid #aaa;"
                  + "       font-family: monospace; font-size: 12px; white-space: pre-wrap; }");
        css.addRule("code { background: #f0f0f0; font-family: monospace; padding: 1px 4px; }");
        css.addRule("h2,h3,h4 { margin: 4px 0; }");
        css.addRule("ul,ol { margin: 2px 0; padding-left: 18px; }");
        css.addRule("li   { margin: 1px 0; }");
        css.addRule("p    { margin: 1px 0; }");
        css.addRule(".bubble { margin-bottom: 14px; border-left: 3px solid #ddd; padding-left: 8px; }");
        chatPane.setEditorKit(kit);
        chatPane.setText("<html><body></body></html>");

        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScroll.setBorder(BorderFactory.createLineBorder(new Color(0xDDDDDD)));

        // ── Status bar ────────────────────────────────────────────────
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);

        // ── Input area ────────────────────────────────────────────────
        inputArea = new JTextArea(4, 60);
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xBBBBBB)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        // Enter = Send,  Shift+Enter = newline
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume(); // ดัก event ทั้งคู่
                    if (e.isShiftDown()) {
                        // Shift+Enter → แทรก newline ณ ตำแหน่ง caret
                        int pos = inputArea.getCaretPosition();
                        inputArea.insert("\n", pos);
                    } else {
                        // Enter เฉยๆ → ส่ง
                        sendMessage();
                    }
                }
            }
        });

        sendButton = new JButton("Send  (Enter)");
        sendButton.setBackground(new Color(0x1976D2));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD));
        sendButton.addActionListener(e -> sendMessage());

        JButton clearInputBtn = new JButton("Clear");
        clearInputBtn.addActionListener(e -> inputArea.setText(""));

        JLabel shiftHint = new JLabel("Shift+Enter = newline");
        shiftHint.setForeground(Color.GRAY);
        shiftHint.setFont(shiftHint.getFont().deriveFont(Font.ITALIC, 11f));

        JPanel inputBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        inputBtns.setBackground(Color.WHITE);
        inputBtns.add(shiftHint);
        inputBtns.add(clearInputBtn);
        inputBtns.add(sendButton);

        JPanel inputRow = new JPanel(new BorderLayout(6, 4));
        inputRow.setBackground(Color.WHITE);
        inputRow.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        inputRow.add(inputBtns,                  BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout(4, 4));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(inputRow,    BorderLayout.CENTER);

        add(toolbar,     BorderLayout.NORTH);
        add(chatScroll,  BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ──────────────────────────────────────────────────────────────────
    // Send message
    // ──────────────────────────────────────────────────────────────────
    private void sendMessage() {
        String userText = inputArea.getText().trim();
        if (userText.isBlank()) return;

        inputArea.setText("");
        appendBubble("You", userText, HEX_USER, false);   // user text: no markdown
        history.add(new ChatMessage(ChatMessage.Role.user, userText));

        setSending(true);

        AISettings s = settings.getCurrentSettings();
        updateModelLabel(s);

        // Full conversation = system prompt + history
        List<ChatMessage> full = new ArrayList<>();
        full.add(new ChatMessage(ChatMessage.Role.system, s.getSystemPrompt()));
        full.addAll(history);

        executor.submit(() -> {
            try {
                AIProvider provider = AIProviderFactory.get(s.getProvider());
                String reply = provider.sendMessage(full, s);

                history.add(new ChatMessage(ChatMessage.Role.assistant, reply));

                SwingUtilities.invokeLater(() -> {
                    appendBubble(provider.getName(), reply, HEX_ASSISTANT, true);  // AI: render markdown
                    setSending(false);
                });
            } catch (Exception ex) {
                api.logging().logToError("[Garux AI Chat] " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    appendError(ex.getMessage());
                    setSending(false);
                });
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // Public API (used by ContextMenuProvider)
    // ──────────────────────────────────────────────────────────────────
    public void appendToInput(String text) {
        String cur = inputArea.getText();
        inputArea.setText(cur.isBlank() ? text : cur + "\n\n" + text);
        inputArea.requestFocus();
    }

    public void sendDirect(String prompt) {
        SwingUtilities.invokeLater(() -> {
            inputArea.setText(prompt);
            sendMessage();
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // Append helpers
    // ──────────────────────────────────────────────────────────────────
    private void appendBubble(String sender, String content, String color, boolean isMarkdown) {
        String time = LocalTime.now().format(TIME_FMT);
        String body = isMarkdown
                ? MarkdownRenderer.toHtml(content)
                : "<p style='margin:0'>" + MarkdownRenderer.escHtml(content) + "</p>";

        String block =
            "<div class='bubble'>" +
            "<span style='color:" + color + ";font-weight:bold;font-size:12px'>" +
            "[" + time + "] " + MarkdownRenderer.escHtml(sender) + ":</span><br>" +
            body +
            "</div>";

        htmlBlocks.add(block);
        rebuildChat();
    }

    private void appendSystemMessage(String text) {
        String block =
            "<div style='color:" + HEX_SYSTEM + ";font-style:italic;margin-bottom:10px'>" +
            "» " + MarkdownRenderer.escHtml(text) +
            "</div>";
        htmlBlocks.add(block);
        rebuildChat();
    }

    private void appendError(String text) {
        String block =
            "<div style='color:" + HEX_ERROR + ";font-weight:bold;margin-bottom:10px'>" +
            "✖ " + MarkdownRenderer.escHtml(text) +
            "</div>";
        htmlBlocks.add(block);
        rebuildChat();
    }

    /** Rebuild the full HTML document and push it to the JTextPane. */
    private void rebuildChat() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        for (String block : htmlBlocks) html.append(block);
        html.append("</body></html>");

        chatPane.setText(html.toString());
        // Scroll to bottom after Swing finishes rendering
        SwingUtilities.invokeLater(() ->
                chatPane.setCaretPosition(chatPane.getDocument().getLength()));
    }

    // ──────────────────────────────────────────────────────────────────
    // State helpers
    // ──────────────────────────────────────────────────────────────────
    private void clearChat() {
        htmlBlocks.clear();
        history.clear();
        chatPane.setText("<html><body></body></html>");
        appendSystemMessage("Chat cleared. พร้อมแล้ว 🔥");
    }

    private void setSending(boolean sending) {
        sendButton.setEnabled(!sending);
        inputArea.setEnabled(!sending);
        statusLabel.setText(sending ? "⏳ Waiting for AI response..." : " ");
    }

    private void updateModelLabel(AISettings s) {
        String model = switch (s.getProvider()) {
            case "anthropic" -> s.getAnthropicModel();
            case "ollama"    -> s.getOllamaModel() + " (local)";
            default          -> s.getOpenaiModel();
        };
        modelLabel.setText("Provider: " + AIProviderFactory.get(s.getProvider()).getName() + " / " + model);
    }
}
