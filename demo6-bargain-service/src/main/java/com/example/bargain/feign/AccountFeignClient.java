package com.example.bargain.feign;

import com.example.common.Result;
import com.example.entity.GameAccount;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "account-service", path = "/api/account/feign")
public interface AccountFeignClient {
    @GetMapping("/{id}")
    Result<GameAccount> getById(@PathVariable Long id);

    @PostMapping("/mark-sold/{id}")
    Result<Void> markAsSold(@PathVariable Long id);
}
