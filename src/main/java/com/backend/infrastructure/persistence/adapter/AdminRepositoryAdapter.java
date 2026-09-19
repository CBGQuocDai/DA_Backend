package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.mapper.AdminMapper;
import com.backend.domain.model.Admin;
import com.backend.domain.adapter.repository.AdminRepository;
import com.backend.infrastructure.persistence.repository.JpaAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminRepositoryAdapter implements AdminRepository {

    private final JpaAdminRepository jpaAdminRepository;
    private final AdminMapper adminMapper;

    @Override
    public Optional<Admin> findByEmail(String email) {
        return jpaAdminRepository.findByEmail(email)
                .map(adminMapper::toDomain);
    }
}
