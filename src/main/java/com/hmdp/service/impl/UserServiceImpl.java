package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机格式错误！");
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 发送验证码
        log.error("调用服务，向手机：{}发送验证码：{}", phone, code);
        // 保存验证码
        session.setAttribute("code", code);
        return Result.ok();
    }
}
