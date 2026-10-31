package com.backend.presentation.controller;

import com.backend.application.service.FileManagementService;
import com.backend.application.dto.response.upload.UploadUrlResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    private static final String BODY = """
            {"type": "PDF", "fileName": "sach.pdf", "size": 1024}
            """;

    @Mock
    private FileManagementService fileManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UploadController(fileManagementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createUploadUrl_returnsTheSignedUpload() throws Exception {
        when(fileManagementService.createUploadUrl(any())).thenReturn(UploadUrlResponse.builder()
                .uploadUrl("https://signed").filePath("tmp/pdfs/a.pdf")
                .headers(Map.of("Content-Type", "application/pdf")).build());

        mockMvc.perform(post("/uploads/sign-url").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uploadUrl").value("https://signed"))
                .andExpect(jsonPath("$.data.filePath").value("tmp/pdfs/a.pdf"))
                .andExpect(jsonPath("$.data.headers['Content-Type']").value("application/pdf"));
    }

    @Test
    void createUploadUrl_mapsBusinessErrors() throws Exception {
        when(fileManagementService.createUploadUrl(any()))
                .thenThrow(new BusinessException(BusinessError.PDF_FORMAT_INCORRECT));

        mockMvc.perform(post("/uploads/sign-url").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BusinessError.PDF_FORMAT_INCORRECT.getCode()));
    }

    @Test
    void createUploadUrl_rejectsMissingFields() throws Exception {
        mockMvc.perform(post("/uploads/sign-url").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(BusinessError.VALIDATION_FAILED.getCode()));
        verifyNoInteractions(fileManagementService);
    }

    @Test
    void createUploadUrl_rejectsUnknownType() throws Exception {
        mockMvc.perform(post("/uploads/sign-url").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("PDF", "BOOK_COVER")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(fileManagementService);
    }
}
