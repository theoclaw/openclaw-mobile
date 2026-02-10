# ClawPhones 快速上手

## 后端已部署

测试服务器已在线（mock 模式，不调真实 LLM）：

```
http://3.142.69.6:8080
```

测试 token：`ocw1_DglF-k9sbOrOkqYMws_Bz0mx-jEMtH0D`

验证：`curl http://3.142.69.6:8080/health`

---

## 装 Android

需要：Android Studio + 手机开 USB 调试

```bash
git clone https://github.com/howardleegeek/openclaw-mobile.git
cd openclaw-mobile/android/clawphones-android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

打开 app → 输入后端地址 `http://3.142.69.6:8080` → 输入 token → 开聊

---

## 装 iOS

需要：Mac + Xcode 15+ + iPhone (iOS 17+)

```bash
git clone https://github.com/howardleegeek/openclaw-mobile.git
```

1. 用 Xcode 打开 `ios/ClawPhones.xcodeproj`
2. 点 ClawPhones target → Signing & Capabilities → 选你的 Team
3. 连 iPhone → 点 Run (▶)
4. 手机上信任开发者：设置 → 通用 → VPN与设备管理 → 信任

打开 app → 输入后端地址 `http://3.142.69.6:8080` → 输入 token → 开聊
