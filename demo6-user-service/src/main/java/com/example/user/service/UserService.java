package com.example.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.UserProfileUpdateDTO;
import com.example.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {
    User login(String username, String password);
    User register(String username, String password, String phone, String email);
    User getByUsername(String username);
    User updateProfile(Long userId, UserProfileUpdateDTO dto);
    void changePassword(Long userId, String oldPassword, String newPassword);
    String uploadAvatar(Long userId, MultipartFile file);
}
