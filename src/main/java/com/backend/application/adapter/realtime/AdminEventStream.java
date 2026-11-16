package com.backend.application.adapter.realtime;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Mở một kết nối SSE nhận các event mà {@link AdminEventPublisher} phát ra. */
public interface AdminEventStream {

    SseEmitter subscribe();
}
