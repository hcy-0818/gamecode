package com.example.common.config;

import com.example.common.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 公共配置 — 初始化 JWT 参数
 */
@Configuration
public class CommonConfig {

    @Value("${jwt.secret:demo6-cloud-jwt-secret-key-2024-microservice}")
    private String jwtSecret;

    @Value("${jwt.expiration:1800000}")
    private long jwtExpiration;

    @PostConstruct
    public void init() {
        JwtUtil.init(jwtSecret, jwtExpiration);
    }
}
