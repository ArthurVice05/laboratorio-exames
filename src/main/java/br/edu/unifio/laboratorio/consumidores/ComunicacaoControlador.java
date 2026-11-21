package br.edu.unifio.laboratorio.consumidores;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.unifio.laboratorio.comum.Armazenamento;
import br.edu.unifio.laboratorio.comum.RegistroJsonl;

/** Consulta da Comunicação (porta 8082). */
@RestController
@RequestMapping("/comunicacao")
@Profile("comunicacao")
public class ComunicacaoControlador {

    private final RegistroJsonl avisos;

    public ComunicacaoControlador(Armazenamento armazenamento) {
        this.avisos = armazenamento.registro("comunicacao-avisos.jsonl");
    }

    @GetMapping("/avisos")
    public List<JsonNode> avisos() {
        return avisos.ler();
    }
}
