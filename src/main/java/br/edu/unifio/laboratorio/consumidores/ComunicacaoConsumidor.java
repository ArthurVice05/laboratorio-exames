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
import br.edu.unifio.laboratorio.contrato.TiposEvento;

/**
 * 2. COMUNICAÇÃO DO LABORATÓRIO - recebe "laudo.liberado", "laudo.invalidado" e "amostra.rejeitada".
 * Efeito: registra o aviso (envio simulado) para o destinatário certo.
 */
@Component
@Profile("comunicacao")
public class ComunicacaoConsumidor {

    private static final Logger log = LoggerFactory.getLogger(ComunicacaoConsumidor.class);

    private final RegistroJsonl avisos;
    private final String instancia;

    public ComunicacaoConsumidor(Armazenamento armazenamento, @Value("${app.instancia}") String instancia) {
        this.avisos = armazenamento.registro("comunicacao-avisos.jsonl");
        this.instancia = instancia;
    }

    @RabbitListener(queues = Topologia.FILA_COMUNICACAO)
    public void receber(Evento evento, Channel canal, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        Confirmacao.processar(evento, canal, tag, log, () -> {
            DadosEvento d = evento.dados();
            String destinatario;
            String mensagem;
            switch (evento.tipo()) {
                case TiposEvento.LAUDO_LIBERADO -> {
                    destinatario = "Paciente " + d.pacienteCodigo();
                    mensagem = "Seu laudo " + d.laudoId() + " (" + d.exame() + ") está disponível no portal de resultados.";
                }
                case TiposEvento.LAUDO_INVALIDADO -> {
                    destinatario = "Paciente " + d.pacienteCodigo() + " e médico solicitante";
                    mensagem = "O laudo " + d.laudoId() + " (" + d.exame() + ") foi invalidado. Motivo: " + d.motivo()
                            + ". Um novo laudo será emitido.";
                }
                case TiposEvento.AMOSTRA_REJEITADA -> {
                    destinatario = "Unidade de coleta";
                    mensagem = "A amostra " + d.amostraId() + " do lote " + d.lote() + " foi rejeitada. Motivo: "
                            + d.motivo() + ". Solicitar nova coleta.";
                }
                default -> {
                    destinatario = "Equipe do laboratório";
                    mensagem = "Evento " + evento.tipo() + " recebido.";
                }
            }
            boolean novo = avisos.registrar(evento.eventoId(), true, dup -> new AvisoRegistrado(
                    evento.eventoId(), evento.tipo(), destinatario, mensagem, OffsetDateTime.now(), instancia));
            if (novo) {
                log.info("[{}] Aviso registrado ({}) para {}: {}", instancia, evento.tipo(), destinatario, mensagem);
            } else {
                log.warn("[{}] Evento {} repetido: aviso já tinha sido registrado", instancia, evento.eventoId());
            }
        });
    }

    public record AvisoRegistrado(String eventoId, String tipo, String destinatario, String mensagem,
                                  OffsetDateTime registradoEm, String instancia) {
    }
}
