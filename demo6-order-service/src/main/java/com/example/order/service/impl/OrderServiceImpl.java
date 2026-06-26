package com.example.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.Result;
import com.example.entity.GameAccount;
import com.example.entity.Order;
import com.example.order.feign.AccountFeignClient;
import com.example.order.feign.BargainFeignClient;
import com.example.order.mapper.OrderMapper;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private AccountFeignClient accountFeignClient;

    @Autowired
    private BargainFeignClient bargainFeignClient;

    @Override
    @Transactional
    @CacheEvict(value = "order:user", allEntries = true)
    public Order createOrder(Long buyerId, Long accountId) {
        Result<GameAccount> result = accountFeignClient.getById(accountId);
        if (result == null || result.getData() == null) {
            throw new RuntimeException("账号不存在");
        }
        GameAccount account = result.getData();
        if (!"LISTED".equals(account.getStatus())) {
            throw new RuntimeException("该账号已下架或已出售");
        }
        if (account.getSellerId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己的账号");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(account.getSellerId());
        order.setAccountId(accountId);
        order.setPrice(account.getPrice());
        order.setStatus("PENDING_PAYMENT");
        save(order);

        // Feign 调用：标记账号为已出售
        try {
            accountFeignClient.markAsSold(accountId);
        } catch (Exception e) {
            // 补偿：恢复订单状态或记录日志
            System.err.println("标记账号售出失败 (accountId=" + accountId + "): " + e.getMessage());
        }

        return order;
    }

    @Override
    @Transactional
    @CacheEvict(value = "order:user", allEntries = true)
    public Order createOrderWithBargain(Long buyerId, Long bargainId) {
        Result<com.example.dto.BargainDTO> bargainResult = bargainFeignClient.getById(bargainId);
        if (bargainResult == null || bargainResult.getData() == null) {
            throw new RuntimeException("还价记录不存在");
        }
        com.example.dto.BargainDTO bargain = bargainResult.getData();
        if (!bargain.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"ACCEPTED".equals(bargain.getStatus())) {
            throw new RuntimeException("还价未被接受");
        }

        Result<GameAccount> accountResult = accountFeignClient.getById(bargain.getAccountId());
        if (accountResult == null || accountResult.getData() == null) {
            throw new RuntimeException("账号不存在");
        }
        GameAccount account = accountResult.getData();
        if (!"LISTED".equals(account.getStatus())) {
            throw new RuntimeException("该账号已下架或已出售");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(account.getSellerId());
        order.setAccountId(bargain.getAccountId());
        order.setPrice(bargain.getSellerPrice() != null ? bargain.getSellerPrice() : bargain.getBuyerPrice());
        order.setStatus("PENDING_PAYMENT");
        save(order);

        try {
            accountFeignClient.markAsSold(bargain.getAccountId());
        } catch (Exception e) {
            System.err.println("标记账号售出失败: " + e.getMessage());
        }

        try {
            bargainFeignClient.updateStatusByAccount(bargain.getAccountId(), "COMPLETED");
        } catch (Exception e) {
            System.err.println("更新还价状态失败: " + e.getMessage());
        }

        return order;
    }

    @Override
    @Transactional
    @CacheEvict(value = "order:user", allEntries = true)
    public Order createFromBargain(Long buyerId, Long accountId, Long sellerId, BigDecimal price) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setAccountId(accountId);
        order.setPrice(price);
        order.setStatus("PENDING_PAYMENT");
        save(order);

        try {
            accountFeignClient.markAsSold(accountId);
        } catch (Exception e) {
            System.err.println("标记账号售出失败: " + e.getMessage());
        }
        return order;
    }

    @Override
    @Transactional
    @CacheEvict(value = "order:user", allEntries = true)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = getById(orderId);
        if (order == null || !order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("仅待付款订单可取消");
        }
        order.setStatus("CANCELLED");
        updateById(order);

        // Feign：恢复账号为上架
        try {
            accountFeignClient.restoreToListed(order.getAccountId());
        } catch (Exception e) {
            System.err.println("恢复账号上架失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "order:user", allEntries = true)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = getById(orderId);
        if (order == null || !order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        removeById(orderId);
    }

    @Override
    public List<Order> listMyOrders(Long userId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    @CacheEvict(value = "order:user", allEntries = true)
    public void payOrder(Long userId, Long orderId) {
        Order order = getById(orderId);
        if (order == null || !order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态异常");
        }
        order.setStatus("PAID");
        updateById(order);

        try {
            bargainFeignClient.updateStatusByAccount(order.getAccountId(), "COMPLETED");
        } catch (Exception e) {
            System.err.println("更新还价状态失败: " + e.getMessage());
        }
    }

    @Override
    public Order getDetail(Long orderId) {
        return getById(orderId);
    }

    private String generateOrderNo() {
        String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "GT" + datetime + uuid;
    }
}
