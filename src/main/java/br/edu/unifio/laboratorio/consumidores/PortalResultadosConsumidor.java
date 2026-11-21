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
 * 1. PORTAL DE RESULTADOS - recebe somente "laudo.liberado".
 * Efeito: publica o laudo no portal UMA única vez por eventoId (idempotente).
 */
@Component
@Profile("portal")
public class PortalResultadosConsumidor {

    private static final Logger log = LoggerFactory.getLogger(PortalResultadosConsumidor.class);

    private final RegistroJsonl publicacoes;
    private final RegistroJsonl duplicatas;
    private final String instancia;

    public PortalResultadosConsumidor(Armazenamento armazenamento, @Value("${app.instancia}") String instancia) {
        this.publicacoes = armazenamento.registro("portal-publicacoes.jsonl");
        this.duplicatas = armazenamento.registro("portal-duplicatas-ignoradas.jsonl");
        this.instancia = instancia;
    }

    @RabbitListener(queues = Topologia.FILA_PORTAL)
    public void receber(Evento evento, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        Confirmacao.processar(evento, canal, tag, log, () -> {
            boolean novo = publicacoes.registrar(evento.eventoId(), true, duplicado -> new LaudoPublicado(
                    evento.eventoId(), evento.dados().laudoId(), evento.dados().exame(),
                    evento.dados().pacienteCodigo(), OffsetDateTime.now(), instancia));
            if (novo) {
                log.info("[{}] Laudo {} ({}) publicado no portal - evento {}", instancia,
                        evento.dados().laudoId(), evento.dados().exame(), evento.eventoId());
            } else {
                duplicatas.registrar(null, false, d -> new DuplicataIgnorada(
                        evento.eventoId(), evento.dados().laudoId(), OffsetDateTime.now()));
                log.warn("[{}] Evento {} REPETIDO: laudo {} já estava publicado, nada foi feito", instancia,
                        evento.eventoId(), evento.dados().laudoId());
            }
        });
    }

    public record LaudoPublicado(String eventoId, String laudoId, String exame, String pacienteCodigo,
                                 OffsetDateTime publicadoEm, String instancia) {
    }

    public record DuplicataIgnorada(String eventoId, String laudoId, OffsetDateTime recebidoEm) {
    }
}
