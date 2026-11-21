package br.edu.unifio.laboratorio.config;

/**
 * Nomes da topologia no RabbitMQ (usados pelo produtor e pelos consumidores).
 */
public final class Topologia {

    // Exchange principal do tipo TOPIC: roteia pela routing key "familia.acao"
    public static final String EXCHANGE_EVENTOS = "laboratorio.eventos";

    // Para onde vão as mensagens rejeitadas (falha no processamento)
    public static final String EXCHANGE_DLX = "laboratorio.dlx";
    public static final String FILA_DLQ = "laboratorio.dlq";

    // Uma fila por responsabilidade de consumo
    public static final String FILA_PORTAL = "lab.portal-resultados";
    public static final String FILA_COMUNICACAO = "lab.comunicacao";
    public static final String FILA_TRIAGEM = "lab.triagem";
    public static final String FILA_AUDITORIA = "lab.auditoria";
    // Uma única fila compartilhada pelas duas instâncias de Indicadores (consumidores concorrentes)
    public static final String FILA_INDICADORES = "lab.indicadores";

    private Topologia() {
    }
}
