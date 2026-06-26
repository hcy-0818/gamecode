package com.example.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.Result;
import com.example.entity.Order;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * 管理端订单管理
 */
@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> getOrders(@RequestParam(required = false) String status) {
        List<Order> orders;
        if (status == null || status.isEmpty() || "ALL".equals(status)) {
            orders = orderService.list(new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreateTime));
        } else {
            orders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, status).orderByDesc(Order::getCreateTime));
        }

        List<Map<String, Object>> dtos = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", order.getId());
            dto.put("orderNo", order.getOrderNo());
            dto.put("buyerId", order.getBuyerId());
            dto.put("sellerId", order.getSellerId());
            dto.put("accountId", order.getAccountId());
            dto.put("price", order.getPrice());
            dto.put("status", order.getStatus());
            dto.put("createTime", order.getCreateTime());
            dtos.add(dto);
        }
        return Result.success(dtos);
    }

    @GetMapping("/order/detail/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", order.getId());
        dto.put("orderNo", order.getOrderNo());
        dto.put("buyerId", order.getBuyerId());
        dto.put("sellerId", order.getSellerId());
        dto.put("accountId", order.getAccountId());
        dto.put("price", order.getPrice());
        dto.put("status", order.getStatus());
        dto.put("createTime", order.getCreateTime());
        return Result.success(dto);
    }

    @DeleteMapping("/order/delete/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.removeById(id);
        return Result.ok("删除成功");
    }
}
