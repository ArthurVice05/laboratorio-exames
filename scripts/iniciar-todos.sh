#!/bin/bash
# Compila (se preciso) e inicia o produtor e os cinco consumidores (Indicadores com 2 instâncias)
source "$(dirname "$0")/comum.sh"
[ -f "$JAR" ] || "$RAIZ/scripts/compilar.sh" || exit 1
for nome in "${ORDEM[@]}"; do "$RAIZ/scripts/iniciar.sh" "$nome" || exit 1; done
echo
"$RAIZ/scripts/status.sh"
