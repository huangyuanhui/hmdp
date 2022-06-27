package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPES;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Result queryList() {
        String shopTypesJson = redisTemplate.opsForValue().get(CACHE_SHOPTYPES);
        if (StrUtil.isNotBlank(shopTypesJson)) {
            return Result.ok(JSONUtil.toList(shopTypesJson, ShopType.class));
        }
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if (CollectionUtil.isEmpty(shopTypes)) {
            return Result.fail("商铺类型为空！");
        }
        redisTemplate.opsForValue().set(CACHE_SHOPTYPES, JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }
}
