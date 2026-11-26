# Roteiro da apresentação (até 20 minutos)

Antes de começar, num terminal do Codespaces:

```bash
scripts/limpar.sh && scripts/iniciar-todos.sh
```

Deixem abertos:

- o **painel do RabbitMQ** (porta 15672, usuário `lab`, senha `lab123`), nas abas *Queues* e *Exchanges*;
- um **segundo terminal** para os comandos.

> Cada integrante apresenta um bloco (a divisão pode ser trocada pelo grupo).

---

## Bloco 1 — Arquitetura e contrato (3 min) · Arthur Viceles Vida

**O que mostrar:**

- O diagrama `docs/arquitetura.png`.
- O contrato JSON no README.

**O que falar:**

- O domínio: o laboratório libera e invalida laudos (processo principal) e recebe e rejeita amostras (processo especializado da triagem).
- O caminho da mensagem: o **produtor** publica na exchange **topic** `laboratorio.eventos`, com o **tipo do evento como routing key**. A exchange copia a mensagem para cada **fila** cujo binding casa com a chave. Cada **consumidor** lê a sua fila, grava o efeito e só então confirma (ACK).
- Os campos do contrato:
  - `eventoId` é único e é usado na duplicidade;
  - `tipo` é a routing key;
  - `ocorridoEm` é o horário do fato;
  - `dados` traz o que cada consumidor precisa.

---

## Bloco 2 — Roteamento (5 min) · Leonardo Vieira Alvim

Mostrem a **matriz evento × consumidor** do README. Antes de cada comando, **digam quem deve receber e quem não deve**. Depois confiram.

```bash
curl -s -X POST localhost:8080/eventos/laudo-liberado | jq
curl -s -X POST localhost:8080/eventos/laudo-invalidado | jq
curl -s -X POST localhost:8080/eventos/amostra-recebida | jq
curl -s -X POST localhost:8080/eventos/amostra-rejeitada | jq
```

Para conferir os efeitos:

```bash
curl -s localhost:8081/portal/laudos | jq            # só o laudo liberado
curl -s localhost:8082/comunicacao/avisos | jq       # liberado, invalidado e rejeitada
curl -s localhost:8083/triagem/registros | jq        # recebida e rejeitada
curl -s localhost:8084/auditoria | jq                # todos
curl -s localhost:8085/indicadores | jq              # só o liberado
```

**No painel:** em *Exchanges → laboratorio.eventos* aparecem os bindings. Em *Queues* aparecem as contagens de mensagens.

**Por que topic e não direct ou fanout** (tabela no README):

- **Direct:** não acompanha uma família.
- **Fanout:** não seleciona evento.
- **Topic:** faz as duas coisas e ainda cobre o "recebe tudo" (`#`).

---

## Bloco 3 — Trabalho concorrente (3 min) · Gabriel Gonçalves

As duas instâncias já estão rodando (8085 e 8086). Publiquem 10 laudos distintos:

```bash
curl -s -X POST "localhost:8080/eventos/lote/laudos-liberados?quantidade=10" | jq length
curl -s localhost:8085/indicadores | jq     # totais por exame + quantos cada instância processou
curl -s localhost:8085/indicadores/processamentos | jq '.[] | {laudoId, instancia}'
```

Para ver ao vivo: `tail -f logs/indicadores-1.log` e `tail -f logs/indicadores-2.log` em dois terminais.

**O que falar:**

- As duas instâncias consomem a **mesma fila** `lab.indicadores` (*competing consumers*). O RabbitMQ entrega cada mensagem a **uma** delas.
- Com `prefetch=1`, quem está livre pega a próxima. A divisão não precisa ser igual.
- Os outros consumidores interessados também receberam os 10 laudos: Portal, Comunicação e Auditoria.

---

## Bloco 4 — Interrupção e recuperação (4 min) · Mauro Roque

```bash
scripts/parar.sh comunicacao
ID=$(curl -s -X POST localhost:8080/eventos/laudo-invalidado | jq -r .eventoId); echo $ID
curl -s "localhost:8084/auditoria?eventoId=$ID" | jq          # a Auditoria processou normalmente
curl -s localhost:8080/filas | jq '.["lab.comunicacao"]'       # mensagensAguardando: 1, consumidores: 0
```

No painel, a fila `lab.comunicacao` mostra **1 mensagem Ready**. Depois religuem:

```bash
scripts/iniciar.sh comunicacao
sleep 2
curl -s localhost:8082/comunicacao/avisos | jq '.[] | select(.eventoId == "'$ID'")'
curl -s localhost:8080/filas | jq '.["lab.comunicacao"]'       # voltou a 0
```

**O que falar:**

- A fila é **durável** e a mensagem é **persistente**. Por isso o evento esperou a Comunicação voltar.
- Os outros consumidores têm filas próprias e não foram afetados (**desacoplamento temporal**).

---

## Bloco 5 — Duplicidade e defesa (5 min) · Lucas Adriano

Peguem o `eventoId` de um laudo já publicado e republiquem com o **mesmo id**:

```bash
ID=$(curl -s localhost:8081/portal/laudos | jq -r '.[0].eventoId'); echo $ID
curl -s -X POST localhost:8080/eventos/$ID/republicar | jq
curl -s localhost:8081/portal/resumo | jq                     # publicacoesPorLaudo: 1 / eventosRepetidosIgnorados: 1
curl -s "localhost:8084/auditoria?eventoId=$ID" | jq          # 2 entregas, a segunda com "duplicado": true
```

**Explicar a confirmação:**

- O ACK é **manual** e só acontece depois que o efeito foi gravado no disco.
- Se o consumidor cair antes do ACK, o RabbitMQ reentrega a mensagem. A checagem do `eventoId` impede o efeito duplicado.
- Se o processamento der erro, o `nack` manda a mensagem para a `laboratorio.dlq`.

**Perguntas prováveis:**

- **"E se surgir um evento novo da família amostra?"**
  ```bash
  curl -s -X POST localhost:8080/eventos/livre -H "Content-Type: application/json" \
       -d '{"tipo":"amostra.descartada","dados":{"amostraId":"AMO-9001","lote":"LOTE-B","motivo":"Prazo vencido"}}'
  ```
  Chega na Triagem (`amostra.*`) e na Auditoria (`#`) sem mudar código nem configuração.
- **"E um evento que nenhuma regra específica pega?"** Um `laudo.revisado` só chega na Auditoria. Se nem ela existisse, o broker devolveria a mensagem ao produtor (`mandatory`).
- **"E se a regra mudar, por exemplo o Portal também publicar laudo invalidado?"** Basta adicionar o binding `laudo.invalidado` à fila `lab.portal-resultados` em `RabbitConfig`. O produtor não muda.
- **"Por que não deixar o RabbitMQ confirmar sozinho (auto-ack)?"** Com auto-ack, a mensagem é apagada na entrega. Se o consumidor falhar, o evento se perde.
