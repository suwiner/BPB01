# 墨鱼浏览器 · Android V2.1.0

基于 Android 原生 Activity + 系统 WebView 构建。以墨鱼浏览器 PC 8.35 品牌元素为参考设计，遵循手机小屏交互习惯。

- 默认黑白极简主题，可切换深色黑白及柔和蓝白。
- 等比例矢量墨鱼鱼环标志，支持不同分辨率，无拉伸。
- 常用网站可新增、编辑、删除，首页每行不超过四个。工具不挤占首页。
- 多标签页、WebView 浏览、网页查找、电脑模式、保存 PDF、基础追踪域名拦截、图片及 JS 开关、文字比例、书签导入/导出（JSON/Netscape HTML）。
- 独立工具箱：电子便签、待办与非精确循环提醒、可编辑日报和周报、周报汇总、计算器、网页翻译、官方 AI 动态入口、八大类常用网站导航、Android 系统自动填充入口。
- 以本机 SharedPreferences 保存书签/任务/便签等信息；不自动与 Windows 同步，不存储网站账号密码。
- 提醒依赖系统通知权限与系统非精确 AlarmManager，省电模式可能延迟提醒；需手机实测。
- 本版仍未集成桌面 CRX 扩展、完整广告拦截、PC 账号云同步与原生新闻抓取。

构建：在 Android SDK 35 + Java 17 + Gradle 8.9 下执行 `gradle -p moyu-android :app:assembleDebug`。
GitHub Actions 工作流位于 `.github/workflows/moyu-android-apk.yml`，构建产物位于 `moyu-android/release/MoyuBrowser_Mobile_2.1.0.apk`。

注意：此 APK 为 GitHub Actions 调试签名构建。不同构建机的调试证书可能变化，无法直接覆盖旧签名 APK；卸载旧版将清除旧版本机数据。正式升级分发需创建并安全保存稳定的发布证书，不应将证书私钥提交到公开 Git 仓库。

## V2.1 重点修订
- 原版 PC 8.35 soft-logo.png 轮廓转为 Android 512×512 等比例矢量图，深色模式使用单独适配图标。
- 工具箱改为统一抗锯齿线框图标；新增网页主动弹窗的用户手势新标签处理。
- 修正 Netscape HTML 收藏夹文件解析；支持 JSON+HTML 导入和去重。
- 新增静态 Lint、APK 结构、包名/版本及签名校验；发布 GitHub 预发布版供直接安装。
- 未实现原生 Chromium 扩展、跨设备同步及完整视频去广告，相关功能不能用纯 WebView 等同替代。

**安装提示**：本版为云端 debug 签名包。签名可能与此前测试 APK 不一致，若提示无法覆盖安装，请先导出书签及重要数据，再卸载旧测试版后安装。待办与笔记暂无完整备份/迁移能力，卸载之前请手工保存。正式持续升级应采用保存在 GitHub Secrets 的固定发布签名密钥。
