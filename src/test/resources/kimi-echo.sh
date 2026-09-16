#!/bin/sh
#
# Test double for the `kimi` CLI: prints every argument verbatim, joined by
# single spaces on one line, and exits with 0 — the same output contract as
# /bin/echo but portable. /bin/echo cannot be used directly because GNU
# coreutils echo (Linux) interprets --version/--help as flags while BSD echo
# (macOS) prints them literally.
#
# Drain piped stdin first: consumers reading to EOF finish instantly and the
# executor's input pump cannot race a closed pipe.
cat > /dev/null 2>/dev/null
printf '%s\n' "$*"
