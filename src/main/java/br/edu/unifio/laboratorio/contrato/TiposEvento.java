package br.edu.unifio.laboratorio.contrato;

/**
 * Tipos de evento no formato "familia.acao". O tipo também é a routing key no RabbitMQ.
 */
public final class TiposEvento {

    // Processo principal: laudos
    public static final String LAUDO_LIBERADO = "laudo.liberado";
    public static final String LAUDO_INVALIDADO = "laudo.invalidado";

    // Processo especializado: amostras (triagem)
    public static final String AMOSTRA_RECEBIDA = "amostra.recebida";
    public static final String AMOSTRA_REJEITADA = "amostra.rejeitada";

    private TiposEvento() {
    }
}
