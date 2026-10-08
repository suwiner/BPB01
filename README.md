# 墨鱼浏览器官方网站 · V10.1

可直接放在 GitHub Pages 的静态网站程序。新版以「柔和、宽松、清晰」为核心视觉语言。

## 页面
- `index.html`：Windows / AI / 工具 / 工作空间 / 主题 / Android / 下载七个章节。首屏使用柔和原创 SVG 扁平插画与 Windows 8.32.0 的真实浏览器截图。
- `android.html`：Android 7.0.0 专页，单一手机主视觉、清晰下载入口、系统要求、SHA256 与安装说明。手机内界面为**示意图**，不是 Android 真机截图。
- `download.html`：Windows 安装版/便携版、Android APK、版本说明、校验清单与 FAQ。
- `styles.css`：统一配色、栅格、响应式布局和柔和动效。
- `app.js`：粒子动画、主视觉鼠标轻视差、AI/工具/工作空间/主题交互、移动端导航与 Windows GitHub Releases 文件核验。

## 版本与发布
- Windows 8.32.0：安装包尚未公开上架至 GitHub Releases；官网仅在确认对应正式发行附件存在且文件信息吻合时启用下载，避免失效链接。
- Android 7.0.0：随网站附带 `downloads/MoyuBrowser_7.0.0_Android.apk`，支持 Android 9+；不等同于 Windows 8.32.0。
- 下载文件完整性可使用 `downloads/SHA256SUMS_Android_7.0.0.txt` 和 `downloads/SHA256SUMS.txt` 校验。
- 已核验 APK 文件摘要、ZIP 完整性；Android 全部真机型号的兼容性尚未全面测试。

## 部署
把 `site/` 中的文件和目录完整发布在站点根目录（含 `.nojekyll`），使用相对路径，无需构建步骤。既有仓库 `suwiner/BPB01` 的 `gh-pages` 是网站分支，`main` 不受影响。

## 动效与无障碍
支持系统 `prefers-reduced-motion`、手动暂停粒子、键盘导航及响应式布局。SVG 插画不会模糊。