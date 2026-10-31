package com.backend.application.dto;

import com.backend.application.dto.request.upload.UploadUrlRequest;
import com.backend.domain.valueobject.UploadType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadUrlRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsCompleteRequest() {
        UploadUrlRequest request = UploadUrlRequest.builder()
                .type(UploadType.PDF).fileName("sach.pdf").size(1024L).build();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void reportsEveryMissingField() {
        assertEquals(3, validator.validate(new UploadUrlRequest()).size());
    }

    @Test
    void rejectsNonPositiveSize() {
        UploadUrlRequest request = UploadUrlRequest.builder()
                .type(UploadType.PDF).fileName("sach.pdf").size(0L).build();

        Set<ConstraintViolation<UploadUrlRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("size", violations.iterator().next().getPropertyPath().toString());
    }
}
