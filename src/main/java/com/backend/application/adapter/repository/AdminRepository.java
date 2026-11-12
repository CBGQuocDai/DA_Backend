package com.backend.application.adapter.repository;

import com.backend.domain.model.Admin;

import java.util.Optional;

public interface AdminRepository {
    /** Tìm tài khoản theo địa chỉ email. */
    Optional<Admin> findByEmail(String email);
    /** Tìm quản trị viên theo ID; trả về Optional rỗng nếu không tồn tại. */
    Optional<Admin> findById(Long id);
}
