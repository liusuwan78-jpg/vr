#!/bin/bash
# RA2Web 安卓版构建脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

cd "$PROJECT_DIR"

echo "=========================================="
echo "  RA2Web Android Build"
echo "=========================================="
echo ""

# 检查环境
echo "[1/4] 检查环境..."
if [ ! -d "$ANDROID_HOME" ]; then
    echo "错误: 找不到 Android SDK: $ANDROID_HOME"
    exit 1
fi

if ! java -version 2>&1 | grep -q "17"; then
    echo "错误: 需要 Java 17，当前版本:"
    java -version
    exit 1
fi

echo "  Java: $(java -version 2>&1 | head -1)"
echo "  Android SDK: $ANDROID_HOME"
echo ""

# 同步游戏资源
echo "[2/4] 同步游戏资源..."
if [ -d "$PROJECT_DIR/../ra2web" ]; then
    bash "$SCRIPT_DIR/sync-ra2web.sh"
else
    echo "  跳过 (未找到 ra2web 源码目录)"
fi
echo ""

# 构建 APK
echo "[3/4] 构建 Debug APK..."
cd "$PROJECT_DIR"
chmod +x ./gradlew 2>/dev/null || true

if [ -f "./gradlew" ]; then
    ./gradlew assembleDebug --no-daemon 2>&1
else
    echo "  没有 gradlew，使用系统 gradle..."
    gradle assembleDebug --no-daemon 2>&1
fi

echo ""

# 输出结果
echo "[4/4] 构建完成!"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo ""
    echo "  APK 路径: $APK_PATH"
    echo "  APK 大小: $SIZE"
    echo ""
else
    echo "  警告: 未找到生成的 APK 文件"
fi
