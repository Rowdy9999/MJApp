#!/bin/bash
# build.sh — Build MJ APK
# Usage: ./build.sh [debug|release]
#
# Prerequisites:
#   - JDK 17+ installed
#   - Android SDK installed (set ANDROID_HOME or ANDROID_SDK_ROOT)
#   - Accept Android SDK licenses: yes | sdkmanager --licenses
#
# Quick setup (if SDK is missing):
#   export ANDROID_HOME=$HOME/Android/Sdk
#   sdkmanager "platforms;android-34" "build-tools;34.0.0"

set -e

BUILD_TYPE="${1:-debug}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "╔═══════════════════════════════════╗"
echo "║         MJ App Builder            ║"
echo "╠═══════════════════════════════════╣"
echo "║  Build: $BUILD_TYPE"
echo "╚═══════════════════════════════════╝"

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Install JDK 17+."
    exit 1
fi
echo "✓ Java: $(java -version 2>&1 | head -1)"

# Check Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  ANDROID_HOME not set. Trying common locations..."
    for dir in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "/opt/android-sdk" "/usr/local/android-sdk"; do
        if [ -d "$dir" ]; then
            export ANDROID_HOME="$dir"
            break
        fi
    done
fi

if [ -z "$ANDROID_HOME" ]; then
    echo "❌ Android SDK not found."
    echo "   Install Android Studio or set ANDROID_HOME."
    echo "   Then run: sdkmanager 'platforms;android-34' 'build-tools;34.0.0'"
    exit 1
fi
echo "✓ Android SDK: $ANDROID_HOME"

# Check if Gradle wrapper exists, if not generate it
if [ ! -f "gradlew" ]; then
    echo "→ Generating Gradle wrapper..."
    # Download gradle wrapper jar
    GRADLE_VERSION="8.5"
    WRAPPER_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

    # Create minimal gradlew
    cat > gradlew << 'GRADLEW'
#!/bin/sh
# Gradle wrapper script
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
GRADLEW
    chmod +x gradlew

    # Download the wrapper jar
    WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
    if [ ! -f "$WRAPPER_JAR" ]; then
        echo "→ Downloading Gradle wrapper jar..."
        curl -L -o "$WRAPPER_JAR" "https://github.com/nicovince/gradlew-bootstrap/releases/download/v0.0.1/gradle-wrapper.jar" 2>/dev/null || {
            echo "   Trying alternate method..."
            # Use gradle init if available
            if command -v gradle &> /dev/null; then
                gradle wrapper --gradle-version "$GRADLE_VERSION"
            else
                echo "❌ Cannot create Gradle wrapper."
                echo "   Install Gradle or Android Studio, then run this again."
                exit 1
            fi
        }
    fi
fi

echo "→ Building APK ($BUILD_TYPE)..."
./gradlew "assemble${BUILD_TYPE^}" --no-daemon

APK_PATH="app/build/outputs/apk/$BUILD_TYPE/app-${BUILD_TYPE}.apk"
if [ -f "$APK_PATH" ]; then
    SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo ""
    echo "✅ Build successful!"
    echo "📦 APK: $APK_PATH ($SIZE)"
    echo ""
    echo "Install on device:"
    echo "  adb install $APK_PATH"
else
    echo "❌ APK not found at expected path."
    echo "   Check build output for errors."
    find app/build/outputs -name "*.apk" 2>/dev/null
fi
