# VR Cardboard Player

这是一个基于 Unity 的 Android Cardboard（手机+眼镜框）VR 播放器项目骨架。目标是生成一个可侧载（APK）的演示应用，支持本地视频播放、HLS URL 输入和通过内置 WebView 打开 B 站页面作为演示。

重要说明
- 本项目仅用于测试/演示用途。若要在中国大陆公开发布，您需确保拥有视频内容的版权与必要的备案/资质。该项目不会包含任何绕过网络审查或教唆翻墙的功能。
- CI 构建需要在 Secrets 中配置 Unity 激活所需信息（参见下文），也可以在本地使用 Unity 编辑器手动构建 APK。

内容与交付物
- Unity 项目骨架（后续提交将加入实际的 Assets/ 脚本和示例场景）
- GitHub Actions 工作流：示例 Unity Android 构建（debug）并上传构建产物
- README：如何侧载 APK、如何在本地构建以及如何替换示例视频文件

下一步
1. 我会在接下来的提交中推送实际的 Unity 项目文件（Assets、ProjectSettings、示例场景与脚本）。
2. CI 工作流需要配置 Unity 激活信息（或您可在本地构建）。如果您希望由 CI 自动签名并发布 Release，请通过安全方式提供签名 keystore（不在公开聊天中发送）。

如果你需要我现在就把 Unity 项目文件推上来，请回复“继续推送 Unity 项目”。
