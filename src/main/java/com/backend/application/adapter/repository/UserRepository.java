package com.backend.application.adapter.repository;

import com.backend.domain.model.User;

import java.util.Optional;

/** Các thao tác tài khoản dùng chung cho quản trị viên và khách hàng. */
public interface UserRepository {
    /** Kiểm tra địa chỉ email đã được tài khoản nào sử dụng hay chưa. */
    boolean existsByEmail(String email);

    /** Tìm tài khoản theo ID; trả về Optional rỗng nếu không tồn tại. */
    Optional<User> findById(Long id);

    /** Cập nhật mật khẩu đã mã hóa của tài khoản. */
    void updatePassword(Long id, String encodedPassword);
}
