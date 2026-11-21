package br.edu.unifio.laboratorio.produtor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unifio.laboratorio.config.Topologia;

/**
 * Situação das filas em tempo real (mensagens aguardando e consumidores conectados).
 * Usado na demonstração de interrupção: com a Comunicação parada, a fila dela acumula a pendência.
 */
@RestController
@Profile("produtor")
public class FilasControlador {

    private static final List<String> FILAS = List.of(Topologia.FILA_PORTAL, Topologia.FILA_COMUNICACAO,
            Topologia.FILA_TRIAGEM, Topologia.FILA_AUDITORIA, Topologia.FILA_INDICADORES, Topologia.FILA_DLQ);

    private final AmqpAdmin admin;

    public FilasControlador(AmqpAdmin admin) {
        this.admin = admin;
    }

    @GetMapping("/filas")
    public Map<String, Object> filas() {
        Map<String, Object> situacao = new LinkedHashMap<>();
        for (String fila : FILAS) {
            QueueInformation info = admin.getQueueInfo(fila);
            situacao.put(fila, info == null ? "não existe"
                    : Map.of("mensagensAguardando", info.getMessageCount(), "consumidores", info.getConsumerCount()));
        }
        return situacao;
    }
}
