package com.backend.application.adapter.security;

import com.backend.domain.model.User;

public interface TokenProvider {
    String generateToken(User u);

    boolean validateToken(String token);

    TokenInfo parseToken(String token);

    /** Blacklists the token's id until it would have expired; invalid tokens are ignored. */
    void revoke(String token);

    boolean isRevoked(String token);

    record TokenInfo(Long userId, String email, String role) {}
}
