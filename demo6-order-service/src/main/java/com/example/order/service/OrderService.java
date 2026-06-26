package com.example.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Order;
import java.util.List;

public interface OrderService extends IService<Order> {
    Order createOrder(Long buyerId, Long accountId);
    Order createOrderWithBargain(Long buyerId, Long bargainId);
    void cancelOrder(Long userId, Long orderId);
    void deleteOrder(Long userId, Long orderId);
    List<Order> listMyOrders(Long userId, String status);
    void payOrder(Long userId, Long orderId);
    Order getDetail(Long orderId);
    Order createFromBargain(Long buyerId, Long accountId, Long sellerId, java.math.BigDecimal price);
}
