package com.example.user.service;

import com.example.entity.User;
import java.util.List;

public interface UserManageService {
    List<User> listUsers(int page, int size, String keyword);
    long countUsers(String keyword);
    User getUserById(Long id);
    User addUser(User user);
    void updateUser(User user);
    void toggleStatus(Long id);
    void deleteUser(Long id);
}
