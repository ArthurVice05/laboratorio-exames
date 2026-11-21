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
import br.edu.unifio.laboratorio.contrato.DadosEvento;
import br.edu.unifio.laboratorio.contrato.Evento;

/**
 * 4. AUDITORIA - recebe TODOS os eventos (binding "#").
 * Efeito: histórico completo. Diferente dos outros, registra também as entregas repetidas,
 * marcando "duplicado": true, porque o histórico deve mostrar tudo o que chegou.
 */
@Component
@Profile("auditoria")
public class AuditoriaConsumidor {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaConsumidor.class);

    private final RegistroJsonl historico;
    private final String instancia;

    public AuditoriaConsumidor(Armazenamento armazenamento, @Value("${app.instancia}") String instancia) {
        this.historico = armazenamento.registro("auditoria-historico.jsonl");
        this.instancia = instancia;
    }

    @RabbitListener(queues = Topologia.FILA_AUDITORIA)
    public void receber(Evento evento, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        Confirmacao.processar(evento, canal, tag, log, () -> {
            boolean novo = historico.registrar(evento.eventoId(), false, duplicado -> new RegistroAuditoria(
                    evento.eventoId(), evento.tipo(), evento.ocorridoEm(), OffsetDateTime.now(), duplicado,
                    evento.origem(), evento.dados()));
            log.info("[{}] Auditado: {} (eventoId {}){}", instancia, evento.tipo(), evento.eventoId(),
                    novo ? "" : " - entrega repetida");
        });
    }

    public record RegistroAuditoria(String eventoId, String tipo, OffsetDateTime ocorridoEm, OffsetDateTime recebidoEm,
                                    boolean duplicado, String origem, DadosEvento dados) {
    }
}
