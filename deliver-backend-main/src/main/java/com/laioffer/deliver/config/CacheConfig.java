package com.laioffer.deliver.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 邮箱验证码缓存（业务缓存，短时有效）
        cacheManager.registerCustomCache("emailCodeCache",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(5))
                        .maximumSize(10000)
                        .build());

        // 邀请码缓存（业务缓存，稍长有效）
        cacheManager.registerCustomCache("inviteCodeCache",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(1))
                        .maximumSize(10000)
                        .build());

        // Token 版本号缓存（安全缓存，跟 token TTL 对齐或稍长）
        cacheManager.registerCustomCache("tokenVersionCache",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(1))
                        .maximumSize(10000)
                        .build());

        // SID 黑名单缓存（安全缓存，存登出的会话 ID，TTL 与 token 一致）
        cacheManager.registerCustomCache("sidBlacklistCache",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(1))
                        .maximumSize(10000)
                        .build());

        // 权限缓存（安全缓存，减少 DB 查询，失效时间可短一些）
        cacheManager.registerCustomCache("permissionCache",
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofMinutes(15))
                        .maximumSize(10000)
                        .build());

        return cacheManager;
    }
}
