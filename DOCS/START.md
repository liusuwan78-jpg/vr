# 项目模板说明

此仓库为 VR Cardboard Player 的初始骨架。后续提交将包含：
- Assets/ (Unity 脚本、场景、视频播放器实现)
- ProjectSettings/ (Unity 项目设置，指定 Unity 2021.3 LTS)

快速开始（本地构建）
1. 安装 Unity 2021.3 LTS（建议使用 Unity Hub）。
2. 在本地 clone 本仓库：
   git clone https://github.com/liusuwan78-jpg/vr.git
3. 用 Unity Hub 打开项目（若尚未有完整项目，等待我推送完整 Assets）。
4. 在 Unity 中打开场景并通过 File > Build Settings 导出 Android APK。

使用 GitHub Actions 构建
- CI 工作流使用 game-ci/unity-builder，默认尝试构建 Android Debug APK 并上传为 artifact。
- 若要在 CI 自动构建成功，您可能需要在仓库 Secrets 中提供 Unity 激活信息，或在本地完成激活并将构建产物上载。

如何替换示例视频
- 代码中将包含一个 resources 或 StreamingAssets 文件夹，用于放置演示用本地视频文件。
- 将视频文件放入 Assets/StreamingAssets/（或 README 指定的位置），在应用中即可选择本地视频播放。

后续计划
- 推送完整的 Unity 项目（包含 Cardboard 支持、VideoPlayer、WebView 打开 B站、UI 与示例场景）。
- 生成一个 debug-signed APK 并发布到 Releases 供您侧载测试。

如需我继续推送完整 Unity 项目，请回复“继续推送 Unity 项目”。
