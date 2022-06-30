-- 获取锁中的线程标识（get key）,比较线程标识与锁的标识是否一致
if (redis.call('GET', KEYS[1]) == ARGV[1]) then
    -- 释放锁 （del key） 删除成功会返回1
    return redis.call('DEL', KEYS[1])
end
return 0