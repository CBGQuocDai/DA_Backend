package com.backend.infrastructure.messageQueue;

import com.backend.application.service.ChapterService;
import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AudioResultListener {

    private final ChapterService chapterService;

    // Payload type is inferred from the parameter, so the worker does not need a __TypeId__ header
    @RabbitListener(queues = RabbitMqConfig.RESULT_QUEUE)
    void onResult(GenerateAudioResult result) {
        chapterService.handleAudioResult(result);
    }
}
