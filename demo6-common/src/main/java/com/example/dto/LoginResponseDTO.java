package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 — 包含 JWT Token 和用户信息
 */
@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String avatar;
}
