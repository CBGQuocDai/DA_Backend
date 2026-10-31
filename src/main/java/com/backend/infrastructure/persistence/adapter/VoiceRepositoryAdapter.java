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
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    private String normalizeString(String key) {
        if (key == null || key.isBlank()) {
            return "%%"; // Hoặc trả về "" tùy thuộc vào logic query của bạn
        }

        // 1. Trim khoảng trắng thừa ở 2 đầu
        // 2. Escape dấu '\' trước tiên (để tránh escape chồng chéo lên các dấu '\' thêm vào sau)
        // 3. Escape dấu '%' và '_'
        String escaped = key.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        // 4. Bọc chuỗi trong cặp dấu '%' vì method findAllByNameLike của JPA
        // KHÔNG tự động thêm '%' giống như findAllByNameContaining
        return "%" + escaped + "%";
    }

    @Override
    public PageResponse<Voice> findAllByNameLike(String voiceName, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        String voiceNameNormal = normalizeString(voiceName);
        Page<JpaVoiceEntity> pageResult = jpaVoiceRepository.findAllByNameLike(voiceNameNormal,pageRequest);
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
