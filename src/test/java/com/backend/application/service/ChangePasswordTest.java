package com.backend.application.service;

import com.backend.application.adapter.repository.AdminRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.UserRepository;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.adapter.security.TokenProvider;
import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.service.impl.AdminServiceImpl;
import com.backend.application.service.impl.CustomerServiceImpl;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Admin;
import com.backend.domain.model.Customer;
import com.backend.domain.valueobject.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Change password is one use case for every role, so admin and customer share its rules. */
@ExtendWith(MockitoExtension.class)
class ChangePasswordTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;

    private AdminService adminService;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(adminRepository, userRepository, passwordEncoder, tokenProvider);
        customerService = new CustomerServiceImpl(customerRepository, userRepository, passwordEncoder, tokenProvider);
    }

    private static ChangePasswordRequest request(String oldPassword, String newPassword) {
        return ChangePasswordRequest.builder().oldPassword(oldPassword).newPassword(newPassword).build();
    }

    @Test
    void adminChangesPasswordWhenOldOneMatches() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(Admin.builder().id(1L).password("hash-cu").build()));
        when(passwordEncoder.matches("cu123456", "hash-cu")).thenReturn(true);
        when(passwordEncoder.encode("moi123456")).thenReturn("hash-moi");

        adminService.changePassword(1L, request("cu123456", "moi123456"));

        verify(userRepository).updatePassword(1L, "hash-moi");
    }

    @Test
    void customerChangesPasswordThroughTheSameRules() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(Customer.builder().id(2L).password("hash-cu").build()));
        when(passwordEncoder.matches("cu123456", "hash-cu")).thenReturn(true);
        when(passwordEncoder.encode("moi123456")).thenReturn("hash-moi");

        customerService.changePassword(2L, request("cu123456", "moi123456"));

        verify(userRepository).updatePassword(2L, "hash-moi");
    }

    @Test
    void wrongOldPasswordIsRefused() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(Customer.builder().id(2L).password("hash-cu").build()));
        when(passwordEncoder.matches("sai", "hash-cu")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerService.changePassword(2L, request("sai", "moi123456")));

        assertEquals(BusinessError.PASSWORD_WRONG, ex.getError());
        verify(userRepository, never()).updatePassword(anyLong(), any());
    }

    @Test
    void samePasswordIsRefused() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(Admin.builder().id(1L).password("hash-cu").build()));
        when(passwordEncoder.matches("cu123456", "hash-cu")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.changePassword(1L, request("cu123456", "cu123456")));

        assertEquals(BusinessError.PASSWORD_UNCHANGED, ex.getError());
        verify(userRepository, never()).updatePassword(anyLong(), any());
    }

    @Test
    void unknownAccountIsRefused() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.changePassword(9L, request("cu123456", "moi123456")));

        assertEquals(BusinessError.USER_NOT_FOUND, ex.getError());
    }

    @Test
    void missingPrincipalIsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerService.changePassword(null, request("cu123456", "moi123456")));

        assertEquals(BusinessError.UNAUTHORIZED, ex.getError());
    }
}
