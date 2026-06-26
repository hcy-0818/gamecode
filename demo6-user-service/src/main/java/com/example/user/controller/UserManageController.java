package com.example.user.controller;

import com.example.common.Result;
import com.example.entity.User;
import com.example.user.service.UserManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class UserManageController {

    @Autowired
    private UserManageService userManageService;

    /** 查询用户列表（分页） */
    @GetMapping("/list")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        List<User> users = userManageService.listUsers(page, size, keyword);
        long total = userManageService.countUsers(keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(userManageService.getUserById(id));
    }

    @PostMapping("/add")
    public Result<User> addUser(@RequestBody User user) {
        User result = userManageService.addUser(user);
        return Result.success("用户添加成功", result);
    }

    @PutMapping("/update")
    public Result<Void> updateUser(@RequestBody User user) {
        userManageService.updateUser(user);
        return Result.ok("用户信息更新成功");
    }

    @PostMapping("/toggle/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        userManageService.toggleStatus(id);
        return Result.ok("状态已切换");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userManageService.deleteUser(id);
        return Result.ok("用户已删除");
    }
}
