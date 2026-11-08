package com.backend.application.adapter.repository;

import com.backend.domain.model.User;

import java.util.Optional;

/** Operations shared by every account type, whatever its role. */
public interface UserRepository {
    boolean existsByEmail(String email);

    /** The account as its concrete type (Admin or Customer). */
    Optional<User> findById(Long id);

    void updatePassword(Long id, String encodedPassword);
}
