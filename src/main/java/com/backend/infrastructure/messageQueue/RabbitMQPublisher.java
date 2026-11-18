package com.backend.infrastructure.messageQueue;

import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.messaging.GenerateAudioRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPublisher implements MessageQueue {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void send(GenerateAudioRequest job) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.JOB_QUEUE, job);
        log.info("Sent TTS job for chapter {}", job.getChapterId());
    }

    @Override
    public void cancel(String jobId) {
        // Fanout: mọi worker đang chạy đều nhận được lệnh hủy
        rabbitTemplate.convertAndSend(RabbitMqConfig.CANCEL_EXCHANGE, "", Map.of("jobId", jobId));
        log.info("Cancelled TTS job {}", jobId);
    }
}
