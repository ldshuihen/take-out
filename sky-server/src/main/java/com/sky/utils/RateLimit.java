package com.sky.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件名：s
 * 作者：24141
 * 创建日期：2026/4/7
 * 描述：
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    // 限流时间窗口（秒）
    int second() default 1;

    // 允许最大请求数
    int maxCount() default 5;
}