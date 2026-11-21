package br.edu.unifio.laboratorio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import br.edu.unifio.laboratorio.comum.RegistroJsonl;
import br.edu.unifio.laboratorio.contrato.DadosEvento;
import br.edu.unifio.laboratorio.contrato.Evento;
import br.edu.unifio.laboratorio.contrato.TiposEvento;

/**
 * Testes que não dependem do RabbitMQ: formato do contrato JSON e idempotência do registro.
 */
class ContratoERegistroTests {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Evento laudoLiberado() {
        return new Evento("evt-123", TiposEvento.LAUDO_LIBERADO, OffsetDateTime.parse("2026-11-21T10:15:30-03:00"),
                "produtor-laboratorio", new DadosEvento("LAU-1001", null, "HEMOGRAMA", "PAC-0042", null, null));
    }

    @Test
    void contratoJsonTemOsCamposCombinados() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(laudoLiberado()));

        assertEquals("evt-123", json.get("eventoId").asText());
        assertEquals("laudo.liberado", json.get("tipo").asText());
        assertEquals("2026-11-21T10:15:30-03:00", json.get("ocorridoEm").asText());
        assertEquals("LAU-1001", json.get("dados").get("laudoId").asText());
        assertFalse(json.get("dados").has("motivo")); // campos nulos não aparecem
    }

    @Test
    void contratoJsonVoltaParaOMesmoEvento() throws Exception {
        Evento original = laudoLiberado();
        Evento lido = mapper.readValue(mapper.writeValueAsString(original), Evento.class);

        assertEquals(original.eventoId(), lido.eventoId());
        assertEquals(original.dados(), lido.dados());
        assertTrue(original.ocorridoEm().isEqual(lido.ocorridoEm()));
    }

    @Test
    void registroIgnoraOMesmoEventoId(@TempDir Path pasta) {
        RegistroJsonl registro = new RegistroJsonl(pasta.resolve("portal.jsonl"), mapper);

        boolean primeira = registro.registrar("evt-123", true, dup -> Map.of("eventoId", "evt-123", "laudoId", "LAU-1001"));
        boolean repetida = registro.registrar("evt-123", true, dup -> Map.of("eventoId", "evt-123", "laudoId", "LAU-1001"));

        assertTrue(primeira);
        assertFalse(repetida);
        assertEquals(1, registro.ler().size());
    }

    @Test
    void registroDeAuditoriaGuardaARepeticaoMarcada(@TempDir Path pasta) {
        RegistroJsonl registro = new RegistroJsonl(pasta.resolve("auditoria.jsonl"), mapper);

        registro.registrar("evt-123", false, dup -> Map.of("eventoId", "evt-123", "duplicado", dup));
        registro.registrar("evt-123", false, dup -> Map.of("eventoId", "evt-123", "duplicado", dup));

        assertEquals(2, registro.ler().size());
        assertFalse(registro.ler().get(0).get("duplicado").asBoolean());
        assertTrue(registro.ler().get(1).get("duplicado").asBoolean());
    }
}
