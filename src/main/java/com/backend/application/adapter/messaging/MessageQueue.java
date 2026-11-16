package com.backend.application.adapter.messaging;

public interface MessageQueue {
    void send(GenerateAudioRequest job);

    /** Báo worker bỏ job có mã {@code jobId}: chưa chạy thì bỏ qua, đang chạy thì không upload kết quả. */
    void cancel(String jobId);
}
