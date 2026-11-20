package com.backend.application.service.impl;

import com.backend.application.adapter.storage.StorageService;
import com.backend.application.service.CustomerService;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.dto.response.user.RegisterResponse;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.adapter.security.TokenProvider;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.UserRepository;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.customer.CustomerUpdateProfileRequest;
import com.backend.application.dto.response.customer.CustomerProfileResponse;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.FileManagementService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Customer;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CustomerServiceImpl extends AbstractUserServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final ResponseMapper responseMapper;
    private final FileManagementService fileManagementService;
    private final StorageService storageService;

    public CustomerServiceImpl(CustomerRepository customerRepository, UserRepository userRepository,
                               PasswordEncoder passwordEncoder, TokenProvider tokenProvider,
                               ResponseMapper responseMapper, FileManagementService fileManagementService, StorageService storageService) {
        super(userRepository, passwordEncoder, tokenProvider);
        this.customerRepository = customerRepository;
        this.responseMapper = responseMapper;
        this.fileManagementService = fileManagementService;
        this.storageService = storageService;
    }

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
        return responseMapper.toCustomerProfileResponse(customer);
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
        customerRepository.save(customer);

        return responseMapper.toCustomerProfileResponse(customer);
    }

    @Override
    @Transactional
    public CustomerProfileResponse uploadAvatar(Long userId, String avatarPath) {
        if (userId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (avatarPath == null || avatarPath.isBlank()) {
            throw new BusinessException(BusinessError.IMAGE_FORMAT_INCORRECT);
        }
        Customer customer = customerRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));

        String oldAvatar = customer.getAvatarUrl();
        String permanentPath = UploadType.IMAGE.permanentPath(avatarPath);
        boolean isNewAvatar = !permanentPath.equals(oldAvatar);
        if (isNewAvatar) {
            fileManagementService.verifyUpload(avatarPath, UploadType.IMAGE);

            customer.setAvatarUrl(permanentPath);
            customerRepository.save(customer);

            fileManagementService.moveToPermanent(avatarPath, UploadType.IMAGE);

            if (oldAvatar != null && !oldAvatar.startsWith("http") && !oldAvatar.equals(permanentPath)) {
                try {
                    storageService.delete(oldAvatar);
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar {}", oldAvatar, e);
                }
            }
        }
        return responseMapper.toCustomerProfileResponse(customer);
    }
}
