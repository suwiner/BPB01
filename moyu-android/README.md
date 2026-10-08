# 墨鱼浏览器 · Android 手机版

以墨鱼浏览器 PC 8.35.0 的品牌与交互诉求为参考，独立开发的原生 Android 轻量版，并非 Electron 运行时打包。

**功能**：主页搜索/Google 与百度、常用网站（可隐藏）、多标签浏览（最多 16 个）、本地书签、历史记录、文件下载、网页分享、深色模式、打开其他 App 分享来的链接、网页文件上传、视频全屏播放。

**安全边界**：默认禁止第三方 Cookie、混合内容、危险证书忽略、文件系统访问及网页主动索取的摄像头/定位权限。浏览内容由系统 Android WebView 呈现。

**限制**：暂不支持 PC 专用 Chromium 扩展/CRX、云同步、电商助手、复杂工具、桌面浏览器会话或 PC 密码库；未集成独立广告过滤引擎。网页下载通过系统 DownloadManager 完成。Android 8.0 起支持。部分网站可能受 WebView 兼容性限制。

构建命令：在已安装 Android SDK 的环境中运行 `gradle -p moyu-android :app:assembleDebug`。输出：`moyu-android/app/build/outputs/apk/debug/app-debug.apk`。

GitHub Actions 在 `moyu-android-mobile-v1` 分支每次修改此项目时自动构建 APK，构建完成后会在分支的 `moyu-android/release/` 目录发布可安装的调试签名版。
