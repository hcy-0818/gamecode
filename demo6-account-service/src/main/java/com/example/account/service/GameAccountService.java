package com.example.account.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.GameAccount;

import java.math.BigDecimal;
import java.util.List;

public interface GameAccountService extends IService<GameAccount> {
    List<GameAccount> listListed();
    GameAccount getDetail(Long id);
    List<GameAccount> listMySales(Long userId, String status);
    void markAsSold(Long accountId);
    void retrieve(Long userId, Long accountId);
    void delete(Long userId, Long accountId);
    void updatePrice(Long accountId, Long buyerId, BigDecimal newPrice);
    BigDecimal getBargainPrice(Long accountId, Long buyerId);
    void restoreToListed(Long accountId);
    void updateAccount(GameAccount account);
    void createAccount(GameAccount account);
}
