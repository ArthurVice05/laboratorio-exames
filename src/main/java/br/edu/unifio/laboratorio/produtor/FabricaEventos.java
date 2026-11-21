package br.edu.unifio.laboratorio.produtor;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.edu.unifio.laboratorio.contrato.DadosEvento;
import br.edu.unifio.laboratorio.contrato.Evento;
import br.edu.unifio.laboratorio.contrato.TiposEvento;

/**
 * Monta os eventos com dados fictícios (nada de dado real de paciente).
 */
@Component
@Profile("produtor")
public class FabricaEventos {

    static final String ORIGEM = "produtor-laboratorio";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final List<String> EXAMES = List.of("HEMOGRAMA", "GLICEMIA", "COLESTEROL_TOTAL", "TSH", "CREATININA");
    private static final List<String> MOTIVOS_INVALIDACAO = List.of(
            "Erro de digitação no valor de referência", "Amostra trocada na bancada", "Equipamento fora de calibração");
    private static final List<String> MOTIVOS_REJEICAO = List.of(
            "Amostra hemolisada", "Volume insuficiente", "Tubo sem identificação", "Temperatura de transporte inadequada");

    private final AtomicInteger sequenciaLaudo = new AtomicInteger(1000);
    private final AtomicInteger sequenciaAmostra = new AtomicInteger(2000);
    private final AtomicInteger sequenciaPaciente = new AtomicInteger(40);

    public Evento laudoLiberado(PedidoLaudo pedido) {
        PedidoLaudo p = pedido == null ? new PedidoLaudo(null, null, null, null) : pedido;
        return novo(TiposEvento.LAUDO_LIBERADO, new DadosEvento(
                ou(p.laudoId(), "LAU-" + sequenciaLaudo.incrementAndGet()), null,
                ou(p.exame(), sortear(EXAMES)),
                ou(p.pacienteCodigo(), "PAC-" + String.format("%04d", sequenciaPaciente.incrementAndGet())),
                null, null));
    }

    public Evento laudoInvalidado(PedidoLaudo pedido) {
        PedidoLaudo p = pedido == null ? new PedidoLaudo(null, null, null, null) : pedido;
        return novo(TiposEvento.LAUDO_INVALIDADO, new DadosEvento(
                ou(p.laudoId(), "LAU-" + sequenciaLaudo.incrementAndGet()), null,
                ou(p.exame(), sortear(EXAMES)),
                ou(p.pacienteCodigo(), "PAC-" + String.format("%04d", sequenciaPaciente.incrementAndGet())),
                null,
                ou(p.motivo(), sortear(MOTIVOS_INVALIDACAO))));
    }

    public Evento amostraRecebida(PedidoAmostra pedido) {
        PedidoAmostra p = pedido == null ? new PedidoAmostra(null, null, null, null) : pedido;
        return novo(TiposEvento.AMOSTRA_RECEBIDA, new DadosEvento(
                null, ou(p.amostraId(), "AMO-" + sequenciaAmostra.incrementAndGet()),
                ou(p.exame(), sortear(EXAMES)), null,
                ou(p.lote(), loteDoDia()), null));
    }

    public Evento amostraRejeitada(PedidoAmostra pedido) {
        PedidoAmostra p = pedido == null ? new PedidoAmostra(null, null, null, null) : pedido;
        return novo(TiposEvento.AMOSTRA_REJEITADA, new DadosEvento(
                null, ou(p.amostraId(), "AMO-" + sequenciaAmostra.incrementAndGet()),
                ou(p.exame(), sortear(EXAMES)), null,
                ou(p.lote(), loteDoDia()),
                ou(p.motivo(), sortear(MOTIVOS_REJEICAO))));
    }

    public Evento livre(String tipo, DadosEvento dados) {
        return novo(tipo, dados == null ? new DadosEvento(null, null, null, null, null, null) : dados);
    }

    private Evento novo(String tipo, DadosEvento dados) {
        return new Evento(UUID.randomUUID().toString(), tipo, OffsetDateTime.now(FUSO), ORIGEM, dados);
    }

    private String loteDoDia() {
        return "LOTE-" + OffsetDateTime.now(FUSO).toLocalDate() + "-A";
    }

    private static String ou(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    private static String sortear(List<String> opcoes) {
        return opcoes.get(ThreadLocalRandom.current().nextInt(opcoes.size()));
    }
}
