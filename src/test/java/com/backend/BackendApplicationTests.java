package com.backend;

import com.backend.application.adapter.ocr.OcrService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	// OcrServiceImpl tạo client Document AI ngay khi khởi động -> cần credential GCP, CI không có
	@MockitoBean
	OcrService ocrService;

	@Test
	void contextLoads() {
	}

}
