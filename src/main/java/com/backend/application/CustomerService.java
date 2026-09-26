package com.backend.application;


import com.backend.domain.dto.request.user.customer.CustomerUpdateProfileRequest;
import com.backend.domain.dto.response.customer.CustomerProfileResponse;

public interface CustomerService extends UserService {
    CustomerProfileResponse getProfile(Long id);

    CustomerProfileResponse updateProfile(Long userId, CustomerUpdateProfileRequest request);
}
