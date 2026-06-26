package com.example.order.feign;

import com.example.common.Result;
import com.example.dto.BargainDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "bargain-service", path = "/api/bargain/feign")
public interface BargainFeignClient {
    @GetMapping("/{id}")
    Result<BargainDTO> getById(@PathVariable Long id);

    @PutMapping("/update-status-by-account")
    Result<Void> updateStatusByAccount(@RequestParam Long accountId, @RequestParam String status);
}
