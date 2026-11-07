package com.backend.infrastructure.persistence.adapter;

import com.backend.application.adapter.repository.UserRepository;
import com.backend.domain.model.User;
import com.backend.infrastructure.persistence.entity.JpaAdminEntity;
import com.backend.infrastructure.persistence.entity.JpaCustomerEntity;
import com.backend.infrastructure.persistence.entity.JpaUserEntity;
import com.backend.infrastructure.persistence.mapper.AdminMapper;
import com.backend.infrastructure.persistence.mapper.CustomerMapper;
import com.backend.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final AdminMapper adminMapper;
    private final CustomerMapper customerMapper;

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id).map(this::toDomain);
    }

    @Override
    public void updatePassword(Long id, String encodedPassword) {
        jpaUserRepository.updatePassword(id, encodedPassword);
    }

    private User toDomain(JpaUserEntity entity) {
        if (entity instanceof JpaAdminEntity admin) {
            return adminMapper.toDomain(admin);
        }
        if (entity instanceof JpaCustomerEntity customer) {
            return customerMapper.toDomain(customer);
        }
        throw new IllegalStateException("Unknown account type: " + entity.getClass().getName());
    }
}
