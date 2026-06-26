package com.example.bargain.feign;

import com.example.common.Result;
import com.example.dto.BargainDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 还价服务 Feign 接口（供 account-service 和 order-service 调用）
 */
@FeignClient(name = "bargain-service", path = "/api/bargain/feign")
public interface BargainFeignClient {

    @GetMapping("/{id}")
    Result<BargainDTO> getById(@PathVariable Long id);

    @GetMapping("/by-account-and-buyer")
    Result<BargainDTO> getByAccountAndBuyer(@RequestParam Long accountId, @RequestParam Long buyerId);

    @PutMapping("/update-status-by-account")
    Result<Void> updateStatusByAccount(@RequestParam Long accountId, @RequestParam String status);

    @DeleteMapping("/by-account/{accountId}")
    Result<Void> deleteByAccountId(@PathVariable Long accountId);
}
