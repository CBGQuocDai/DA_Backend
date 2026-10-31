package com.backend.application.service;

import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.dto.response.user.LoginResponse;
import com.backend.application.dto.response.user.RegisterResponse;

public interface UserService {
    LoginResponse login(LoginRequest req);
    RegisterResponse register(RegisterRequest req);
}
