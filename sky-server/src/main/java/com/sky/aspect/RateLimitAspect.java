package com.sky.aspect;

import com.sky.context.BaseContext;
import com.sky.utils.RateLimit;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * 文件名：ss
 * 作者：24141
 * 创建日期：2026/4/7
 * 描述：
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    // 加载Lua脚本
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/rateLimit.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sra.getRequest();

        // 1. 获取用户ID（可换成IP）
        Long userId = getCurrentUserId();
        if (userId == null) {
            userId = 0L;
        }

        // 2. 接口URI
        String uri = request.getRequestURI();

        // 3. 构造Redis Key：按用户+接口限流
        String key = "rate:limit:" + userId + ":" + uri;

        int second = rateLimit.second();
        int maxCount = rateLimit.maxCount();

        // 4. 执行Lua脚本
        Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(maxCount),
                String.valueOf(second)
        );

        // 5. 返回0=限流
        if (result == null || result == 0) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }

        return pjp.proceed();
    }

    // 获取当前登录用户ID（你项目里的真实方法）
    private Long getCurrentUserId() {
        try {
            return BaseContext.getCurrentId();
        } catch (Exception e) {
            return null;
        }
    }
}