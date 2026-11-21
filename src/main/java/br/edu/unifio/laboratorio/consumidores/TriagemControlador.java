package br.edu.unifio.laboratorio.consumidores;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.unifio.laboratorio.comum.Armazenamento;
import br.edu.unifio.laboratorio.comum.RegistroJsonl;

/** Consulta da Triagem (porta 8083). */
@RestController
@RequestMapping("/triagem")
@Profile("triagem")
public class TriagemControlador {

    private final RegistroJsonl registros;

    public TriagemControlador(Armazenamento armazenamento) {
        this.registros = armazenamento.registro("triagem-registros.jsonl");
    }

    @GetMapping("/registros")
    public List<JsonNode> registros() {
        return registros.ler();
    }
}
