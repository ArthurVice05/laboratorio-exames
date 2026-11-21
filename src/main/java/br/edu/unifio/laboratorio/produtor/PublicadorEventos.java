package br.edu.unifio.laboratorio.produtor;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.unifio.laboratorio.comum.Armazenamento;
import br.edu.unifio.laboratorio.comum.RegistroJsonl;
import br.edu.unifio.laboratorio.config.Topologia;
import br.edu.unifio.laboratorio.contrato.Evento;

/**
 * Publica os eventos na exchange topic, usando o tipo como routing key.
 * Mensagens persistentes + filas duráveis: um consumidor desligado não perde eventos.
 */
@Service
@Profile("produtor")
public class PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final RabbitTemplate rabbitTemplate;
    private final RegistroJsonl publicacoes;
    private final Armazenamento armazenamento;

    public PublicadorEventos(RabbitTemplate rabbitTemplate, Armazenamento armazenamento) {
        this.rabbitTemplate = rabbitTemplate;
        this.armazenamento = armazenamento;
        this.publicacoes = armazenamento.registro("produtor-publicacoes.jsonl");

        // Confirmação do broker (publisher confirm): o RabbitMQ avisa se recebeu a mensagem
        rabbitTemplate.setConfirmCallback((correlacao, ack, causa) -> {
            String id = correlacao == null ? "?" : correlacao.getId();
            if (ack) {
                log.info("Broker confirmou o evento {}", id);
            } else {
                log.error("Broker NÃO confirmou o evento {}: {}", id, causa);
            }
        });
        // Mensagem que não casou com nenhuma fila volta para o produtor (mandatory=true)
        rabbitTemplate.setReturnsCallback(retorno -> log.warn("Evento sem fila de destino (routing key {}): {}",
                retorno.getRoutingKey(), retorno.getReplyText()));
    }

    public Evento publicar(Evento evento, boolean republicacao) {
        rabbitTemplate.convertAndSend(Topologia.EXCHANGE_EVENTOS, evento.tipo(), evento, mensagem -> {
            mensagem.getMessageProperties().setMessageId(evento.eventoId());
            mensagem.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return mensagem;
        }, new CorrelationData(evento.eventoId()));

        publicacoes.registrar(null, false, duplicado ->
                new RegistroPublicacao(evento.eventoId(), evento.tipo(), OffsetDateTime.now(), republicacao, evento));
        log.info("{} {} (eventoId {})", republicacao ? "REPUBLICADO" : "Publicado", evento.tipo(), evento.eventoId());
        return evento;
    }

    public List<JsonNode> listarPublicacoes() {
        return publicacoes.ler();
    }

    // Procura um evento já publicado para republicar com o MESMO eventoId (teste de duplicidade)
    public Evento buscarPublicado(String eventoId) {
        for (JsonNode registro : publicacoes.ler()) {
            if (eventoId.equals(registro.path("eventoId").asText())) {
                try {
                    return armazenamento.mapper().treeToValue(registro.get("evento"), Evento.class);
                } catch (Exception e) {
                    throw new IllegalStateException("Registro do evento " + eventoId + " ilegível", e);
                }
            }
        }
        return null;
    }

    public record RegistroPublicacao(String eventoId, String tipo, OffsetDateTime publicadoEm,
                                     boolean republicacao, Evento evento) {
    }
}
