package com.example.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 — 生成与验证 Token
 */
public class JwtUtil {

    private static final String DEFAULT_SECRET = "demo6-cloud-jwt-secret-key-2024-microservice";
    private static final long DEFAULT_EXPIRATION = 30 * 60 * 1000L; // 30分钟

    private static String secret = DEFAULT_SECRET;
    private static long expiration = DEFAULT_EXPIRATION;

    public static void init(String secret, long expiration) {
        JwtUtil.secret = (secret != null && !secret.isEmpty()) ? secret : DEFAULT_SECRET;
        JwtUtil.expiration = expiration > 0 ? expiration : DEFAULT_EXPIRATION;
    }

    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     */
    public static String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 JWT Token
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从 Token 中获取角色
     */
    public static String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 验证 Token 是否有效（未过期且签名正确）
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 剩余有效时间（毫秒）
     */
    public static long getRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}
