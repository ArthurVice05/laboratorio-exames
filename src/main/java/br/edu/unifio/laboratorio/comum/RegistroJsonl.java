package br.edu.unifio.laboratorio.comum;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Registro estruturado em arquivo JSON Lines (um JSON por linha) na pasta "dados/".
 * É o "efeito verificável" de cada consumidor: pode ser consultado pela API ou com cat.
 *
 * As escritas usam trava de arquivo (FileLock), que vale entre processos diferentes.
 * Por isso as duas instâncias de Indicadores podem gravar no mesmo arquivo sem duplicar
 * nem corromper linhas, e a checagem "esse eventoId já foi processado?" é atômica.
 */
public class RegistroJsonl {

    private final Path arquivo;
    private final ObjectMapper mapper;

    public RegistroJsonl(Path arquivo, ObjectMapper mapper) {
        this.arquivo = arquivo;
        this.mapper = mapper;
    }

    /**
     * Grava um registro do evento.
     *
     * @param eventoId      identificador usado na checagem de duplicidade
     * @param somenteSeNovo true = não grava se o eventoId já existe no arquivo (idempotência)
     * @param montar        recebe "duplicado?" e devolve o objeto a gravar
     * @return true se o evento era inédito neste registro
     */
    public synchronized boolean registrar(String eventoId, boolean somenteSeNovo, Function<Boolean, Object> montar) {
        try {
            Files.createDirectories(arquivo.toAbsolutePath().getParent());
            try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.CREATE,
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
                 FileLock trava = canal.lock()) {

                boolean duplicado = eventoId != null && contem(lerLinhas(canal), eventoId);
                if (duplicado && somenteSeNovo) {
                    return false;
                }
                String linha = mapper.writeValueAsString(montar.apply(duplicado)) + "\n";
                canal.write(ByteBuffer.wrap(linha.getBytes(StandardCharsets.UTF_8)), canal.size());
                canal.force(true); // garante que está no disco ANTES do ack
                return !duplicado;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar em " + arquivo, e);
        }
    }

    public synchronized List<JsonNode> ler() {
        if (!Files.exists(arquivo)) {
            return List.of();
        }
        try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.READ);
             FileLock trava = canal.lock(0, Long.MAX_VALUE, true)) {
            List<JsonNode> registros = new ArrayList<>();
            for (String linha : lerLinhas(canal)) {
                registros.add(mapper.readTree(linha));
            }
            return registros;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + arquivo, e);
        }
    }

    private List<String> lerLinhas(FileChannel canal) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) canal.size());
        long posicao = 0;
        while (buffer.hasRemaining()) {
            int lidos = canal.read(buffer, posicao);
            if (lidos < 0) {
                break;
            }
            posicao += lidos;
        }
        String conteudo = new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
        List<String> linhas = new ArrayList<>();
        for (String linha : conteudo.split("\n")) {
            if (!linha.isBlank()) {
                linhas.add(linha);
            }
        }
        return linhas;
    }

    private boolean contem(List<String> linhas, String eventoId) throws IOException {
        for (String linha : linhas) {
            if (eventoId.equals(mapper.readTree(linha).path("eventoId").asText())) {
                return true;
            }
        }
        return false;
    }
}
