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
 * 3. TRIAGEM - recebe toda a família "amostra.*", inclusive tipos novos (ex.: amostra.descartada).
 * Efeito: registra a situação da amostra com lote e motivo (quando houver).
 */
@Component
@Profile("triagem")
public class TriagemConsumidor {

    private static final Logger log = LoggerFactory.getLogger(TriagemConsumidor.class);

    private final RegistroJsonl registros;
    private final String instancia;

    public TriagemConsumidor(Armazenamento armazenamento, @Value("${app.instancia}") String instancia) {
        this.registros = armazenamento.registro("triagem-registros.jsonl");
        this.instancia = instancia;
    }

    @RabbitListener(queues = Topologia.FILA_TRIAGEM)
    public void receber(Evento evento, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        Confirmacao.processar(evento, canal, tag, log, () -> {
            // "amostra.recebida" -> situação "recebida"; um tipo novo da família também é aceito
            String situacao = evento.tipo().substring(evento.tipo().indexOf('.') + 1);
            boolean novo = registros.registrar(evento.eventoId(), true, dup -> new RegistroTriagem(
                    evento.eventoId(), evento.tipo(), situacao, evento.dados().amostraId(), evento.dados().lote(),
                    evento.dados().exame(), evento.dados().motivo(), OffsetDateTime.now(), instancia));
            if (novo) {
                log.info("[{}] Amostra {} do lote {}: {}{}", instancia, evento.dados().amostraId(), evento.dados().lote(),
                        situacao, evento.dados().motivo() == null ? "" : " (motivo: " + evento.dados().motivo() + ")");
            } else {
                log.warn("[{}] Evento {} repetido: já registrado na triagem", instancia, evento.eventoId());
            }
        });
    }

    public record RegistroTriagem(String eventoId, String tipo, String situacao, String amostraId, String lote,
                                  String exame, String motivo, OffsetDateTime registradoEm, String instancia) {
    }
}
