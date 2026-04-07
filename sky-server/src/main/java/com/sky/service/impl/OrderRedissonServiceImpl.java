package com.sky.service.impl;

import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.result.Result;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * 文件名：OrderRedissonServiceImpl
 * 作者：24141
 * 创建日期：2026/4/7
 * 描述：
 */

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class OrderRedissonServiceImpl {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private OrderMapper orderMapper;

    // 加载 Lua 脚本
    private static final DefaultRedisScript<Long> SCRIPT;
    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setLocation(new ClassPathResource("stockLock.lua"));
        SCRIPT.setResultType(Long.class);
    }

    /**
     * 并发安全下单方法
     */
    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(Long userId, Long dishId) {
        // ===================== 第1步：Redis+Lua 原子拦截 =====================
        Long result = stringRedisTemplate.execute(
                SCRIPT,
                Collections.singletonList(dishId.toString()),  // KEY1: 菜品ID
                Collections.singletonList(userId.toString()) // KEY2: 用户ID
        );

        int code = result.intValue();
        if (code == 1) {
            return Result.fail("您已下单，请勿重复操作");
        }
        if (code == 2) {
            return Result.fail("菜品库存不足");
        }

        // ===================== 第2步：Redisson 分布式锁 =====================
        String lockKey = "lock:order:" + userId + ":" + dishId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLock = lock.tryLock();

        if (!isLock) {
            // 缓存库存回滚（重要）
            stringRedisTemplate.opsForValue().increment("dish:stock:" + dishId);
            stringRedisTemplate.delete("order:user:" + userId + ":" + dishId);
            return Result.fail("系统繁忙，请稍后再试");
        }

        try {
            // ===================== 第3步：数据库乐观锁扣库存 =====================
            Dish dish = dishMapper.selectById(dishId);
            if (dish.getStock() <= 0) {
                throw new RuntimeException("库存不足");
            }
            dish.setStock(dish.getStock() - 1);
            // 乐观锁更新：自动带 version 条件
            int rows = dishMapper.updateById(dish);
            if (rows == 0) {
                throw new RuntimeException("并发更新失败，请重试");
            }

            // ===================== 第4步：创建订单（唯一索引防重复） =====================
            Order order = new Order();
            order.setUserId(userId);
            order.setDishId(dishId);
            order.setOrderStatus(0); // 0-未支付
            orderMapper.insert(order);

            return Result.success("下单成功");
        } finally {
            lock.unlock(); // 释放锁
        }
    }
}