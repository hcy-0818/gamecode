package com.example.bargain.feign;

import com.example.common.Result;
import com.example.entity.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "order-service", path = "/api/order/feign")
public interface OrderFeignClient {
    @PostMapping("/create-from-bargain")
    Result<Order> createFromBargain(@RequestBody Map<String, Object> request);
}
