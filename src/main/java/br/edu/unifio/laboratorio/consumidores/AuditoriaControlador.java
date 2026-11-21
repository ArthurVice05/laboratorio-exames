package br.edu.unifio.laboratorio.consumidores;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.unifio.laboratorio.comum.Armazenamento;
import br.edu.unifio.laboratorio.comum.RegistroJsonl;

/**
 * Histórico da Auditoria (porta 8084), consultável por identificador, tipo e horário.
 * Ex.: /auditoria?tipo=laudo.liberado
 *      /auditoria?eventoId=7f1c2a9e-...
 *      /auditoria?de=2026-11-21T08:00:00-03:00&ate=2026-11-21T12:00:00-03:00
 */
@RestController
@RequestMapping("/auditoria")
@Profile("auditoria")
public class AuditoriaControlador {

    private final RegistroJsonl historico;

    public AuditoriaControlador(Armazenamento armazenamento) {
        this.historico = armazenamento.registro("auditoria-historico.jsonl");
    }

    @GetMapping
    public List<JsonNode> consultar(
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return historico.ler().stream()
                .filter(r -> eventoId == null || eventoId.equals(r.path("eventoId").asText()))
                .filter(r -> tipo == null || tipo.equals(r.path("tipo").asText()))
                .filter(r -> de == null || !ocorridoEm(r).isBefore(de))
                .filter(r -> ate == null || !ocorridoEm(r).isAfter(ate))
                .toList();
    }

    // Quantidade de registros por tipo de evento
    @GetMapping("/resumo")
    public Map<String, Integer> resumo() {
        Map<String, Integer> porTipo = new TreeMap<>();
        for (JsonNode registro : historico.ler()) {
            porTipo.merge(registro.path("tipo").asText(), 1, Integer::sum);
        }
        return porTipo;
    }

    private OffsetDateTime ocorridoEm(JsonNode registro) {
        return OffsetDateTime.parse(registro.path("ocorridoEm").asText());
    }
}
