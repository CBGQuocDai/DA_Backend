package com.backend.domain.repository;

import com.backend.domain.model.Admin;

import java.util.Optional;

public interface AdminRepository {
    Optional<Admin> findByEmail(String email);
}
