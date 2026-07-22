# RA2Web 安卓版

基于开源网页版红色警戒（RA2WEB / 网页红井）的安卓原生 App 移植。
**专为 Android 13/14/15 新版安卓优化适配**

## 功能特性

### 核心功能
- 🎮 **完整游戏**：原汁原味的红色警戒2网页版
- 📱 **触屏优化**：双指平移/缩放、长按右键、滑动框选
- 📶 **联机对战**：连接官方服务器，与全球玩家对战
- 💾 **离线运行**：首次下载资源后，单机模式完全离线
- 🔔 **震动反馈**：选中/建造/受击震动提示
- 🌙 **全屏沉浸**：沉浸式全屏，刘海屏适配

### 新版安卓适配
- ✅ **Android 15 (API 35)**：targetSdk 升级到 35
- ✅ **边对边显示**：Edge-to-Edge 全面屏体验
- ✅ **Android 13+ 通知权限**：POST_NOTIFICATIONS 运行时权限
- ✅ **Android 13+ 细分存储权限**：READ_MEDIA_IMAGES / READ_MEDIA_AUDIO
- ✅ **Android 14+ 预测性返回**：OnBackPressedDispatcher 手势导航
- ✅ **Android 14+ 前台服务类型**：dataSync 前台服务下载资源
- ✅ **Android 12+ 启动画面**：SplashScreen API 启动动画
- ✅ **核心库 Desugaring**：兼容旧版安卓的新 API
- ✅ **沉浸式手势导航**：短暂显示后自动隐藏系统栏
- ✅ **刘海/挖孔屏适配**：LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

## 系统要求

- **最低版本**：Android 7.0 (API 24)
- **推荐版本**：Android 13+ (API 33)
- **最佳体验**：Android 14/15
- **存储空间**：约 2GB（游戏资源）
- **网络**：首次启动需要下载资源，联机需要联网

## 构建方法

### 环境要求

- Java 17+
- Android SDK (API 35)
- Gradle 8.5+

### 构建步骤

```bash
# 1. 同步游戏资源
bash scripts/sync-ra2web.sh /path/to/ra2web

# 2. 构建 Debug APK
bash scripts/build-apk.sh

# 3. 或者直接用 Gradle
gradle assembleDebug
```

构建产物位于：`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
ra2web-android/
├── app/src/main/
│   ├── java/com/ra2web/app/
│   │   ├── MainActivity.kt              # 主 Activity
│   │   ├── TouchGestureHandler.kt       # 触屏手势识别
│   │   ├── JsBridge.kt                  # JS 与原生互调
│   │   ├── ResourceManager.kt           # 资源下载管理
│   │   ├── ResourceDownloadService.kt   # 前台下载服务
│   │   ├── VibrationHelper.kt           # 震动反馈
│   │   └── DisplayHelper.kt             # 显示适配
│   ├── assets/ra2web/                   # 游戏引擎资源
│   ├── res/                             # 安卓资源
│   │   ├── xml/network_security_config.xml
│   │   └── values/themes.xml
│   └── AndroidManifest.xml
├── scripts/
│   ├── sync-ra2web.sh                   # 同步游戏资源
│   └── build-apk.sh                     # 构建脚本
└── README.md
```

## 操作说明

| 操作 | 手势 |
|---|---|
| 选中单位/建筑 | 单指点击 |
| 选中同类单位 | 单指双击 |
| 取消/右键功能 | 单指长按 |
| 框选单位 | 单指滑动 |
| 平移地图 | 双指拖动 |
| 缩放视野 | 双指捏合 |

## 权限说明

| 权限 | 用途 | 必要性 |
|---|---|---|
| INTERNET | 下载游戏资源、联机对战 | 必需 |
| ACCESS_NETWORK_STATE | 检测网络状态 | 必需 |
| VIBRATE | 操作震动反馈 | 推荐 |
| WAKE_LOCK | 游戏时保持屏幕常亮 | 推荐 |
| POST_NOTIFICATIONS | 显示下载进度通知 (Android 13+) | 可选 |
| FOREGROUND_SERVICE_DATA_SYNC | 前台服务下载资源 | 必需 |

## 许可声明

- 本项目基于 RA2WEB / 网页红井（开源项目）进行安卓移植
- 仅供个人学习和研究使用，严禁商业用途
- 游戏美术资源版权归 EA 公司所有
- 移植版本保留 "RA2WEB"、"网页红井" 等名称标识
