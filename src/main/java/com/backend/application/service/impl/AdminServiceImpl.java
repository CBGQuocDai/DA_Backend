package com.backend.application.service.impl;

import com.backend.application.service.AdminService;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.adapter.security.TokenProvider;
import com.backend.application.adapter.repository.AdminRepository;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.dto.response.user.RegisterResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Admin;
import com.backend.domain.valueobject.BusinessError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    public LoginResponse login(LoginRequest req) {
        // 1. Query user by email from database
        Admin user = adminRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new BusinessException(BusinessError.LOGIN_FAIL));
        // 2. Compare password using PasswordEncoder
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(BusinessError.LOGIN_FAIL);
        }
        // 3. Generate token on success
        String token = tokenProvider.generateToken(user);
        return LoginResponse.builder().accessToken(token).build();
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (adminRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(BusinessError.USER_EXISTED);
        }

        Admin admin =
            Admin.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullname())
                .build();

        Admin saved = adminRepository.save(admin);
        return RegisterResponse.builder()
            .email(saved.getEmail())
            .fullname(saved.getFullName())
            .role("ADMIN")
            .build();
    }
}
