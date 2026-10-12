# 校园网自动认证 (Campus AutoAuth) — Android 后台静默认证工具

> **宣传语**：校园网自动认证，一次配置，告别反复登录。

一款专为高校校园网设计的轻量级 Android 自动认证工具。旨在通过一次初始账号录入与配置，在连接校园 Wi-Fi 遇到 Captive Portal 强制跳转时，由系统后台服务自动完成认证交互，减少日常重复手动弹窗与点击登录。

---

## ✨ 核心特性

- **无感静默认证**：捕获连通性探测跳转，仅在真正遇到校园网登录网关时发起认证，避免无谓请求。
- **本地硬件加密**：账号密码全程使用 Android Keystore 安全硬件隔离存储，不上传第三方服务器。
- **VPN / 代理共存**：针对高校学生常用的分流工具（如 Mihomo / Surfboard 等），遵循分流规则直接放行认证端点，无需断开代理。
- **超轻量体积**：纯原生轻量实现，APK 安装包仅约 33 KB，极低后台内存与电量消耗。

---

## ⚠️ 使用范围与客观边界

1. **高校网络适配**：本项目已适配南京医科大学（NJMU）等采用 Aruba / WebPortal 强制跳转的校园网络。各高校 Portal 认证协议各异，暂不承诺全网所有高校即插即用。
2. **系统保活边界**：受 Android 现代系统激进的后台电池优化策略影响，部分机型（如小米、华为、三星）需用户在系统设置中为本应用开启“无限制后台运行”或“自启动”白名单权限，无法承诺“永不被系统杀后台”。
3. **网络状态**：网络中断或欠费停机时无法凭空上网。

---

## 📦 下载与安装

- **官网直接下载**：[chessyuan.com/apps/campus-auth](https://chessyuan.com/apps/campus-auth)
- **GitHub Releases**：[Releases 页面](https://github.com/chessyuan/campus-auth/releases)
- **当前版本**：`v1.4`
- **支持系统**：Android 8.0 (API 26) 及以上
- **文件大小**：33,163 bytes (~32.4 KB)
- **SHA-256**：`F75B6935B844CCBFBBDD94C46C86484F49E223BAA60F9DB327E47C5412213D04`

---

## 📬 反馈与支持

如有适配需求或使用问题，欢迎前往官方网站或联系开发者：
- 官网：[chessyuan.com](https://chessyuan.com)
- 邮箱：`admin@chessyuan.com`
