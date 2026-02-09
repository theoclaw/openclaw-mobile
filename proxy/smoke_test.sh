#!/usr/bin/env bash
set -euo pipefail

BASE="${1:-http://127.0.0.1:8080}"

echo "[1/3] health"
curl -sS "${BASE}/health" | python3 -c 'import sys,json; print(json.load(sys.stdin))'

echo "[2/3] unauthorized"
code=$(curl -sS -o /dev/null -w "%{http_code}" "${BASE}/v1/chat/completions" -H 'Content-Type: application/json' -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}' || true)
echo "status=$code (expected 401)"

echo "[3/3] mock chat"
curl -sS "${BASE}/v1/chat/completions" \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer ocw1_smoketest' \
  -d '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"你好 Nana"}]}' | python3 -c 'import sys,json; obj=json.load(sys.stdin); print(obj.get("choices")[0]["message"]["content"])'
