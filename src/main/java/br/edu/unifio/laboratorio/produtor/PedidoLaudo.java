package br.edu.unifio.laboratorio.produtor;

/**
 * Corpo opcional para disparar eventos de laudo. Campo ausente = valor fictício gerado.
 */
public record PedidoLaudo(String laudoId, String exame, String pacienteCodigo, String motivo) {
}
