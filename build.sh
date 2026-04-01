#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────
#  Build script for Burp AI Chat Extension
#  Requires: Java 17+, Maven 3.8+
#
#  Install Maven (macOS): brew install maven
#  Install Maven (Ubuntu): sudo apt install maven
# ─────────────────────────────────────────────────────────────────────

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "╔══════════════════════════════════════════════╗"
echo "║     Building Burp AI Chat Extension          ║"
echo "╚══════════════════════════════════════════════╝"

# Check Java version
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
  echo "ERROR: Java 17 or higher is required (found: $JAVA_VER)"
  exit 1
fi
echo "✓ Java $JAVA_VER detected"

# Check Maven
if ! command -v mvn &> /dev/null; then
  echo ""
  echo "Maven not found. Install it first:"
  echo "  macOS:  brew install maven"
  echo "  Ubuntu: sudo apt install maven"
  echo "  Windows: https://maven.apache.org/download.cgi"
  exit 1
fi
echo "✓ Maven $(mvn --version | head -1 | awk '{print $3}') detected"

echo ""
echo "▶ Building fat jar..."
mvn clean package -q

JAR_PATH="$SCRIPT_DIR/target/burp-ai-chat-1.0.0.jar"
if [ -f "$JAR_PATH" ]; then
  echo ""
  echo "╔══════════════════════════════════════════════╗"
  echo "║  Build SUCCESS!                              ║"
  echo "╚══════════════════════════════════════════════╝"
  echo ""
  echo "Output jar:  $JAR_PATH"
  echo ""
  echo "How to load in Burp Suite:"
  echo "  1. Open Burp Suite"
  echo "  2. Go to Extensions > Installed > Add"
  echo "  3. Extension Type: Java"
  echo "  4. Select: $JAR_PATH"
  echo "  5. Click Next — the 'AI Chat' tab should appear"
else
  echo "Build FAILED. Check output above."
  exit 1
fi
