#!/bin/sh
# Capture a theme prototype in both modes: sh src/shot.sh <slug> [outdir]
# Writes <slug>-light.png and <slug>-dark.png (phones row + desktop, 1760px wide).
here="$(cd "$(dirname "$0")/.." && pwd)"
out="${2:-$here/../../.impeccable/review/themes}"
mkdir -p "$out"
bin="$(ls -d "$HOME"/Library/Caches/ms-playwright/chromium_headless_shell-*/chrome-*/headless_shell 2>/dev/null | tail -1)"
[ -x "$bin" ] || bin="$(ls -d "$HOME"/Library/Caches/ms-playwright/chromium_headless_shell-*/*/*hell* 2>/dev/null | tail -1)"
for m in light dark; do
  "$bin" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --virtual-time-budget=6000 \
    --user-data-dir="$(mktemp -d)" --window-size=1760,2140 --screenshot="$out/$1-$m.png" "file://$here/$1.html#$m-still" >/dev/null 2>&1
  echo "$out/$1-$m.png"
done
