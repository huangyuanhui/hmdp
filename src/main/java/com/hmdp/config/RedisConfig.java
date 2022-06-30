package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置Redisson客户端
 */
@Configuration
public class RedisConfig {

    /**
     * 可重入锁：同一个线程多次获取同一把锁！！！
     * redisson可重入的原理
     *
     * 参考JDK里面提供的可重入锁的原理：线程在获取锁的时候，如果判断要获取的锁已经有人的情况下，就去看一下获取锁的
     * 是不是自己，也就是说是不是用一个线程，如果是同一个线程，那么也会让线程再次获取锁，但是有计数器会记录线程重入的次数
     * ，即该线程总共获取锁几次，也就是说同一个线程在获取锁的时候，计数器会累加，在释放锁的时候，计数器会累减
     *
     * 因此，redisson实现重入锁的时候，可以参考JDK里面提供的可重入锁的实现：
     * 也就是在锁里边不仅仅要记录获取锁的线程，还得记录获取这个线程重入的次数，即这个线程总共拿了多少次锁，每获取一次锁，次数就加1
     * ,redisson实现重入锁可以利用hash数据接购，key为锁的名称，field1的值记录获取锁的线程标识，filed2的值记录重入次数，
     *
     *
     * redisson实现重入锁的数据接购：
     *                  value
     *  key
     *            field       value
     *
     *  lock     threadId     count
     *
     *
     *  因为redisson实现重入锁利用的是hash数据结构，而不是String，string可以set key value nx ex，hash结构式没有
     *  这样的命令的，所以只能把 nx ex 的逻辑拆开，先判断锁是否存在，再手动来设置过期时间
     *
     *        KEY[1]：锁的key    ARGV[2]：线程的唯一标致     ARGV[1]：锁的自动释放时间
     *
     *        redisson获取重入锁lua脚本（确保获取锁的原子性）：
     *                         // 判断锁是否存在
     *                         "if (redis.call('exists', KEYS[1]) == 0) then " +
     *                         // 不存在，获取锁并添加线程标识，重入次数设成1
     *                         "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
     *                         // 设置锁的有效期
     *                         "redis.call('pexpire', KEYS[1], ARGV[1]); " +
     *                         "return nil; " +
     *                         "end; " +
     *                         // 锁存在，判断锁标识是否是当前线程
     *                         "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
     *                         // 是当前线程，重入次数加1
     *                         "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
     *                         // 锁有效期重置
     *                         "redis.call('pexpire', KEYS[1], ARGV[1]); " +
     *                         "return nil; " +
     *                         "end; " +
     *                         // 锁不上自己（当前线程）的，返回0
     *                         "return redis.call('pttl', KEYS[1]);"
     *
     *
     *        KEY[1]：锁的key    ARGV[3]：线程的唯一标致     ARGV[2]：锁的自动释放时间
     *
     *        redisson获取重入锁lua脚本（确保释放锁的原子性）：
     *                         // 判断锁标识是否是当前线程
     *                         "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then " +
     *                         // 不是自己的，直接返回
     *                         "return nil;" +
     *                         "end; " +
     *                         // 锁是当前线程的，重入次数减1
     *                         "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
     *                         // 判断重入次数是否大于0
     *                         "if (counter > 0) then " +
     *                         // 重入次数大于0，证明不是最外层，还有别的业务，重置有效期，给后续业务执行留够时间
     *                         "redis.call('pexpire', KEYS[1], ARGV[2]); " +
     *                         "return 0; " +
     *                         "else " +
     *                         // 重入次数是0了，要真正删除锁了
     *                         "redis.call('del', KEYS[1]); " +
     *                         "redis.call('publish', KEYS[2], ARGV[1]); " +
     *                         "return 1; " +
     *                         "end; " +
     *                         "return nil;",
     *
     */

    @Bean
    public RedissonClient redissonClient() {
        // 配置类
        Config config = new Config();
        // 添加redis地址，这里添加了单节点的地址，也可以使用config.useClusterServers()添加集群地址
        config.useSingleServer().setAddress("redis://120.77.168.189:6379").setPassword("123456");
        // 创建客户端
        return Redisson.create(config);
    }
}
