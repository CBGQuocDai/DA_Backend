package com.backend.application.adapter.messaging;

public interface MessageQueue {
    void send(GenerateAudioRequest job);
}
