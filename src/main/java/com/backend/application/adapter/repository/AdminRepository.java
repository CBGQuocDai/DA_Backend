package com.backend.application.adapter.repository;

import com.backend.domain.model.Admin;

import java.util.Optional;

public interface AdminRepository {
    Optional<Admin> findByEmail(String email);
    Optional<Admin> findById(Long id);
    Admin save(Admin admin);
}
