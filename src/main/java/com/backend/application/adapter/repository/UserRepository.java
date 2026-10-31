package com.backend.application.adapter.repository;



public interface UserRepository {
    boolean existsByEmail(String email);
}
