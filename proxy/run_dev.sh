#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

python3 -m venv .venv
. .venv/bin/activate

python3 -m pip install -q --upgrade pip
pip install -q -r requirements.txt

export LISTEN_HOST="${LISTEN_HOST:-127.0.0.1}"
export LISTEN_PORT="${LISTEN_PORT:-8080}"

python3 server.py
