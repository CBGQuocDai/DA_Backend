package com.backend.domain.adapter;

public interface PasswordEncoder {
    boolean matches(String rawPassword, String encodedPassword);
}
