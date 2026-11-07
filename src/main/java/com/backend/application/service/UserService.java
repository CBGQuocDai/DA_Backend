package com.backend.application.service;

import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.dto.response.user.RegisterResponse;

public interface UserService {
    LoginResponse login(LoginRequest req);
    RegisterResponse register(RegisterRequest req);

    /** Changes the signed-in account's own password; the same rules for every role. */
    void changePassword(Long userId, ChangePasswordRequest request);
}
