package com.hmdp.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @param expireSec
     * @return
     */
    boolean tryLock(long expireSec);

    /**
     * 释放锁
     */
    void unLock();
}
