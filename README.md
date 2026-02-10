# ClawPhones

**On-device AI assistant for [Oyster Labs](https://oysterecosystem.com) hardware.**

ClawPhones ships pre-installed on the Universal Phone, Puffy, and ClawGlasses. It provides a unified voice + text chat interface backed by a tiered LLM routing system that transparently selects the best model for each user's subscription level.

- **30,000+ devices** deployed across DePIN network
- **3 LLM tiers** — DeepSeek (Free), Kimi (Pro), Claude (Max)
- **Server-side conversation persistence** — full chat history synced across sessions
- **OpenAI-compatible API** — standard `/v1/chat/completions` protocol

## Architecture

```
┌─────────────┐  ┌──────────────┐
│  iOS App    │  │  Android App │
│  (SwiftUI)  │  │  (Java)      │
└──────┬──────┘  └──────┬───────┘
       │                │
       └───────┬────────┘
               ▼
     ┌─────────────────┐
     │  OpenClaw Proxy  │    FastAPI + aiosqlite
     │  api.openclaw.ai │    Token auth (ocw1_*)
     └────────┬────────┘
              │
     ┌────────┼────────┐
     ▼        ▼        ▼
 DeepSeek   Kimi    Claude
  (Free)    (Pro)    (Max)
```

## Repository Layout

```
proxy/                          FastAPI backend (~840 lines)
├── server.py                   LLM routing, conversation CRUD, token auth
├── requirements.txt            Python dependencies
└── data/                       SQLite database (auto-created)

ios/                            SwiftUI iOS client
├── ClawPhones.xcodeproj
├── ClawPhones/
│   ├── App/                    Entry point, scene delegate
│   ├── Models/                 Codable data models
│   ├── Services/               API client, Keychain, device config
│   ├── ViewModels/             Chat & conversation state
│   └── Views/                  SwiftUI screens
└── ClawPhonesTests/

android/clawphones-android/     Android client (Termux-based)
├── app/src/main/java/ai/clawphones/agent/
│   ├── ChatActivity.java       Voice-first chat UI with TTS/STT
│   ├── ConversationApiClient.java  HTTP API client (5 endpoints)
│   ├── ClawPhonesConfig.java   Device config & auth profiles
│   ├── PreinstallProvisioner.java  Factory OEM provisioning
│   └── UpdateChecker.java      OTA version checking
└── termux-shared/              Terminal emulation layer
```

## API Reference

**Base URL:** `https://api.openclaw.ai`
**Auth:** `Authorization: Bearer ocw1_<token>`

### LLM Chat (OpenAI-compatible)

```
POST /v1/chat/completions
```

Requests are routed to a backend model based on the token's tier:

| Tier | Model | Best For |
|------|-------|----------|
| Free | DeepSeek | Daily chat, cost-efficient |
| Pro | Kimi | Long-context reasoning |
| Max | Claude | Highest quality |

### Conversation Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/conversations` | Create conversation |
| `GET` | `/v1/conversations` | List conversations (paginated) |
| `GET` | `/v1/conversations/{id}` | Get conversation with messages |
| `POST` | `/v1/conversations/{id}/chat` | Send message & get response |
| `DELETE` | `/v1/conversations/{id}` | Delete conversation |

### Admin

```
POST /admin/generate-token    Generate device tokens (requires admin key)
GET  /health                  Health check
```

## Getting Started

### Proxy (local development)

```bash
cd proxy
pip install -r requirements.txt

# Run in mock mode (no real LLM calls)
MOCK_MODE=1 ADMIN_KEY=devadmin LISTEN_PORT=8080 python server.py

# Verify
curl http://127.0.0.1:8080/health
```

### iOS

Requires Xcode 15+ and iOS 17 SDK.

```bash
cd ios
open ClawPhones.xcodeproj
# Or build from CLI:
xcodebuild -scheme ClawPhones -sdk iphonesimulator build
```

### Android

Requires JDK 17 and Android SDK 34.

```bash
cd android/clawphones-android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Configuration

| Environment Variable | Required | Description |
|---------------------|----------|-------------|
| `ANTHROPIC_API_KEY` | Yes (prod) | Claude API key for Max tier |
| `DEEPSEEK_API_KEY` | Yes (prod) | DeepSeek API key for Free tier |
| `KIMI_API_KEY` | Yes (prod) | Kimi API key for Pro tier |
| `ADMIN_KEY` | Yes | Admin key for token generation |
| `LISTEN_PORT` | No | Server port (default: 8000) |
| `MOCK_MODE` | No | Set to `1` for development without real LLM calls |

## Brand

**ClawPhones** — [clawphones.com](https://clawphones.com)

Part of the [Oyster Labs](https://oysterecosystem.com) ecosystem: Universal Phone, Puffy, ClawGlasses, and the $WORLD token network.

## License

GPLv3. See [LICENSE](LICENSE).
