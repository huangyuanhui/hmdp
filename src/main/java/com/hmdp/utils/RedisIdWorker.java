package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private StringRedisTemplate redisTemplate;

    public RedisIdWorker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 开始时间
     */
    private static final long BASIC_TIMESTAMP = 1640995200L;
    /**
     * 位移
     */
    private static final int BIT_COUNT = 32;

    /**
     * id
     *
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        // 时间戳
        LocalDateTime now = LocalDateTime.now();
        long timestamp = now.toEpochSecond(ZoneOffset.UTC) - BASIC_TIMESTAMP;
        // 序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = redisTemplate.opsForValue().increment("inc:" + keyPrefix + ":" + date);
        return timestamp << BIT_COUNT | count;
    }
}
