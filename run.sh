#!/usr/bin/env bash
# ==============================================================================
# FindIt - Server Run Script
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

if [ ! -d "backend/bin/com/findit" ]; then
    echo "==> Binaries not found. Running build..."
    ./build.sh
fi

echo "==> Starting FindIt Web Application Server on http://localhost:8080 ..."
exec $JAVA_HOME/bin/java -cp backend/bin com.findit.Application

