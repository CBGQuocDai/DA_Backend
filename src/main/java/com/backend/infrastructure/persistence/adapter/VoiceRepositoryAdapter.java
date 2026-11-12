package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.VoiceRepository;
import com.backend.application.dto.response.PageResponse;
import com.backend.infrastructure.persistence.mapper.VoiceMapper;
import com.backend.domain.model.Voice;
import com.backend.infrastructure.persistence.entity.JpaVoiceEntity;
import com.backend.infrastructure.persistence.repository.JpaVoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.backend.infrastructure.persistence.util.LikePatterns;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VoiceRepositoryAdapter implements VoiceRepository {

    private final JpaVoiceRepository jpaVoiceRepository;
    private final VoiceMapper voiceMapper;

    @Override
    public PageResponse<Voice> findAllByNameLike(String voiceName, int page, int size) {
        // Sắp xếp theo ID giảm dần để thứ tự phân trang ổn định.
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String voiceNameNormal = LikePatterns.contains(voiceName);
        Page<JpaVoiceEntity> pageResult = jpaVoiceRepository.findAllByNameLike(voiceNameNormal,pageRequest);
        return LikePatterns.toPageResponse(pageResult, voiceMapper::toDomain);
    }
    @Override
    public Voice save(Voice v) {
        JpaVoiceEntity voice = voiceMapper.toEntity(v);
        return voiceMapper.toDomain(jpaVoiceRepository.save(voice));
    }

    @Override
    public Voice update(Voice v) {
        int affected = jpaVoiceRepository.updateVoice(
                v.getId(), v.getName(), v.getDescription(), v.getExampleAudio());
        if (affected == 0) {
            throw new RuntimeException("Voice not found with id: " + v.getId());
        }
        return v;
    }

    @Override
    public void delete(Voice v) {
        jpaVoiceRepository.delete(voiceMapper.toEntity(v));
    }

    @Override
    public Optional<Voice> findById(Long id) {
        return jpaVoiceRepository.findById(id).map(voiceMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaVoiceRepository.deleteById(id);
    }
}
