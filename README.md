# Garux AI Chat — Burp Suite Extension

> AI-powered security assistant inside Burp Suite — chat with OpenAI, Claude, or local Ollama models while pentesting.

![Burp Suite](https://img.shields.io/badge/Burp%20Suite-Extension-orange?logo=burpsuite)
![Java](https://img.shields.io/badge/Java-17%2B-blue?logo=java)
![License](https://img.shields.io/badge/License-MIT-green)

---

## Features

- 💬 **AI Chat tab** built directly into Burp Suite
- 🤖 **Multi-provider support**
  - OpenAI (GPT-4o, GPT-4, GPT-3.5, etc.)
  - Anthropic Claude (claude-opus, claude-sonnet, etc.)
  - Ollama — run models **locally / offline** (llama3, mistral, deepseek-r1, etc.)
- 🔍 **Right-click context menu** on any request/response
  - Analyze Request
  - Analyze Response
  - Analyze Request + Response
  - Find Injection Points
  - Generate curl command
- 🔑 **API key masking** — keys stored securely in Burp's preference store
- 📋 **Live model listing** — fetch available models directly from the API
- ✍️ **Markdown rendering** — AI responses render with proper formatting
- ⚡ **Enter to send**, Shift+Enter for newline

---

## Installation

### Requirements
- Burp Suite Pro or Community (2023.10+)
- Java 17+

### Download & Load
1. Download the latest `burp-garux-ai-chat-*.jar` from [Releases](https://github.com/garux-sec/garux-ai-chat-burp/releases)
2. Open Burp Suite → **Extensions** → **Installed** → **Add**
3. Extension type: **Java**
4. Select the downloaded `.jar` file
5. Click **Next** — the **Garux AI Chat** tab will appear

---

## Configuration

1. Go to **Garux AI Chat** → **⚙ Settings**
2. Select your AI Provider
3. Enter your API key
4. Click **↻ List Models** to fetch available models
5. Click **Save Settings**

### OpenAI / Compatible
- Get API key: https://platform.openai.com/api-keys
- Compatible with: Azure OpenAI, Groq, LM Studio, etc.

### Anthropic Claude
- Get API key: https://console.anthropic.com/

### Ollama (Free / Local)
```bash
# Install Ollama
brew install ollama        # macOS
# or: https://ollama.com

# Start server
ollama serve

# Pull a model
ollama pull llama3
ollama pull mistral
ollama pull deepseek-r1
```

---

## Build from Source

```bash
# Requirements: Java 17+, Maven 3.8+
git clone https://github.com/garux-sec/garux-ai-chat-burp.git
cd garux-ai-chat-burp
mvn clean package

# Output: target/burp-garux-ai-chat-1.0.0.jar
```

---

## Privacy Notice

> ⚠️ HTTP requests and responses you analyze will be sent to the AI provider you have configured (OpenAI, Anthropic, or your local Ollama instance).
> Please do not send sensitive or confidential data without proper authorization.
> Always ensure you are authorized to test the target system.

---

## License

MIT License — see [LICENSE](LICENSE)

---

## Contributing

Pull requests are welcome! Please open an issue first to discuss major changes.
