package br.edu.unifio.laboratorio.contrato;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Dados de negócio do evento. Cada tipo usa só os campos de que precisa;
 * os campos nulos não aparecem no JSON.
 *
 * Laudo:   laudoId, exame, pacienteCodigo (+ motivo na invalidação)
 * Amostra: amostraId, lote, exame (+ motivo na rejeição)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DadosEvento(
        String laudoId,
        String amostraId,
        String exame,
        String pacienteCodigo,
        String lote,
        String motivo) {
}
