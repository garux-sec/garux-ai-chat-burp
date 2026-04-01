package com.aireport.burp.ai;

import java.util.List;

/**
 * Common interface for all AI backends.
 * Each provider implements sendMessage() which takes a conversation
 * history and returns the assistant's next reply.
 */
public interface AIProvider {

    /** Human-readable name shown in the Settings dropdown. */
    String getName();

    /**
     * Send the conversation and get the next assistant reply.
     *
     * @param messages  List of {role, content} maps (system / user / assistant)
     * @param settings  Current extension settings (API key, model, etc.)
     * @return          Assistant reply text
     * @throws Exception on network or API errors
     */
    String sendMessage(List<ChatMessage> messages, AISettings settings) throws Exception;
}
