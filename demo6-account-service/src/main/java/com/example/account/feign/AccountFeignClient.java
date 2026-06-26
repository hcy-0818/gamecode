package com.example.account.feign;

import com.example.common.Result;
import com.example.entity.GameAccount;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 账号服务 Feign 接口（供 order-service 和 bargain-service 调用）
 */
@FeignClient(name = "account-service", path = "/api/account/feign")
public interface AccountFeignClient {

    @GetMapping("/{id}")
    Result<GameAccount> getById(@PathVariable Long id);

    @PostMapping("/mark-sold/{id}")
    Result<Void> markAsSold(@PathVariable Long id);

    @PutMapping("/restore/{id}")
    Result<Void> restoreToListed(@PathVariable Long id);

    @GetMapping("/bargain-price")
    Result<BigDecimal> getBargainPrice(@RequestParam Long accountId, @RequestParam Long buyerId);
}
