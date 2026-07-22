#!/bin/bash
# 同步 RA2WEB 游戏资源到安卓 assets 目录

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
RA2WEB_SRC="${1:-$PROJECT_DIR/../ra2web}"
ASSETS_DIR="$PROJECT_DIR/app/src/main/assets/ra2web"

echo "同步 RA2WEB 资源..."
echo "  源目录: $RA2WEB_SRC"
echo "  目标目录: $ASSETS_DIR"

if [ ! -d "$RA2WEB_SRC" ]; then
    echo "错误: 找不到 RA2WEB 源码目录: $RA2WEB_SRC"
    exit 1
fi

# 创建目标目录
mkdir -p "$ASSETS_DIR"

# 复制顶层文件
cp "$RA2WEB_SRC/index.html" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/config.ini" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/servers.ini" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/mods.ini" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/style.css" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/manifest.webmanifest" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/favicon.ico" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/breaking-news.html" "$ASSETS_DIR/"
cp "$RA2WEB_SRC/ra2web-sw.js" "$ASSETS_DIR/" 2>/dev/null || true

# 复制 lib 目录
rm -rf "$ASSETS_DIR/lib"
cp -r "$RA2WEB_SRC/lib" "$ASSETS_DIR/"

# 复制 res 目录
rm -rf "$ASSETS_DIR/res"
cp -r "$RA2WEB_SRC/res" "$ASSETS_DIR/"

# 复制最新版本的 runtime
rm -rf "$ASSETS_DIR/runtime"
mkdir -p "$ASSETS_DIR/runtime/releases"
LATEST_RUNTIME=$(ls -d "$RA2WEB_SRC"/runtime/releases/0.83.* 2>/dev/null | sort -V | tail -1)
if [ -n "$LATEST_RUNTIME" ]; then
    cp -r "$LATEST_RUNTIME" "$ASSETS_DIR/runtime/releases/"
    echo "  Runtime: $(basename "$LATEST_RUNTIME")"
fi

# 复制最新版本的 assets
rm -rf "$ASSETS_DIR/assets"
mkdir -p "$ASSETS_DIR/assets/releases"
LATEST_ASSETS=$(ls -d "$RA2WEB_SRC"/assets/releases/0.83.* 2>/dev/null | sort -V | tail -1)
if [ -n "$LATEST_ASSETS" ]; then
    cp -r "$LATEST_ASSETS" "$ASSETS_DIR/assets/releases/"
    echo "  Assets: $(basename "$LATEST_ASSETS")"
fi

# 计算大小
SIZE=$(du -sh "$ASSETS_DIR" | cut -f1)
echo ""
echo "完成! 总大小: $SIZE"
