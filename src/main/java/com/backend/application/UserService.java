package com.backend.application;

import com.backend.domain.dto.request.user.LoginRequest;
import com.backend.domain.dto.response.user.LoginResponse;

public interface UserService {
    LoginResponse login(LoginRequest req);
}
