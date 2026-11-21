package br.edu.unifio.laboratorio.produtor;

/**
 * Corpo opcional para disparar eventos de amostra. Campo ausente = valor fictício gerado.
 */
public record PedidoAmostra(String amostraId, String lote, String exame, String motivo) {
}
