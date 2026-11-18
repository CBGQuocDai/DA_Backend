package com.backend.infrastructure.messageQueue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "tts";
    public static final String JOB_QUEUE = "tts.jobs";
    public static final String RESULT_QUEUE = "tts.results";
    public static final String DEAD_LETTER_EXCHANGE = "tts.dlx";
    public static final String RESULT_DLQ = "tts.results.dlq";
    /** Lệnh hủy job; mỗi worker tự tạo một queue riêng gắn vào exchange này. */
    public static final String CANCEL_EXCHANGE = "tts.cancel";

    @Bean
    public DirectExchange ttsExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue jobQueue() {
        return new Queue(JOB_QUEUE, true);
    }

    @Bean
    public Binding jobBinding(Queue jobQueue, DirectExchange ttsExchange) {
        return BindingBuilder.bind(jobQueue).to(ttsExchange).with(JOB_QUEUE);
    }

    @Bean
    public FanoutExchange cancelExchange() {
        return new FanoutExchange(CANCEL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue resultDlq() {
        return new Queue(RESULT_DLQ, true);
    }

    @Bean
    public Binding resultDlqBinding(Queue resultDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(resultDlq).to(deadLetterExchange).with(RESULT_QUEUE);
    }

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    @Bean
    public Binding resultBinding(Queue resultQueue, DirectExchange ttsExchange) {
        return BindingBuilder.bind(resultQueue).to(ttsExchange).with(RESULT_QUEUE);
    }

    // Jackson 3 JSON; Boot wires it into RabbitTemplate and the listener container factories
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
