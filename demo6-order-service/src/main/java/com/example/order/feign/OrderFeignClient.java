package com.example.order.feign;

import com.example.common.Result;
import com.example.entity.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单服务 Feign 接口（供 bargain-service 调用）
 */
@FeignClient(name = "order-service", path = "/api/order/feign")
public interface OrderFeignClient {

    @PostMapping("/create-from-bargain")
    Result<Order> createFromBargain(@RequestBody Map<String, Object> request);
}
