#!/bin/bash
# Funções usadas pelos outros scripts. Rode os scripts a partir da raiz do projeto.

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$RAIZ/target/laboratorio.jar"
LOGS="$RAIZ/logs"
PIDS="$RAIZ/run"
PRODUTOR="http://localhost:8080"

# nome -> "perfil porta"
declare -A APPS=(
  [produtor]="produtor 8080"
  [portal]="portal 8081"
  [comunicacao]="comunicacao 8082"
  [triagem]="triagem 8083"
  [auditoria]="auditoria 8084"
  [indicadores-1]="indicadores 8085"
  [indicadores-2]="indicadores 8086"
)
ORDEM=(produtor portal comunicacao triagem auditoria indicadores-1 indicadores-2)

# true se o PID guardado ainda é uma aplicação deste projeto (evita PID reaproveitado após reiniciar o Codespace)
rodando() {
  local arquivo="$PIDS/$1.pid"
  [ -f "$arquivo" ] && ps -p "$(cat "$arquivo")" -o args= 2>/dev/null | grep -q "laboratorio.jar"
}

porta_de() { echo "${APPS[$1]}" | cut -d' ' -f2; }

aguardar_porta() {
  local porta=$1
  for _ in $(seq 1 90); do
    # curl só termina com sucesso quando a porta responde (qualquer status HTTP)
    if curl -s -o /dev/null "http://localhost:$porta/"; then
      return 0
    fi
    sleep 1
  done
  return 1
}
