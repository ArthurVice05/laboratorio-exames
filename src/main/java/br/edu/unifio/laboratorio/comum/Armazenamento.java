package br.edu.unifio.laboratorio.comum;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cria os registros JSON Lines dentro da pasta configurada em app.dados-dir (padrão: dados/).
 */
@Component
public class Armazenamento {

    private final Path pasta;
    private final ObjectMapper mapper;
    // Uma instância por arquivo: consumidor e controlador do mesmo processo compartilham a mesma trava
    private final Map<Path, RegistroJsonl> registros = new ConcurrentHashMap<>();

    public Armazenamento(@Value("${app.dados-dir:dados}") String pasta, ObjectMapper mapper) {
        this.pasta = Path.of(pasta);
        this.mapper = mapper;
    }

    public RegistroJsonl registro(String nomeArquivo) {
        return registros.computeIfAbsent(pasta.resolve(nomeArquivo).toAbsolutePath(), p -> new RegistroJsonl(p, mapper));
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
