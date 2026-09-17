package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.mapper.UserMapper;
import com.backend.domain.model.User;
import com.backend.domain.adapter.repository.UserRepository;
import com.backend.infrastructure.persistence.repository.JpaAdminRepository;
import com.backend.infrastructure.persistence.repository.JpaCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaAdminRepository jpaAdminRepository;
    private final JpaCustomerRepository jpaCustomerRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByEmail(String email) {
        Optional<User> user = jpaAdminRepository.findByEmail(email)
                .map(userMapper::toDomain);
        if (user.isPresent()) {
            return user;
        }
        return jpaCustomerRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }
}
