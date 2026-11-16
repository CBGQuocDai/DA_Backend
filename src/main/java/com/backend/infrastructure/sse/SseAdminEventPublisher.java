package com.backend.infrastructure.sse;

import com.backend.application.adapter.realtime.AdminEventPublisher;
import com.backend.application.adapter.realtime.AdminEventStream;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.ChapterStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Giữ các kết nối SSE của admin trong bộ nhớ và phát event tới tất cả.
 * Chỉ đúng khi backend chạy một instance: event phát ở instance nào thì chỉ admin nối vào instance đó nhận được.
 */
@Component
@Slf4j
public class SseAdminEventPublisher implements AdminEventPublisher, AdminEventStream {

    static final String BOOK_STATUS_EVENT = "book-status";
    static final String CHAPTER_STATUS_EVENT = "chapter-status";
    private static final long HEARTBEAT_INTERVAL_MS = 25_000;

    private record BookStatusPayload(Long bookId, BookStatus status) {
    }

    private record ChapterStatusPayload(Long bookId, Long chapterId, ChapterStatus status, String audioUrl,
                                        Integer duration) {
    }

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public SseEmitter subscribe() {
        // Không đặt timeout: kết nối chết được phát hiện qua heartbeat
        return register(new SseEmitter(0L));
    }

    SseEmitter register(SseEmitter emitter) {
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    int connectionCount() {
        return emitters.size();
    }

    @Override
    public void bookStatusChanged(Long bookId, BookStatus status) {
        BookStatusPayload payload = new BookStatusPayload(bookId, status);
        broadcast(() -> SseEmitter.event().name(BOOK_STATUS_EVENT).data(payload, MediaType.APPLICATION_JSON));
    }

    @Override
    public void chapterStatusChanged(Long bookId, Long chapterId, ChapterStatus status, String audioUrl,
                                     Integer duration) {
        ChapterStatusPayload payload = new ChapterStatusPayload(bookId, chapterId, status, audioUrl, duration);
        broadcast(() -> SseEmitter.event().name(CHAPTER_STATUS_EVENT).data(payload, MediaType.APPLICATION_JSON));
    }

    /** Comment định kỳ để proxy không cắt kết nối rảnh và để dọn các kết nối đã chết. */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    void heartbeat() {
        broadcast(() -> SseEmitter.event().comment("ping"));
    }

    // Mỗi emitter cần một builder riêng; lỗi gửi chỉ gỡ emitter đó, không ảnh hưởng luồng nghiệp vụ
    private void broadcast(Supplier<SseEventBuilder> event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event.get());
            } catch (IOException | RuntimeException e) {
                emitters.remove(emitter);
                log.debug("Dropping closed SSE connection: {}", e.getMessage());
            }
        }
    }
}
