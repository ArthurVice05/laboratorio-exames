package br.edu.unifio.laboratorio.config;

import static br.edu.unifio.laboratorio.config.Topologia.*;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Topologia completa, declarada por TODAS as aplicações ao conectar no broker.
 * A declaração é idempotente: se já existe igual, nada muda. Assim a configuração
 * é reproduzível e as filas existem mesmo que um consumidor nunca tenha sido iniciado.
 *
 * Exchange "laboratorio.eventos" (topic, durável)
 *   lab.portal-resultados  <- laudo.liberado
 *   lab.comunicacao        <- laudo.liberado, laudo.invalidado, amostra.rejeitada
 *   lab.triagem            <- amostra.*          (toda a família "amostra", inclusive fatos novos)
 *   lab.auditoria          <- #                  (todos os eventos)
 *   lab.indicadores        <- laudo.liberado     (fila única; 2 instâncias competem por ela)
 */
@Configuration
public class RabbitConfig {

    @Bean
    public Declarables topologia() {
        TopicExchange eventos = ExchangeBuilder.topicExchange(EXCHANGE_EVENTOS).durable(true).build();
        FanoutExchange dlx = ExchangeBuilder.fanoutExchange(EXCHANGE_DLX).durable(true).build();
        Queue dlq = QueueBuilder.durable(FILA_DLQ).build();

        Queue portal = fila(FILA_PORTAL);
        Queue comunicacao = fila(FILA_COMUNICACAO);
        Queue triagem = fila(FILA_TRIAGEM);
        Queue auditoria = fila(FILA_AUDITORIA);
        Queue indicadores = fila(FILA_INDICADORES);

        return new Declarables(
                eventos, dlx, dlq,
                BindingBuilder.bind(dlq).to(dlx),
                portal, comunicacao, triagem, auditoria, indicadores,
                vinculo(portal, eventos, "laudo.liberado"),
                vinculo(comunicacao, eventos, "laudo.liberado"),
                vinculo(comunicacao, eventos, "laudo.invalidado"),
                vinculo(comunicacao, eventos, "amostra.rejeitada"),
                vinculo(triagem, eventos, "amostra.*"),
                vinculo(auditoria, eventos, "#"),
                vinculo(indicadores, eventos, "laudo.liberado"));
    }

    // Fila durável; mensagem rejeitada (nack sem requeue) vai para a DLQ
    private Queue fila(String nome) {
        return QueueBuilder.durable(nome).deadLetterExchange(EXCHANGE_DLX).build();
    }

    private Binding vinculo(Queue fila, TopicExchange exchange, String padrao) {
        return BindingBuilder.bind(fila).to(exchange).with(padrao);
    }

    // Mensagens em JSON (usa o ObjectMapper do Spring, com suporte a datas)
    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter(objectMapper);
        // Converte para o tipo do parâmetro do listener (Evento), sem depender do nome da classe no cabeçalho
        conversor.setAlwaysConvertToInferredType(true);
        return conversor;
    }
}
