#!/bin/bash
# Volta ao estado inicial: para tudo, apaga os registros (dados/, logs/) e esvazia as filas.
source "$(dirname "$0")/comum.sh"
"$RAIZ/scripts/parar.sh" todos
rm -rf "$RAIZ/dados" "$LOGS"
for fila in lab.portal-resultados lab.comunicacao lab.triagem lab.auditoria lab.indicadores laboratorio.dlq; do
  curl -s -o /dev/null -u lab:lab123 -X DELETE "http://localhost:15672/api/queues/%2F/$fila/contents"
done
echo "Registros apagados e filas esvaziadas."
