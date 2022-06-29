package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * redis实现分布式锁
 */
public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate redisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 锁前缀
     */
    private static final String KEY_PREFIX = "lock:";
    /**
     * 线程id前缀：UUID标识jvm
     */
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    @Override
    public boolean tryLock(long expireSec) {
        // 获取线程标示，（问题：线程id在jvm中是递增的，但是多个jvm中线程标识可能会一样的情况）
        //long threadId = Thread.currentThread().getId();
        //Boolean success = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId + "", expireSec, TimeUnit.SECONDS);

        String threadId = ID_PREFIX + Thread.currentThread().getId();  // UUID标识jvm，线程id标识线程
        Boolean success = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, expireSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
        // 获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        String id = redisTemplate.opsForValue().get(KEY_PREFIX + name);
        if (threadId.equals(id)) {
            redisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
