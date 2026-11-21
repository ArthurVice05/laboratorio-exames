#!/bin/bash
# Inicia uma aplicação em segundo plano.
# Uso: scripts/iniciar.sh produtor|portal|comunicacao|triagem|auditoria|indicadores-1|indicadores-2
source "$(dirname "$0")/comum.sh"
nome=$1
if [ -z "$nome" ] || [ -z "${APPS[$nome]}" ]; then
  echo "Uso: scripts/iniciar.sh <nome>   nomes: ${ORDEM[*]}"; exit 1
fi
[ -f "$JAR" ] || "$RAIZ/scripts/compilar.sh" || exit 1
mkdir -p "$LOGS" "$PIDS"
if rodando "$nome"; then
  echo "$nome já está rodando (PID $(cat "$PIDS/$nome.pid"))"; exit 0
fi
read -r perfil porta <<< "${APPS[$nome]}"
cd "$RAIZ"
nohup java -jar "$JAR" --spring.profiles.active="$perfil" --server.port="$porta" --app.instancia="$nome" \
  > "$LOGS/$nome.log" 2>&1 &
echo $! > "$PIDS/$nome.pid"
if aguardar_porta "$porta"; then
  echo "$nome iniciado (porta $porta, PID $(cat "$PIDS/$nome.pid")) - log em logs/$nome.log"
else
  echo "ERRO: $nome não respondeu na porta $porta. Veja logs/$nome.log"; exit 1
fi
