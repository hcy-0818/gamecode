package com.example.account.feign;

import com.example.common.Result;
import com.example.dto.BargainDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 还价服务 Feign 客户端（account-service 调用 bargain-service）
 */
@FeignClient(name = "bargain-service", path = "/api/bargain/feign")
public interface BargainFeignClient {

    @GetMapping("/by-account-and-buyer")
    Result<BargainDTO> getByAccountAndBuyer(@RequestParam Long accountId, @RequestParam Long buyerId);

    @DeleteMapping("/by-account/{accountId}")
    Result<Void> deleteByAccountId(@PathVariable Long accountId);
}
