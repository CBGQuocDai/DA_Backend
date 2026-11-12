package com.backend.application.adapter.repository;

import com.backend.domain.model.Customer;

import java.util.Optional;

public interface CustomerRepository {
    /** Tìm tài khoản theo địa chỉ email. */
    Optional<Customer> findByEmail(String email);
    /** Lưu dữ liệu và trả về khách hàng đã lưu. */
    Customer save(Customer customer);
    /** Tìm khách hàng theo ID; trả về Optional rỗng nếu không tồn tại. */
    Optional<Customer> findById(Long id);

    /** Đếm tổng số khách hàng. */
    long count();
}
