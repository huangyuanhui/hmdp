package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
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

    /**
     * 释放锁lua脚本
     */
    private static final DefaultRedisScript<Long> unlockScript;

    static {
        unlockScript = new DefaultRedisScript<>();
        unlockScript.setLocation(new ClassPathResource("unlock.lua"));
        unlockScript.setResultType(Long.class);
    }


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
    public void unlock() {
        // 调用lua脚本释放锁
        redisTemplate.execute(
                unlockScript,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }


    /*
    @Override
    public void unlock() {
        // 获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        String id = redisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 此方法还是有问题，因为从redis中获取锁标识去判断锁标识是否一致 与 释放锁不是原子操作，在极端情况下就会产生问题，
        // 比如，线程一判断锁标识后，在执行释放锁的时候发生了阻塞，而阻塞的时候如果足够长，很有可能会触发锁的超时释放，锁一旦超时释放
        // 那么此时别的线程又可以乘虚而入成功获取锁，而当线程二获取锁成功后，线程一阻塞结束醒来去执行释放锁的操作，于是就把线程二的锁释放掉了
        //，此时线程三又可以乘虚而入获取锁，那么再一次发生线程安全问题
        if (threadId.equals(id)) {
            redisTemplate.delete(KEY_PREFIX + name);
        }
    }
    */
}
