package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.VoiceRepository;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.mapper.VoiceMapper;
import com.backend.domain.model.Voice;
import com.backend.infrastructure.persistence.entity.JpaVoiceEntity;
import com.backend.infrastructure.persistence.repository.JpaVoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VoiceRepositoryAdapter implements VoiceRepository {

    private final JpaVoiceRepository jpaVoiceRepository;
    private final VoiceMapper voiceMapper;

    @Override
    public PageResponse<Voice> findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<JpaVoiceEntity> pageResult = jpaVoiceRepository.findAll(pageRequest);
        return PageResponse.<Voice>builder()
                .page(pageRequest.getPageNumber())
                .pageSize(pageRequest.getPageSize())
                .total((int) pageResult.getTotalElements())
                .totalPage(pageResult.getTotalPages())
                .content(pageResult.getContent().stream().map(voiceMapper::toDomain).toList())
                .build();
    }

    @Override
    public Voice save(Voice v) {
        JpaVoiceEntity voice = voiceMapper.toEntity(v);
        return voiceMapper.toDomain(jpaVoiceRepository.save(voice));
    }

    @Override
    public void delete(Voice v) {

    }
}
