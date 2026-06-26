package com.example.user.feign;

import com.example.common.Result;
import com.example.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 接口（供其他服务调用）
 */
@FeignClient(name = "user-service", path = "/api/user/feign")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<User> getById(@PathVariable Long id);

    @GetMapping("/by-username/{username}")
    Result<User> getByUsername(@PathVariable String username);
}
