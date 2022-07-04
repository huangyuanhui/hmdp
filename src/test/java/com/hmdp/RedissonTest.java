package com.hmdp;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class RedissonTest {

    @Autowired
    private RedissonClient redissonClient;

    private RLock lock;

    @BeforeEach
    void setUp() {
        lock = redissonClient.getLock("order");
    }

    /**
     *                     获取锁成功。。。。1
     *                     获取锁成功。。。。2
     *                     开始执行业务。。。。2
     *                     准备释放锁。。。。2
     *                     继续执行业务。。。。1
     *                     准备释放锁。。。。1
     */

    @Test
    void method1() throws InterruptedException {
        // 尝试获取锁
        //boolean isLock = lock.tryLock();

        // 加了等待时间，锁就变成了可重试的锁，就是说再等待时间内，如果没获取到锁，会再次重试
        /**
         * reission实现可重试锁的原理：
         * redission在tryLock方法内部调用tryAcquire方法去获取锁时，获取成功返回空对象，获取失败返回锁剩余时间，
         * 获取失败时，当判断超时时间小于获取锁的时间时，会去订阅锁释放的通知，当在锁释放时间内收到锁释放的
         * 通知时，还要再次去判断 超时时间 是否 小于从订阅到收到锁释放通知的时间，当小于时，开始while循环，
         * 调用tryAcquire方法去重试获取锁。
         *
         * 重试获取锁时，通过订阅锁释放的通知和信号量，避免了无效的重试与CPU的占用
         *
         *
         */
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
        if (!isLock) {
            log.error("获取锁失败。。。。1");
            return;
        }
        try {
            log.info("获取锁成功。。。。1");
            method2();
            log.info("继续执行业务。。。。1");
        }finally {
            log.warn("准备释放锁。。。。1");
            lock.unlock();
        }
    }

    @Test
    void method2() {
        // 尝试获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("获取锁失败。。。。2");
            return;
        }
        try {
            log.info("获取锁成功。。。。2");
            log.info("开始执行业务。。。。2");
        }finally {
            log.warn("准备释放锁。。。。2");
            lock.unlock();
        }
    }
}
