# 墨鱼浏览器官方网站 V8.0 · Motion Atlas

实际部署的静态网站程序；不是图片或展示稿。

## 页面
- `index.html`：七章节高对比排版、四组 SVG 矢量插画、按章节控制的轻量粒子动画、Android 独立模块、AI/工具/工作流/主题交互。
- `download.html`：Windows 8.32.0 下载校验、Android 7.0.0 安装入口、安装步骤、问答。
- `android.html`：Android 手机版介绍、原生 APK 下载与 SHA256 校验。Android 手机图是示意性界面，不是设备截图。

## 动画与性能
- `app.js` 中的 HTML Canvas 粒子会在可见时才渲染；绘制区域被限制在视觉模块内部，不穿过正文。
- 减弱动画无障碍偏好以及“暂停动效”按钮可停止循环效果。
- SVG 插画为矢量格式，放大没有位图背景模糊。

## 文件
- Android: `MoyuBrowser_7.0.0_Android.apk`，955930 字节，SHA256 `51c2253e9856332a7348174ba64e20f88f4ef99bd90a4930856bd291294cc7a1`。归档校验与 JAR 签名检查通过。尚未完成 Android 真机验证和正式应用商店发布。
- Windows 8.32.0 安装版及便携版没有包含在此源码包。下载按钮仅在 GitHub Releases 中校验到正确的正式附件时启用。

## 部署
把 `site/` 全部文件发布到 HTTPS 静态站点根目录即可。GitHub Pages 部署至 `gh-pages` 根目录，不会修改仓库主分支。

## 说明
Android 7.0.0 与 Windows 8.32.0 是不同版本；发布站点不会假称两者的功能完全一致。