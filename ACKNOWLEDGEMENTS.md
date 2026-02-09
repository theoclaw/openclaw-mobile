# 致敬（Acknowledgements）

OpenClaw Mobile（Nana）是一次面向真实用户场景的工程化落地：我们在开源社区已验证的基础设施之上做“产品级预装 + 傻瓜式体验”。我们会尽量保持上游项目的设计意图，在条件允许时回馈改进，并严格保留许可证与署名信息。

## 我们学习并二次开发的上游项目
- BotDrop Android（upstream）：https://github.com/zhixianio/botdrop-android  
  本仓库的 Route A 预装链路（Termux bootstrap、安装器脚本驱动、前台服务保活等）来自对 BotDrop 的 fork 与扩展：加入了 device_token 出厂预置、云端代理三档路由、以及更“语音优先”的交互闭环。（GPLv3）
- Termux：https://github.com/termux/termux-app  
  提供 Android 上稳定的 userland/runtime 基础，使得在普通手机上运行网关与 agent 栈成为可能。

## 基础设施与依赖库
- FastAPI：https://github.com/fastapi/fastapi（proxy API server）
- Uvicorn：https://github.com/encode/uvicorn（ASGI server）
- httpx：https://github.com/encode/httpx（HTTP client）
- Python websockets 生态（WebSocket client/server 的基础构件）

## 许可证说明
本 monorepo 以 **GPLv3** 发布（见根目录 `LICENSE`）。`android/botdrop-android/` 子目录也包含其上游许可文件与声明，本仓库的分发与二次开发必须保持 GPL 兼容。
