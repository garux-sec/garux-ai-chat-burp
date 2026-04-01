package com.aireport.burp.ai;

/**
 * A single message in a conversation.
 * role: "system" | "user" | "assistant"
 */
public class ChatMessage {

    public enum Role { system, user, assistant }

    private final Role role;
    private final String content;

    public ChatMessage(Role role, String content) {
        this.role    = role;
        this.content = content;
    }

    public Role    getRole()    { return role; }
    public String  getContent() { return content; }

    public String roleString() {
        return role.name();
    }
}
