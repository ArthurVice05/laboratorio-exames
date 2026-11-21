package br.edu.unifio.laboratorio.produtor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.unifio.laboratorio.contrato.Evento;

/**
 * API do produtor (porta 8080). Cada endpoint registra um fato e publica o evento.
 */
@RestController
@RequestMapping("/eventos")
@Profile("produtor")
public class EventoControlador {

    private final FabricaEventos fabrica;
    private final PublicadorEventos publicador;

    public EventoControlador(FabricaEventos fabrica, PublicadorEventos publicador) {
        this.fabrica = fabrica;
        this.publicador = publicador;
    }

    @PostMapping("/laudo-liberado")
    public ResponseEntity<Evento> laudoLiberado(@RequestBody(required = false) PedidoLaudo pedido) {
        return criado(publicador.publicar(fabrica.laudoLiberado(pedido), false));
    }

    @PostMapping("/laudo-invalidado")
    public ResponseEntity<Evento> laudoInvalidado(@RequestBody(required = false) PedidoLaudo pedido) {
        return criado(publicador.publicar(fabrica.laudoInvalidado(pedido), false));
    }

    @PostMapping("/amostra-recebida")
    public ResponseEntity<Evento> amostraRecebida(@RequestBody(required = false) PedidoAmostra pedido) {
        return criado(publicador.publicar(fabrica.amostraRecebida(pedido), false));
    }

    @PostMapping("/amostra-rejeitada")
    public ResponseEntity<Evento> amostraRejeitada(@RequestBody(required = false) PedidoAmostra pedido) {
        return criado(publicador.publicar(fabrica.amostraRejeitada(pedido), false));
    }

    // Sequência de laudos liberados distintos (demonstração das duas instâncias de Indicadores)
    @PostMapping("/lote/laudos-liberados")
    public ResponseEntity<List<Evento>> loteLaudosLiberados(@RequestParam(defaultValue = "10") int quantidade) {
        if (quantidade < 1 || quantidade > 100) {
            return ResponseEntity.badRequest().build();
        }
        List<Evento> eventos = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            eventos.add(publicador.publicar(fabrica.laudoLiberado(null), false));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(eventos);
    }

    // Evento de um tipo qualquer "familia.acao" (ex.: amostra.descartada) para a pergunta do "evento novo"
    @PostMapping("/livre")
    public ResponseEntity<?> livre(@RequestBody PedidoEventoLivre pedido) {
        if (pedido == null || pedido.tipo() == null || !pedido.tipo().matches("[a-z]+\\.[a-z_]+")) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe o tipo no formato familia.acao, ex.: amostra.descartada"));
        }
        return criado(publicador.publicar(fabrica.livre(pedido.tipo(), pedido.dados()), false));
    }

    // Republica um evento já publicado com o MESMO eventoId (teste de duplicidade)
    @PostMapping("/{eventoId}/republicar")
    public ResponseEntity<?> republicar(@PathVariable String eventoId) {
        Evento evento = publicador.buscarPublicado(eventoId);
        if (evento == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Evento " + eventoId + " não foi publicado por este produtor"));
        }
        return ResponseEntity.ok(publicador.publicar(evento, true));
    }

    @GetMapping
    public List<JsonNode> listar() {
        return publicador.listarPublicacoes();
    }

    private static <T> ResponseEntity<T> criado(T corpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(corpo);
    }
}
