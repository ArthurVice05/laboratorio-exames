#!/bin/bash
# Para uma aplicação (ou todas).
# Uso: scripts/parar.sh comunicacao   |   scripts/parar.sh todos
source "$(dirname "$0")/comum.sh"
alvo=${1:-todos}
if [ "$alvo" = "todos" ]; then nomes=("${ORDEM[@]}"); else nomes=("$alvo"); fi
for nome in "${nomes[@]}"; do
  arquivo="$PIDS/$nome.pid"
  if rodando "$nome"; then
    kill "$(cat "$arquivo")"
    while kill -0 "$(cat "$arquivo")" 2>/dev/null; do sleep 0.5; done
    echo "$nome parado"
  fi
  rm -f "$arquivo"
done
