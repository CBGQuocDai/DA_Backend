package com.backend.domain.repository;

public interface UserRepository {
    boolean existsByEmail(String email);
}
