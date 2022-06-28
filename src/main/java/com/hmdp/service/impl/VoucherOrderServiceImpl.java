package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (LocalDateTime.now().isBefore(seckillVoucher.getBeginTime())) {
            return Result.fail("秒杀未开始！");
        }
        if (LocalDateTime.now().isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("秒杀已结束！");
        }
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足！");
        }
        Long userId = UserHolder.getUser().getId();
        // 创建订单
        return createVoucherOrder(voucherId);
    }

    /**
     * 此方法是有问题的，因为是在方法内部加的锁，而事务是在方法上，那么此时，会先释放锁，再去提交事务。
     * 那么会有这种情况：锁释放了，意味着其他线程可以进来，但是事务尚未提交，即新增的订单很有可能还没写到数据库，
     * 有新线程进来查询订单会依然查不到，那么依然会有一人一单问题
     * 因此：这个方法的问题是锁的范围有点小，应该把整个函数锁起来，即事务提交之后，再去释放锁
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 一人一单：在扣减库存前，判断用户是否下过单了
        Long userId = UserHolder.getUser().getId();
        // 所谓一人一单，就是同一个用户来了，才去判断并发安全问题，也就是说只需要对用户加锁，也就是说同一个用户加同一把锁，缩小锁的范围，提高性能
        // 注意：不能使用userId.toString()，因为该方法实现里是一个全新的对象、全新的字符串；应该使用：userId.toString().intern()
        synchronized (userId.toString().intern()) {
            int count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
            if (count > 0) {
                return Result.fail("该用户已经购买过了！");
            }

            // 扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")    // set stock = stock - 1
                    .eq("voucher_id", voucherId).gt("stock", 0) // voucher_id = ? and stock > 0
                    .update();
            if (!success) {
                return Result.fail("库存不足！");
            }

            // 创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(userId);
            save(voucherOrder);
            return Result.ok(orderId);
        }
    }
}
