package com.backend.infrastructure.messageQueue;

import com.backend.application.adapter.messaging.JobRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisJobRegistry implements JobRegistry {

    private static final String PREFIX = "tts:job:chapter:";
    /** Đủ dài cho job xếp hàng lâu; hết hạn thì kết quả được nhận như job không có mã. */
    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void put(Long chapterId, String jobId) {
        redisTemplate.opsForValue().set(PREFIX + chapterId, jobId, TTL);
    }

    @Override
    public Optional<String> find(Long chapterId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + chapterId));
    }

    @Override
    public void remove(Long chapterId) {
        redisTemplate.delete(PREFIX + chapterId);
    }
}
