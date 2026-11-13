package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Voice;

import java.util.Optional;

public interface VoiceRepository {

    /** Tìm giọng đọc theo tên không phân biệt hoa thường, phân trang với ID giảm dần. */
    PageResponse<Voice> findAllByNameLike(String voiceName, int page, int size);
    /** Lưu dữ liệu và trả về giọng đọc đã lưu. */
    Voice save(Voice v);
    /** Cập nhật dữ liệu của giọng đọc hiện có. */
    Voice update(Voice v);
    /** Xóa giọng đọc được chỉ định. */
    void delete(Voice v);
    /** Tìm giọng đọc theo ID; trả về Optional rỗng nếu không tồn tại. */
    Optional<Voice> findById(Long id);
    /** Xóa giọng đọc theo ID. */
    void deleteById(Long id);
}
