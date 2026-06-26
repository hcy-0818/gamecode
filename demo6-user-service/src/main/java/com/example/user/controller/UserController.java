package com.example.user.controller;

import com.example.common.Result;
import com.example.common.context.UserContext;
import com.example.common.jwt.JwtUtil;
import com.example.dto.ChangePasswordDTO;
import com.example.dto.LoginResponseDTO;
import com.example.dto.UserLoginRequest;
import com.example.dto.UserProfileUpdateDTO;
import com.example.entity.User;
import com.example.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 用户注册 */
    @PostMapping("/register")
    public Result<User> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String phone = body.get("phone");
        String email = body.get("email");

        if (username == null || username.isEmpty()) {
            return Result.error(400, "请输入用户名");
        }
        if (password == null || password.isEmpty()) {
            return Result.error(400, "请输入密码");
        }
        if (password.length() < 6) {
            return Result.error(400, "密码长度至少为6位");
        }

        try {
            User user = userService.register(username, password, phone, email);
            return Result.success("注册成功", user);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 用户登录 — 返回 JWT Token */
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@RequestBody UserLoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user == null) {
            return Result.error(400, "用户名或密码错误");
        }

        // 生成 JWT Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginResponseDTO response = new LoginResponseDTO(
                token, user.getId(), user.getUsername(), user.getRole(), user.getAvatar()
        );
        return Result.success("登录成功", response);
    }

    /** 管理员登录 */
    @PostMapping("/admin-login")
    public Result<LoginResponseDTO> adminLogin(@RequestBody UserLoginRequest req) {
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

    /** 退出登录 — Token 加入黑名单 */
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

    /** 获取当前登录用户信息 */
    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        User dbUser = userService.getById(userId);
        if (dbUser != null) {
            dbUser.setPassword(null);
        }
        return Result.success(dbUser);
    }

    /** 获取当前用户信息（管理端兼容） */
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        return getProfile();
    }

    /** 更新个人资料 */
    @PostMapping("/profile")
    public Result<User> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            User updatedUser = userService.updateProfile(userId, dto);
            return Result.success(updatedUser);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 修改密码 */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        if (dto.getOldPassword() == null || dto.getOldPassword().isEmpty()) {
            return Result.error(400, "请输入原密码");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isEmpty()) {
            return Result.error(400, "请输入新密码");
        }
        if (dto.getConfirmPassword() == null || dto.getConfirmPassword().isEmpty()) {
            return Result.error(400, "请确认新密码");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return Result.error(400, "两次输入的密码不一致");
        }
        try {
            userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
            return Result.success(null);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 上传头像 */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            String avatarPath = userService.uploadAvatar(userId, file);
            return Result.success(avatarPath);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 获取头像文件 */
    @GetMapping("/avatar/{filename}")
    public void getAvatar(@PathVariable String filename, jakarta.servlet.http.HttpServletResponse response) {
        String filePath = "uploads/avatars/" + filename;
        File file = new File(filePath);

        if (!file.exists()) {
            filePath = "static/images/default-avatar.png";
            file = new File(filePath);
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            response.setContentType("image/png");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            response.setStatus(404);
        }
    }

    // ========== Feign 内部调用端点 ==========

    @GetMapping("/feign/{id}")
    public Result<User> feignGetById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @GetMapping("/feign/by-username/{username}")
    public Result<User> feignGetByUsername(@PathVariable String username) {
        User user = userService.getByUsername(username);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
