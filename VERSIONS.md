# Versions / Pointers

## Android App (Nana)
- Repo: `howardleegeek/botdrop-android` (fork of `zhixianio/botdrop-android`)
- PR: https://github.com/howardleegeek/botdrop-android/pull/1
- Branch: `codex/oyster-preinstall-chat`
- Build artifact: run `./gradlew assembleDebug` in the Android repo to get a universal debug APK.

## Proxy (OpenAI-compatible)
- Repo: https://github.com/howardleegeek/openclaw-proxy
- Notes: supports `/v1/chat/completions` + token-tier routing + `MOCK_MODE=1` for smoke tests
