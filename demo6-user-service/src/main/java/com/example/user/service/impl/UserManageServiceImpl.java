package com.example.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
public class UserManageServiceImpl implements UserManageService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<User> listUsers(int page, int size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> result = userMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }

    @Override
    public long countUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword);
        }
        return userMapper.selectCount(wrapper);
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Transactional
    public User addUser(User user) {
        user.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        user.setRole("USER");
        user.setStatus("NORMAL");
        userMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        User existing = userMapper.selectById(user.getId());
        if (existing != null) {
            user.setPassword(existing.getPassword());
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setStatus("NORMAL".equals(user.getStatus()) ? "DISABLED" : "NORMAL");
            userMapper.updateById(user);
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
