package br.edu.unifio.laboratorio.contrato;

import java.time.OffsetDateTime;

/**
 * Contrato JSON de todos os eventos publicados no RabbitMQ.
 *
 * {
 *   "eventoId": "7f1c2a9e-...",          identificador único (UUID); repetido só na republicação de teste
 *   "tipo": "laudo.liberado",            tipo do fato = routing key
 *   "ocorridoEm": "2026-11-21T10:15:30-03:00",
 *   "origem": "produtor-laboratorio",
 *   "dados": { "laudoId": "LAU-1001", "exame": "HEMOGRAMA", "pacienteCodigo": "PAC-0042" }
 * }
 */
public record Evento(
        String eventoId,
        String tipo,
        OffsetDateTime ocorridoEm,
        String origem,
        DadosEvento dados) {
}
