#!/usr/bin/env bash
# build_apk.sh - attempts to build the project locally using Gradle wrapper.
# Note: You need Java JDK 11+ and Android SDK command-line tools installed.
# Usage: ./build_apk.sh
set -e
echo "Ensure you have ANDROID_HOME or ANDROID_SDK_ROOT set and sdkmanager available."
echo "Running ./gradlew assembleDebug ..."
chmod +x ./gradlew || true
./gradlew assembleDebug
echo "If build succeeds, APK will be at app/build/outputs/apk/debug/app-debug.apk"
