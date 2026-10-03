package com.backend.application;

import com.backend.domain.dto.request.user.LoginRequest;
import com.backend.domain.dto.request.user.RegisterRequest;
import com.backend.domain.dto.response.user.LoginResponse;
import com.backend.domain.dto.response.user.RegisterResponse;

public interface UserService {
    LoginResponse login(LoginRequest req);
    RegisterResponse register(RegisterRequest req);
}
