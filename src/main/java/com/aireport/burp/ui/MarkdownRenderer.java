package com.aireport.burp.ui;

/**
 * Lightweight Markdown → HTML converter for Swing JTextPane (HTMLEditorKit).
 *
 * Supports:
 *  - Fenced code blocks  (``` ... ```)
 *  - Inline code          (`code`)
 *  - Headings             (# ## ###)
 *  - Bold / Italic        (** * ***)
 *  - Bullet lists         (- or *)
 *  - Numbered lists       (1. 2. ...)
 *  - Horizontal rule      (---)
 *  - Blank lines          → paragraph break
 */
public class MarkdownRenderer {

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";

        StringBuilder out = new StringBuilder();

        // ── Step 1: split on fenced code blocks (``` ... ```) ──────────
        String[] fenceParts = markdown.split("```", -1);

        for (int i = 0; i < fenceParts.length; i++) {
            if (i % 2 == 1) {
                // Inside a code block
                String block = fenceParts[i];
                // Strip optional language tag on first line (e.g. "java\n...")
                int nl = block.indexOf('\n');
                if (nl >= 0) {
                    String lang = block.substring(0, nl).trim();
                    block = block.substring(nl + 1);
                    out.append("<pre style='background:#f5f5f5;padding:8px;border-left:3px solid #ccc;"
                             + "font-family:monospace;font-size:12px;white-space:pre-wrap'>");
                    if (!lang.isEmpty()) {
                        out.append("<span style='color:#888;font-size:10px'>").append(escHtml(lang)).append("</span><br>");
                    }
                    out.append(escHtml(block)).append("</pre>");
                } else {
                    out.append("<pre style='background:#f5f5f5;padding:8px;border-left:3px solid #ccc;"
                             + "font-family:monospace;font-size:12px;white-space:pre-wrap'>")
                       .append(escHtml(block)).append("</pre>");
                }
            } else {
                // Normal text — process line by line
                out.append(renderLines(fenceParts[i]));
            }
        }

        return out.toString();
    }

    // ──────────────────────────────────────────────────────────────────
    // Process normal (non-fenced) text line by line
    // ──────────────────────────────────────────────────────────────────
    private static String renderLines(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb  = new StringBuilder();
        boolean inUl      = false;
        boolean inOl      = false;

        for (String raw : lines) {
            String line = raw;  // keep original for regex

            // ── Horizontal rule ──
            if (line.matches("^(-{3,}|\\*{3,}|_{3,})\\s*$")) {
                sb = closeLists(sb, inUl, inOl);
                inUl = inOl = false;
                sb.append("<hr>");
                continue;
            }

            // ── Headings ──
            if (line.startsWith("### ")) {
                sb = closeLists(sb, inUl, inOl); inUl = inOl = false;
                sb.append("<h4 style='margin:6px 0;color:#333'>").append(inline(line.substring(4))).append("</h4>");
                continue;
            }
            if (line.startsWith("## ")) {
                sb = closeLists(sb, inUl, inOl); inUl = inOl = false;
                sb.append("<h3 style='margin:6px 0;color:#222'>").append(inline(line.substring(3))).append("</h3>");
                continue;
            }
            if (line.startsWith("# ")) {
                sb = closeLists(sb, inUl, inOl); inUl = inOl = false;
                sb.append("<h2 style='margin:6px 0;color:#111'>").append(inline(line.substring(2))).append("</h2>");
                continue;
            }

            // ── Bullet list ──
            if (line.matches("^[*\\-] .+")) {
                if (inOl) { sb.append("</ol>"); inOl = false; }
                if (!inUl) { sb.append("<ul style='margin:2px 0;padding-left:20px'>"); inUl = true; }
                sb.append("<li>").append(inline(line.substring(2))).append("</li>");
                continue;
            }

            // ── Numbered list ──
            if (line.matches("^\\d+[.)].+")) {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (!inOl) { sb.append("<ol style='margin:2px 0;padding-left:20px'>"); inOl = true; }
                String content = line.replaceFirst("^\\d+[.)] ?", "");
                sb.append("<li>").append(inline(content)).append("</li>");
                continue;
            }

            // ── Blank line ──
            if (line.trim().isEmpty()) {
                sb = closeLists(sb, inUl, inOl); inUl = inOl = false;
                sb.append("<br>");
                continue;
            }

            // ── Regular paragraph line ──
            sb = closeLists(sb, inUl, inOl); inUl = inOl = false;
            sb.append("<p style='margin:1px 0'>").append(inline(line)).append("</p>");
        }

        closeLists(sb, inUl, inOl);
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────
    // Inline formatting: bold, italic, inline code
    // ──────────────────────────────────────────────────────────────────
    private static String inline(String text) {
        // Split on backtick to protect inline code from other replacements
        String[] parts = text.split("`", -1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 1) {
                // Inline code
                sb.append("<code style='background:#f0f0f0;padding:1px 4px;border-radius:2px;"
                        + "font-family:monospace'>")
                  .append(escHtml(parts[i]))
                  .append("</code>");
            } else {
                // Normal text: HTML-escape first, then apply bold/italic
                String s = escHtml(parts[i]);
                // Bold+Italic ***
                s = s.replaceAll("\\*\\*\\*(.+?)\\*\\*\\*", "<b><i>$1</i></b>");
                // Bold **
                s = s.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
                // Italic *
                s = s.replaceAll("\\*(.+?)\\*", "<i>$1</i>");
                // Strikethrough ~~
                s = s.replaceAll("~~(.+?)~~", "<s>$1</s>");
                sb.append(s);
            }
        }
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────
    private static StringBuilder closeLists(StringBuilder sb, boolean inUl, boolean inOl) {
        if (inUl) sb.append("</ul>");
        if (inOl) sb.append("</ol>");
        return sb;
    }

    /** HTML-escape special characters (but NOT inside already-converted HTML). */
    public static String escHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
