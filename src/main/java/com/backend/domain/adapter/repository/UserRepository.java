package com.backend.domain.adapter.repository;

import com.backend.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    boolean existsByEmail(String email);
}
