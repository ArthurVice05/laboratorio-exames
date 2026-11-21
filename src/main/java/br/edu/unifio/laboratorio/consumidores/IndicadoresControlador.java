package br.edu.unifio.laboratorio.consumidores;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.unifio.laboratorio.comum.Armazenamento;
import br.edu.unifio.laboratorio.comum.RegistroJsonl;

/**
 * Indicadores (porta 8085 na instância 1 e 8086 na instância 2).
 * As duas leem o mesmo registro, então mostram o mesmo total.
 */
@RestController
@RequestMapping("/indicadores")
@Profile("indicadores")
public class IndicadoresControlador {

    private final RegistroJsonl contabilizados;
    private final String instancia;

    public IndicadoresControlador(Armazenamento armazenamento, @Value("${app.instancia}") String instancia) {
        this.contabilizados = armazenamento.registro("indicadores-laudos.jsonl");
        this.instancia = instancia;
    }

    @GetMapping
    public Map<String, Object> totais() {
        List<JsonNode> registros = contabilizados.ler();
        Map<String, Integer> porExame = new TreeMap<>();
        Map<String, Integer> porInstancia = new TreeMap<>();
        for (JsonNode r : registros) {
            porExame.merge(r.path("exame").asText(), 1, Integer::sum);
            porInstancia.merge(r.path("instancia").asText(), 1, Integer::sum);
        }
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("consultadoNa", instancia);
        resposta.put("totalLaudosLiberados", registros.size());
        resposta.put("laudosLiberadosPorExame", porExame);
        resposta.put("processadosPorInstancia", porInstancia);
        return resposta;
    }

    // Lista de cada evento com a instância que o processou
    @GetMapping("/processamentos")
    public List<JsonNode> processamentos() {
        return contabilizados.ler();
    }
}
