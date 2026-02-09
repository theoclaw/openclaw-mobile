# OpenClaw Mobile (Nana) Workspace

这份 workspace 是我们把 **BotDrop 开源项目** + **OpenClaw/NanaBot** + **三档订阅代理（DeepSeek/Kimi/Claude）** 整合成“老大妈可用”预装方案的落地资料库。

## 一句话入口
Route A（系统级预装 App）主链路在 Android repo：  
- App（fork BotDrop）：[howardleegeek/botdrop-android](https://github.com/howardleegeek/botdrop-android)（PR: [#1](https://github.com/howardleegeek/botdrop-android/pull/1)）
- Proxy（OpenAI-compatible）：[howardleegeek/openclaw-proxy](https://github.com/howardleegeek/openclaw-proxy)

## 核心文档
- 预装方案（面向 3 万台出厂流程）：`PREINSTALL_MOMS.md`
- 详细计划（域名/限额/订阅/风控/路线图）：`OPENCLAW_MOBILE_DETAILED_PLAN.md`
- BotDrop 对比与复用点：`BOTDROP_COMPARISON.md`

## 工具脚本
- Factory ADB provisioning：`factory_provision_adb.sh`
- Termux 兜底安装脚本（NanaBot）：`install-nanabot.sh`

## Backend（OpenAI-compatible Proxy）
Proxy 负责把 **device_token** 路由到三档模型（free→DeepSeek / pro→Kimi / max→Claude），对端侧暴露 `POST /v1/chat/completions`。
