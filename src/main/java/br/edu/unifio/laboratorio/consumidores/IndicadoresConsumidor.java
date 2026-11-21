package br.edu.unifio.laboratorio.consumidores;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import br.edu.unifio.laboratorio.comum.Armazenamento;
import br.edu.unifio.laboratorio.comum.RegistroJsonl;
import br.edu.unifio.laboratorio.config.Topologia;
import br.edu.unifio.laboratorio.contrato.Evento;

/**
 * 5. INDICADORES OPERACIONAIS - recebe somente "laudo.liberado".
 * Duas instâncias consomem a MESMA fila (consumidores concorrentes): o RabbitMQ entrega cada
 * mensagem a uma só delas. Com prefetch=1, quem estiver livre pega a próxima.
 * Efeito: contabiliza o laudo; o total por exame é calculado sobre o registro compartilhado.
 */
@Component
@Profile("indicadores")
public class IndicadoresConsumidor {

    private static final Logger log = LoggerFactory.getLogger(IndicadoresConsumidor.class);

    private final RegistroJsonl contabilizados;
    private final String instancia;
    private final long tempoProcessamentoMs;

    public IndicadoresConsumidor(Armazenamento armazenamento, @Value("${app.instancia}") String instancia,
                                 @Value("${app.indicadores.tempo-processamento-ms:300}") long tempoProcessamentoMs) {
        this.contabilizados = armazenamento.registro("indicadores-laudos.jsonl");
        this.instancia = instancia;
        this.tempoProcessamentoMs = tempoProcessamentoMs;
    }

    @RabbitListener(queues = Topologia.FILA_INDICADORES)
    public void receber(Evento evento, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        Confirmacao.processar(evento, canal, tag, log, () -> {
            simularProcessamento();
            boolean novo = contabilizados.registrar(evento.eventoId(), true, dup -> new LaudoContabilizado(
                    evento.eventoId(), evento.dados().laudoId(), evento.dados().exame(), instancia, OffsetDateTime.now()));
            if (novo) {
                log.info("[{}] Contabilizado laudo {} de {} - evento {}", instancia, evento.dados().laudoId(),
                        evento.dados().exame(), evento.eventoId());
            } else {
                log.warn("[{}] Evento {} repetido: não contabilizado de novo", instancia, evento.eventoId());
            }
        });
    }

    // Pequena pausa para a divisão entre as duas instâncias ficar visível na demonstração
    private void simularProcessamento() {
        try {
            Thread.sleep(tempoProcessamentoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record LaudoContabilizado(String eventoId, String laudoId, String exame, String instancia,
                                     OffsetDateTime processadoEm) {
    }
}
