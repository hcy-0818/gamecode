package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.UserProfileUpdateDTO;
import com.example.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String AVATAR_DIR = "uploads/avatars/";

    @Override
    public User login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
               .eq(User::getPassword, password)
               .eq(User::getDeleted, 0);
        User user = getOne(wrapper);

        if (user != null && "DISABLED".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        return user;
    }

    @Override
    public User register(String username, String password, String phone, String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone != null ? phone : "");
        user.setEmail(email != null ? email : "");
        user.setRole("USER");
        user.setStatus("NORMAL");
        save(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return getOne(wrapper);
    }

    @Override
    @CacheEvict(value = "user:login", key = "#userId")
    public User updateProfile(Long userId, UserProfileUpdateDTO dto) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, dto.getUsername());
            if (count(wrapper) > 0) {
                throw new RuntimeException("用户名已被使用");
            }
            user.setUsername(dto.getUsername());
        }

        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }

        updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!user.getPassword().equals(oldPassword)) {
            throw new RuntimeException("原密码错误");
        }

        if (newPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }

        user.setPassword(newPassword);
        updateById(user);
    }

    @Override
    @CacheEvict(value = "user:login", key = "#userId")
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只支持图片格式");
        }

        try {
            Path dirPath = Paths.get(AVATAR_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = dirPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath);

            String avatarPath = "/api/user/avatar/" + filename;
            User user = getById(userId);
            if (user != null) {
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    String oldAvatarPath = user.getAvatar().replace("/api/user/avatar/", AVATAR_DIR);
                    Files.deleteIfExists(Paths.get(oldAvatarPath));
                }
                user.setAvatar(avatarPath);
                updateById(user);
            }

            return avatarPath;
        } catch (IOException e) {
            throw new RuntimeException("上传失败：" + e.getMessage());
        }
    }
}
