package com.example.order.controller;

import com.example.common.Result;
import com.example.common.context.UserContext;
import com.example.entity.Order;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@RequestBody Map<String, Long> body) {
        Long userId = UserContext.getUserId();
        Long accountId = body.get("accountId");
        Order order = orderService.createOrder(userId, accountId);
        return Result.success("下单成功", order);
    }

    @PostMapping("/createWithBargain")
    public Result<Order> createWithBargain(@RequestBody Map<String, Long> body) {
        Long userId = UserContext.getUserId();
        Long bargainId = body.get("bargainId");
        Order order = orderService.createOrderWithBargain(userId, bargainId);
        return Result.success("下单成功，使用还价价格", order);
    }

    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        orderService.payOrder(userId, id);
        return Result.ok("支付成功");
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        orderService.cancelOrder(userId, id);
        return Result.ok("已取消");
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        orderService.deleteOrder(userId, id);
        return Result.ok("已删除");
    }

    @GetMapping("/myOrders")
    public Result<List<Order>> myOrders(@RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        if ("ALL".equals(status) || status == null) {
            status = null;
        }
        return Result.success(orderService.listMyOrders(userId, status));
    }

    @GetMapping("/detail/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }

    // ========== Feign 内部调用端点 ==========

    @PostMapping("/feign/create-from-bargain")
    public Result<Order> feignCreateFromBargain(@RequestBody Map<String, Object> request) {
        Long buyerId = Long.valueOf(request.get("buyerId").toString());
        Long accountId = Long.valueOf(request.get("accountId").toString());
        Long sellerId = Long.valueOf(request.get("sellerId").toString());
        BigDecimal price = new BigDecimal(request.get("price").toString());
        Order order = orderService.createFromBargain(buyerId, accountId, sellerId, price);
        return Result.success(order);
    }
}
