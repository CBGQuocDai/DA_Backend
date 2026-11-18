package com.backend.infrastructure.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfig {

    /**
     * Thread chạy OCR sách mới upload. Pool nhỏ để không gửi quá nhiều request Document AI cùng lúc
     * (dễ bị 429); sách upload khi pool đang bận xếp hàng chờ.
     */
    @Bean
    public ThreadPoolTaskExecutor ocrTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setThreadNamePrefix("ocr-");
        return executor;
    }
}
