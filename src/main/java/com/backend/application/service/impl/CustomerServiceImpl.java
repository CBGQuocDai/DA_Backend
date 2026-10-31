package com.backend.application.service.impl;

import com.backend.application.service.CustomerService;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.adapter.security.TokenProvider;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.UserRepository;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.dto.request.user.customer.CustomerChangePasswordRequest;
import com.backend.application.dto.request.user.customer.CustomerUpdateProfileRequest;
import com.backend.application.dto.response.customer.CustomerProfileResponse;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.dto.response.user.RegisterResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Customer;
import com.backend.domain.valueobject.BusinessError;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest req) {
        // 1. Query user by email from database
        Customer user = customerRepository.findByEmail(req.getEmail())
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
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(BusinessError.USER_EXISTED);
        }

        Customer customer = Customer.builder()
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .fullName(req.getFullname())
            .build();

        Customer saved = customerRepository.save(customer);
        return RegisterResponse.builder()
            .email(saved.getEmail())
            .fullname(saved.getFullName())
            .role("CUSTOMER")
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(Long id) {
        if (id == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        return CustomerProfileResponse.builder()
            .id(customer.getId())
            .email(customer.getEmail())
            .fullName(customer.getFullName())
            .avatarUrl(customer.getAvatarUrl())
            .build();
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateProfile(Long userId,
        CustomerUpdateProfileRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        if (request.getFullName() != null) {
            customer.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            customer.setAvatarUrl(request.getAvatarUrl());
        }
        customerRepository.save(customer);

        return CustomerProfileResponse.builder()
            .id(customer.getId())
            .email(customer.getEmail())
            .fullName(customer.getFullName()).avatarUrl(customer.getAvatarUrl()).build();
    }

    @Override
    @Transactional
    public void changePassword(Long userId,
        CustomerChangePasswordRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), customer.getPassword())) {
            throw new BusinessException(BusinessError.PASSWORD_WRONG);
        }
        if (request.getNewPassword() != null) {
            customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
            customerRepository.save(customer);
        }

    }
}
