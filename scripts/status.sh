#!/bin/bash
# Mostra quais aplicações estão rodando e a situação das filas
source "$(dirname "$0")/comum.sh"
echo "Aplicações:"
for nome in "${ORDEM[@]}"; do
  if rodando "$nome"; then
    printf "  %-14s rodando  (porta %s)\n" "$nome" "$(porta_de "$nome")"
  else
    printf "  %-14s parado\n" "$nome"
  fi
done
echo
echo "Filas (via produtor):"
if situacao=$(curl -sf "$PRODUTOR/filas"); then echo "$situacao" | jq .; else echo "  (produtor fora do ar)"; fi
