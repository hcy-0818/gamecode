package com.example.user.controller;

import com.example.common.Result;
import com.example.common.jwt.JwtUtil;
import com.example.dto.LoginResponseDTO;
import com.example.dto.UserLoginRequest;
import com.example.entity.User;
import com.example.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 管理员登录/登出（路径 /api/admin/login, /api/admin/logout）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@RequestBody UserLoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user == null) {
            return Result.error(400, "用户名或密码错误");
        }
        if (!"ADMIN".equals(user.getRole())) {
            return Result.error(403, "非管理员账号");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginResponseDTO response = new LoginResponseDTO(
                token, user.getId(), user.getUsername(), user.getRole(), user.getAvatar()
        );
        return Result.success("管理员登录成功", response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ") && redisTemplate != null) {
            String token = authHeader.substring(7);
            long remainingTime = JwtUtil.getRemainingTime(token);
            if (remainingTime > 0) {
                redisTemplate.opsForSet().add("token:blacklist", token);
                redisTemplate.expire("token:blacklist", remainingTime, TimeUnit.MILLISECONDS);
            }
        }
        return Result.ok("已退出");
    }
}
