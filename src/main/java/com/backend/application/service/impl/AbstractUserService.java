package com.backend.application.service.impl;

import com.backend.application.adapter.repository.UserRepository;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.service.UserService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.User;
import com.backend.domain.valueobject.BusinessError;
import org.springframework.transaction.annotation.Transactional;

/** The account use cases that do not depend on the role. */
public abstract class AbstractUserService implements UserService {

    protected final UserRepository userRepository;
    protected final PasswordEncoder passwordEncoder;

    protected AbstractUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
