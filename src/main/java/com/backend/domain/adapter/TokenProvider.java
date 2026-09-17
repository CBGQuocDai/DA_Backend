package com.backend.domain.adapter;

import com.backend.domain.model.User;

public interface TokenProvider {
    String generateToken(User u);

    boolean validateToken(String token);

    TokenInfo parseToken(String token);

    record TokenInfo(Long userId, String email, String role) {}
}
