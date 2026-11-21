package br.edu.unifio.laboratorio.consumidores;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.unifio.laboratorio.comum.Armazenamento;
import br.edu.unifio.laboratorio.comum.RegistroJsonl;

/** Consulta do Portal de resultados (porta 8081). */
@RestController
@RequestMapping("/portal")
@Profile("portal")
public class PortalControlador {

    private final RegistroJsonl publicacoes;
    private final RegistroJsonl duplicatas;

    public PortalControlador(Armazenamento armazenamento) {
        this.publicacoes = armazenamento.registro("portal-publicacoes.jsonl");
        this.duplicatas = armazenamento.registro("portal-duplicatas-ignoradas.jsonl");
    }

    @GetMapping("/laudos")
    public List<JsonNode> laudos() {
        return publicacoes.ler();
    }

    @GetMapping("/duplicatas")
    public List<JsonNode> duplicatasIgnoradas() {
        return duplicatas.ler();
    }

    // Quantas vezes cada laudo foi publicado (deve ser sempre 1)
    @GetMapping("/resumo")
    public Map<String, Object> resumo() {
        Map<String, Integer> publicacoesPorLaudo = new LinkedHashMap<>();
        for (JsonNode laudo : publicacoes.ler()) {
            publicacoesPorLaudo.merge(laudo.path("laudoId").asText(), 1, Integer::sum);
        }
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("laudosPublicados", publicacoesPorLaudo.size());
        resumo.put("eventosRepetidosIgnorados", duplicatas.ler().size());
        resumo.put("publicacoesPorLaudo", publicacoesPorLaudo);
        return resumo;
    }
}
