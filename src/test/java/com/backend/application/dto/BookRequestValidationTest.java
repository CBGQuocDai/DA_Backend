package com.backend.application.dto;

import com.backend.application.dto.request.book.BookRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookRequestValidationTest {

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

    private static BookRequest complete() {
        return BookRequest.builder()
                .title("Sách").price(BigDecimal.TEN).description("Mô tả")
                .voiceId(1L).categoryIds(List.of(1L)).authorIds(List.of(2L))
                .contentFile("tmp/pdfs/a.pdf").coverImage("tmp/images/a.png")
                .build();
    }

    @Test
    void acceptsCompleteRequest() {
        assertTrue(validator.validate(complete()).isEmpty());
    }

    @Test
    void requiresBothFiles() {
        BookRequest request = complete();
        request.setContentFile(null);
        request.setCoverImage(" ");

        assertEquals(Set.of("contentFile", "coverImage"), validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet()));
    }

    @Test
    void requiresEveryOtherField() {
        BookRequest request = complete();
        request.setTitle(" ");
        request.setCategoryIds(List.of());

        assertEquals(2, validator.validate(request).size());
    }
}
