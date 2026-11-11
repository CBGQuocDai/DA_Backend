package com.backend.application.service.impl;

import com.backend.application.adapter.repository.UserRepository;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.adapter.security.TokenProvider;
import com.backend.application.service.AdminService;
import com.backend.application.adapter.repository.AdminRepository;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Admin;
import com.backend.domain.valueobject.BusinessError;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl extends AbstractUserServiceImpl implements AdminService {

    private final AdminRepository adminRepository;

    public AdminServiceImpl(AdminRepository adminRepository, UserRepository userRepository,
                            PasswordEncoder passwordEncoder, TokenProvider tokenProvider) {
        super(userRepository, passwordEncoder, tokenProvider);

        this.adminRepository = adminRepository;
    }

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
}
