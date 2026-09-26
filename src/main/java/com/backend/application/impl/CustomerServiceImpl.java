package com.backend.application.impl;

import com.backend.application.CustomerService;
import com.backend.domain.adapter.PasswordEncoder;
import com.backend.domain.adapter.TokenProvider;
import com.backend.domain.adapter.repository.CustomerRepository;
import com.backend.domain.adapter.repository.UserRepository;
import com.backend.domain.dto.request.user.LoginRequest;
import com.backend.domain.dto.request.user.RegisterRequest;
import com.backend.domain.dto.request.user.customer.CustomerUpdateProfileRequest;
import com.backend.domain.dto.response.customer.CustomerProfileResponse;
import com.backend.domain.dto.response.user.LoginResponse;
import com.backend.domain.dto.response.user.RegisterResponse;
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
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        CustomerProfileResponse response = CustomerProfileResponse.builder()
            .id(customer.getId())
            .email(customer.getEmail())
            .fullName(customer.getFullName())
            .avatarUrl(customer.getAvatarUrl())
            .build();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileResponse updateProfile(Long userId,
        CustomerUpdateProfileRequest request) {
        Customer customer = customerRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        if (request.getFullName() != null) {
            customer.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            customer.setAvatarUrl(request.getAvatarUrl());
        }
        customerRepository.save(customer);

        CustomerProfileResponse response = CustomerProfileResponse.builder()
            .id(customer.getId())
            .email(customer.getEmail())
            .fullName(customer.getFullName()).avatarUrl(customer.getAvatarUrl()).build();
        return response;
    }
}
