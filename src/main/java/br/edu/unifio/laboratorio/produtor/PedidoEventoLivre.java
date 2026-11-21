package br.edu.unifio.laboratorio.produtor;

import br.edu.unifio.laboratorio.contrato.DadosEvento;

/**
 * Publica um tipo de evento qualquer (ex.: "amostra.descartada" ou "laudo.revisado").
 * Serve para mostrar como a topologia trata um fato novo.
 */
public record PedidoEventoLivre(String tipo, DadosEvento dados) {
}
