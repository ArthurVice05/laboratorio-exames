#!/bin/bash
# Reproduz os testes do roteiro e confere automaticamente cada resultado.
#   1. Roteamento dos quatro eventos (+ eventos novos)
#   2. Trabalho concorrente das duas instâncias de Indicadores
#   3. Interrupção e recuperação da Comunicação
#   4. Duplicidade: republicação com o mesmo eventoId
# ATENÇÃO: começa do zero (apaga dados/ e esvazia as filas).
source "$(dirname "$0")/comum.sh"

OK=0; FALHAS=0
verificar() {  # verificar "descrição" condição(0/1)
  if [ "$2" = "1" ]; then echo "  [OK]    $1"; OK=$((OK+1)); else echo "  [FALHA] $1"; FALHAS=$((FALHAS+1)); fi
}
publicar() {  # publicar endpoint [json]
  if [ -n "$2" ]; then
    curl -s -X POST -H "Content-Type: application/json" -d "$2" "$PRODUTOR/eventos/$1"
  else
    curl -s -X POST "$PRODUTOR/eventos/$1"
  fi
}
tem() {  # tem URL eventoId -> 1 se o eventoId aparece na lista
  curl -s "$1" | jq --arg id "$2" '[.[] | select(.eventoId == $id)] | length > 0' | grep -q true && echo 1 || echo 0
}
nao_tem() { [ "$(tem "$1" "$2")" = "1" ] && echo 0 || echo 1; }
PORTAL="http://localhost:8081/portal/laudos"
COMUNICACAO="http://localhost:8082/comunicacao/avisos"
TRIAGEM="http://localhost:8083/triagem/registros"
AUDITORIA="http://localhost:8084/auditoria"
INDICADORES="http://localhost:8085/indicadores/processamentos"
espera() { sleep "${1:-3}"; }

echo "== Preparando ambiente limpo"
"$RAIZ/scripts/limpar.sh" >/dev/null
"$RAIZ/scripts/compilar.sh" >/dev/null || { echo "Falha ao compilar"; exit 1; }
"$RAIZ/scripts/iniciar-todos.sh" >/dev/null || { echo "Falha ao iniciar as aplicações (veja logs/)"; exit 1; }
espera 3

echo
echo "== 1. Roteamento"
L=$(publicar laudo-liberado | jq -r .eventoId)
I=$(publicar laudo-invalidado | jq -r .eventoId)
R=$(publicar amostra-recebida | jq -r .eventoId)
J=$(publicar amostra-rejeitada | jq -r .eventoId)
espera 4
echo " laudo.liberado ($L)"
verificar "Portal recebeu"          "$(tem "$PORTAL" "$L")"
verificar "Comunicação recebeu"     "$(tem "$COMUNICACAO" "$L")"
verificar "Triagem NÃO recebeu"     "$(nao_tem "$TRIAGEM" "$L")"
verificar "Auditoria recebeu"       "$(tem "$AUDITORIA" "$L")"
verificar "Indicadores recebeu"     "$(tem "$INDICADORES" "$L")"
echo " laudo.invalidado ($I)"
verificar "Portal NÃO recebeu"      "$(nao_tem "$PORTAL" "$I")"
verificar "Comunicação recebeu"     "$(tem "$COMUNICACAO" "$I")"
verificar "Triagem NÃO recebeu"     "$(nao_tem "$TRIAGEM" "$I")"
verificar "Auditoria recebeu"       "$(tem "$AUDITORIA" "$I")"
verificar "Indicadores NÃO recebeu" "$(nao_tem "$INDICADORES" "$I")"
echo " amostra.recebida ($R)"
verificar "Portal NÃO recebeu"      "$(nao_tem "$PORTAL" "$R")"
verificar "Comunicação NÃO recebeu" "$(nao_tem "$COMUNICACAO" "$R")"
verificar "Triagem recebeu"         "$(tem "$TRIAGEM" "$R")"
verificar "Auditoria recebeu"       "$(tem "$AUDITORIA" "$R")"
verificar "Indicadores NÃO recebeu" "$(nao_tem "$INDICADORES" "$R")"
echo " amostra.rejeitada ($J)"
verificar "Portal NÃO recebeu"      "$(nao_tem "$PORTAL" "$J")"
verificar "Comunicação recebeu"     "$(tem "$COMUNICACAO" "$J")"
verificar "Triagem recebeu"         "$(tem "$TRIAGEM" "$J")"
verificar "Auditoria recebeu"       "$(tem "$AUDITORIA" "$J")"
verificar "Indicadores NÃO recebeu" "$(nao_tem "$INDICADORES" "$J")"

echo " evento novo da família amostra: amostra.descartada"
D=$(publicar livre '{"tipo":"amostra.descartada","dados":{"amostraId":"AMO-9001","lote":"LOTE-TESTE","motivo":"Prazo de armazenamento vencido"}}' | jq -r .eventoId)
echo " evento sem regra específica: laudo.revisado"
V=$(publicar livre '{"tipo":"laudo.revisado","dados":{"laudoId":"LAU-9001","exame":"TSH"}}' | jq -r .eventoId)
espera 4
verificar "amostra.descartada: Triagem recebeu (amostra.*)"   "$(tem "$TRIAGEM" "$D")"
verificar "amostra.descartada: Auditoria recebeu (#)"         "$(tem "$AUDITORIA" "$D")"
verificar "amostra.descartada: Comunicação NÃO recebeu"       "$(nao_tem "$COMUNICACAO" "$D")"
verificar "laudo.revisado: só a Auditoria recebeu"            "$(tem "$AUDITORIA" "$V")"
verificar "laudo.revisado: Portal NÃO recebeu"                "$(nao_tem "$PORTAL" "$V")"
verificar "laudo.revisado: Indicadores NÃO recebeu"           "$(nao_tem "$INDICADORES" "$V")"

echo
echo "== 2. Trabalho concorrente (Indicadores 1 e 2)"
antes=$(curl -s http://localhost:8085/indicadores | jq .totalLaudosLiberados)
publicar "lote/laudos-liberados?quantidade=10" >/dev/null
espera 8
depois=$(curl -s http://localhost:8085/indicadores | jq .totalLaudosLiberados)
inst1=$(curl -s http://localhost:8085/indicadores | jq '.processadosPorInstancia["indicadores-1"] // 0')
inst2=$(curl -s http://localhost:8085/indicadores | jq '.processadosPorInstancia["indicadores-2"] // 0')
echo "  total antes: $antes | depois: $depois | indicadores-1: $inst1 | indicadores-2: $inst2"
verificar "10 laudos contabilizados uma única vez" "$([ $((depois - antes)) -eq 10 ] && echo 1 || echo 0)"
verificar "as duas instâncias processaram eventos" "$([ "$inst1" -gt 0 ] && [ "$inst2" -gt 0 ] && echo 1 || echo 0)"
verificar "as duas instâncias mostram o mesmo total" "$([ "$(curl -s http://localhost:8086/indicadores | jq .totalLaudosLiberados)" = "$depois" ] && echo 1 || echo 0)"

echo
echo "== 3. Interrupção e recuperação da Comunicação"
"$RAIZ/scripts/parar.sh" comunicacao >/dev/null
X=$(publicar laudo-invalidado | jq -r .eventoId)
espera 3
pendentes=$(curl -s "$PRODUTOR/filas" | jq '.["lab.comunicacao"].mensagensAguardando')
echo "  com a Comunicação parada: $pendentes mensagem(ns) aguardando na fila lab.comunicacao"
verificar "Auditoria processou mesmo com a Comunicação parada" "$(tem "$AUDITORIA" "$X")"
verificar "o evento ficou pendente na fila da Comunicação"    "$([ "$pendentes" -ge 1 ] && echo 1 || echo 0)"
"$RAIZ/scripts/iniciar.sh" comunicacao >/dev/null
espera 4
verificar "ao religar, a Comunicação processou o pendente"     "$(tem "$COMUNICACAO" "$X")"
verificar "fila da Comunicação zerada" "$([ "$(curl -s "$PRODUTOR/filas" | jq '.["lab.comunicacao"].mensagensAguardando')" = "0" ] && echo 1 || echo 0)"

echo
echo "== 4. Duplicidade (republicação com o mesmo eventoId $L)"
LAUDO=$(curl -s "$PORTAL" | jq -r --arg id "$L" '.[] | select(.eventoId == $id) | .laudoId')
curl -s -X POST "$PRODUTOR/eventos/$L/republicar" >/dev/null
espera 4
pub=$(curl -s http://localhost:8081/portal/resumo | jq --arg l "$LAUDO" '.publicacoesPorLaudo[$l]')
ign=$(curl -s http://localhost:8081/portal/resumo | jq .eventosRepetidosIgnorados)
aud=$(curl -s "$AUDITORIA?eventoId=$L" | jq length)
dup=$(curl -s "$AUDITORIA?eventoId=$L" | jq '[.[] | select(.duplicado)] | length')
ind=$(curl -s "$INDICADORES" | jq --arg id "$L" '[.[] | select(.eventoId == $id)] | length')
echo "  laudo $LAUDO: publicações no portal = $pub | repetições ignoradas = $ign | auditoria = $aud entregas ($dup duplicada)"
verificar "Portal publicou o laudo uma única vez"          "$([ "$pub" = "1" ] && echo 1 || echo 0)"
verificar "Portal registrou a repetição como ignorada"     "$([ "$ign" -ge 1 ] && echo 1 || echo 0)"
verificar "Auditoria registrou as 2 entregas (1 duplicada)" "$([ "$aud" = "2" ] && [ "$dup" = "1" ] && echo 1 || echo 0)"
verificar "Indicadores não contou de novo"                 "$([ "$ind" = "1" ] && echo 1 || echo 0)"
verificar "Nenhuma mensagem foi para a DLQ" "$([ "$(curl -s "$PRODUTOR/filas" | jq '.["laboratorio.dlq"].mensagensAguardando')" = "0" ] && echo 1 || echo 0)"

echo
echo "Resultado: $OK OK, $FALHAS falha(s)."
echo "As aplicações continuam rodando. Para parar: scripts/parar.sh todos"
[ "$FALHAS" -eq 0 ]
