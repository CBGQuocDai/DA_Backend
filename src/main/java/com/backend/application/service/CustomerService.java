package com.backend.application.service;


import com.backend.application.dto.request.user.customer.CustomerChangePasswordRequest;
import com.backend.application.dto.request.user.customer.CustomerUpdateProfileRequest;
import com.backend.application.dto.response.customer.CustomerProfileResponse;

public interface CustomerService extends UserService {

    CustomerProfileResponse getProfile(Long id);

    CustomerProfileResponse updateProfile(Long userId, CustomerUpdateProfileRequest request);

    void changePassword(Long userId, CustomerChangePasswordRequest request);
}
