package com.backend;

import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.repository.AdminRepository;
import com.backend.application.adapter.repository.CustomerRepository;
import com.backend.application.adapter.repository.UserRepository;
import com.backend.application.adapter.security.PasswordEncoder;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.user.ChangePasswordRequest;
import com.backend.application.dto.request.user.LoginRequest;
import com.backend.application.dto.request.user.RegisterRequest;
import com.backend.application.service.AdminService;
import com.backend.application.service.CustomerService;
import com.backend.application.service.FileManagementService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Admin;
import com.backend.domain.model.Customer;
import com.backend.domain.model.User;
import com.backend.domain.valueobject.BusinessError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shared change-password path against the real user table (JOINED inheritance) on H2. */
@SpringBootTest
@ActiveProfiles("test")
class ChangePasswordIntegrationTest {

    // Same overrides as the other integration test, so both share one cached context
    @MockitoBean
    OcrService ocrService;
    @MockitoBean
    StorageService storageService;
    @MockitoBean
    FileManagementService fileManagementService;
    @MockitoBean
    MessageQueue messageQueue;

    @Autowired
    AdminService adminService;
    @Autowired
    CustomerService customerService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AdminRepository adminRepository;
    @Autowired
    CustomerRepository customerRepository;

    private static RegisterRequest registration(String email) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("cu123456");
        req.setFullname("Người thử");
        return req;
    }

    private static LoginRequest login(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private static String email(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@da.vn";
    }

    private Long idOf(String email, Class<? extends User> type) {
        return type == Admin.class
                ? adminRepository.findByEmail(email).orElseThrow().getId()
                : customerRepository.findByEmail(email).orElseThrow().getId();
    }

    @Test
    void customerCanLogInWithTheNewPasswordOnly() {
        String email = email("kh");
        customerService.register(registration(email));
        Long id = idOf(email, Customer.class);

        customerService.changePassword(id, ChangePasswordRequest.builder()
                .oldPassword("cu123456").newPassword("moi123456").build());

        assertDoesNotThrow(() -> customerService.login(login(email, "moi123456")));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> customerService.login(login(email, "cu123456")));
        assertEquals(BusinessError.LOGIN_FAIL, ex.getError());
    }

    @Test
    void adminPasswordIsUpdatedAndAccountKeepsItsType() {
        String email = email("qt");
        adminService.register(registration(email));
        Long id = idOf(email, Admin.class);

        adminService.changePassword(id, ChangePasswordRequest.builder()
                .oldPassword("cu123456").newPassword("moi123456").build());

        User user = userRepository.findById(id).orElseThrow();
        assertInstanceOf(Admin.class, user);
        assertTrue(passwordEncoder.matches("moi123456", user.getPassword()));
        assertDoesNotThrow(() -> adminService.login(login(email, "moi123456")));
    }
}
