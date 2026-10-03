package com.backend.domain.adapter.repository;

import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.model.Voice;

import java.util.Optional;

public interface VoiceRepository {
    PageResponse<Voice> findAll(int page, int size);
    Voice save(Voice v);
    void delete(Voice v);
    Optional<Voice> findById(Long id);
    void deleteById(Long id);
}
