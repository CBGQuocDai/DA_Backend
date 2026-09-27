package com.backend.domain.adapter.repository;

import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Voice;

public interface VoiceRepository {
    PageResponse<Voice> findAll(int page, int size);
    Voice save(Voice v);
    void delete(Voice v);
}
