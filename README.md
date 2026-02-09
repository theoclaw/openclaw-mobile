# OpenClaw Mobile (Nana) Monorepo

目标：把 **系统级预装 Android App（Route A）** + **OpenAI-compatible Proxy（DeepSeek/Kimi/Claude 三档）** + **出厂/运维脚本与文档** 放在一个仓库里。

## 目录
- `android/botdrop-android/`：Android 预装 App（基于 BotDrop fork，含 Route A 预装 + 内置语音聊天 UI）
- `proxy/`：OpenAI-compatible Proxy（按 device_token tier 路由 DeepSeek/Kimi/Claude，含限额骨架）
- `PREINSTALL_MOMS.md`：3 万台出厂预装方案（老大妈用户）
- `OPENCLAW_MOBILE_DETAILED_PLAN.md`：详细计划（域名/限额/订阅/风控/路线图）
- `factory_provision_adb.sh`：出厂 ADB provisioning 模板

## 快速验证（本机）

### Proxy（无上游 key 也能跑通链路）
```bash
cd proxy
MOCK_MODE=1 ADMIN_KEY=devadmin LISTEN_PORT=8080 ./run_dev.sh
./smoke_test.sh http://127.0.0.1:8080
```

### Android（需要 JDK + Android SDK）
```bash
cd android/botdrop-android
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Legacy Mirrors（可忽略）
历史上曾拆成多个 repo；现在已经全部整合进本仓库。旧 repo 仅保留做兼容指向：
- https://github.com/howardleegeek/botdrop-android
- https://github.com/howardleegeek/openclaw-proxy
