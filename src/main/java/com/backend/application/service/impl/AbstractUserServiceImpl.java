package com.backend.application.service.impl;

import com.backend.application.adapter.repository.UserRepository;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.adapter.security.TokenProvider;
import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.service.UserService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.User;
import com.backend.domain.valueobject.BusinessError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public abstract class AbstractUserServiceImpl implements UserService {

    protected final UserRepository userRepository;
    protected final PasswordEncoder passwordEncoder;
    protected final TokenProvider tokenProvider;

    protected AbstractUserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                      TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void logout(String token) {
        if (token != null) {
            tokenProvider.revoke(token);
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(BusinessError.PASSWORD_WRONG);
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(BusinessError.PASSWORD_UNCHANGED);
        }
        userRepository.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }
}
