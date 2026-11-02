#!/usr/bin/env bash
# ==============================================================================
# FindIt - Build Script
# ==============================================================================
set -e

# Detect JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home"
    elif [ -d "/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home"
    fi
fi

echo "==> Using Java: $($JAVA_HOME/bin/java -version 2>&1 | head -n 1)"
mkdir -p backend/bin

echo "==> Compiling Java sources..."
$JAVA_HOME/bin/javac -d backend/bin $(find backend/src/main/java -name "*.java")

echo "==> Copying resources..."
cp backend/src/main/resources/application.properties backend/bin/

echo "==> Build completed successfully in backend/bin!"

