package br.edu.unifio.laboratorio.consumidores;

import java.io.IOException;

import org.slf4j.Logger;

import com.rabbitmq.client.Channel;

import br.edu.unifio.laboratorio.contrato.Evento;

/**
 * Confirmação manual (ACK) vinculada ao sucesso do processamento.
 *
 * 1. O efeito é gravado (e forçado para o disco).
 * 2. Só então enviamos basicAck: o RabbitMQ apaga a mensagem da fila.
 * 3. Se der erro, enviamos basicNack sem requeue: a mensagem vai para a DLQ (laboratorio.dlq).
 *
 * Se o processo cair entre o passo 1 e o 2, o RabbitMQ reentrega a mensagem;
 * a checagem de eventoId (idempotência) impede que o efeito aconteça duas vezes.
 */
public final class Confirmacao {

    private Confirmacao() {
    }

    public static void processar(Evento evento, Channel canal, long tag, Logger log, Runnable efeito) throws IOException {
        try {
            efeito.run();
            canal.basicAck(tag, false);
        } catch (RuntimeException e) {
            log.error("Falha ao processar o evento {} ({}). Mensagem enviada para a DLQ: {}",
                    evento.eventoId(), evento.tipo(), e.getMessage());
            canal.basicNack(tag, false, false);
        }
    }
}
