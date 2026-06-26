package com.example.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway 全局 JWT 鉴权过滤器
 *
 * 核心逻辑：
 * 1. 静态资源直接放行
 * 2. 无论是否白名单，都尝试从 Authorization 头提取 JWT 并注入 X-User-* 请求头
 * 3. 白名单路径：无 Token 也能通过（公开访问）；有 Token 则注入用户信息
 * 4. 非白名单路径：必须有有效 Token，否则返回 401
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 白名单：无需登录即可访问，但有 Token 也会注入用户信息 */
    private static final List<String> WHITELIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/admin/login",
            "/api/account/list",
            "/api/account/detail",
            "/api/account/image",
            "/api/user/avatar/"
    );

    @Value("${jwt.secret:demo6-cloud-jwt-secret-key-2024-microservice}")
    private String jwtSecret;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 静态资源直接放行
        if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")
                || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".ico")
                || path.endsWith(".svg") || path.endsWith(".woff") || path.endsWith(".woff2")) {
            return chain.filter(exchange);
        }

        // 2. 尝试从 Authorization 头提取 JWT
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean isWhitelisted = isWhitelisted(path);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Long userId = claims.get("userId", Long.class);
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);

                // 检查黑名单（仅对非白名单路径严格检查）
                if (redisTemplate != null) {
                    return redisTemplate.opsForSet().isMember("token:blacklist", token)
                            .flatMap(isBlacklisted -> {
                                if (Boolean.TRUE.equals(isBlacklisted)) {
                                    // 白名单路径：Token 黑名单也放行，但不注入用户信息
                                    if (isWhitelisted) {
                                        return chain.filter(exchange);
                                    }
                                    return unauthorized(exchange, "Token已失效，请重新登录");
                                }
                                return injectUserHeaders(exchange, chain, userId, username, role);
                            });
                }

                // 无 Redis 时直接注入
                return injectUserHeaders(exchange, chain, userId, username, role);

            } catch (Exception e) {
                // Token 无效：白名单放行，非白名单返回 401
                if (!isWhitelisted) {
                    return unauthorized(exchange, "Token无效或已过期");
                }
            }
        } else {
            // 无 Token：非白名单返回 401
            if (!isWhitelisted) {
                return unauthorized(exchange, "请先登录");
            }
        }

        // 白名单路径，无有效 Token，直接放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> injectUserHeaders(ServerWebExchange exchange, GatewayFilterChain chain,
                                          Long userId, String username, String role) {
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", username)
                .header("X-User-Role", role)
                .build();
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
