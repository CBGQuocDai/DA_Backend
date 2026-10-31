package com.backend.application.adapter.audio;

public interface AudioConverter {
    /** Throws a business error when {@code mp3} cannot be decoded. */
    byte[] mp3ToWav(byte[] mp3);
}
