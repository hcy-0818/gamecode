package com.example.bargain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.BargainDTO;
import com.example.entity.Bargain;

import java.math.BigDecimal;
import java.util.List;

public interface BargainService extends IService<Bargain> {
    Bargain createBargain(Long buyerId, Long accountId, BigDecimal buyerPrice);
    void acceptBargain(Long sellerId, Long bargainId);
    void counterBargain(Long sellerId, Long bargainId, BigDecimal sellerPrice);
    void buyerAccept(Long buyerId, Long bargainId);
    List<BargainDTO> listMyBargains(Long buyerId);
    List<BargainDTO> listReceivedBargains(Long sellerId);
    Bargain getByAccountAndBuyer(Long accountId, Long buyerId);
    void updateBargainStatusByAccount(Long accountId, String status);
    void deleteBargain(Long userId, Long bargainId);
    void deleteByAccountId(Long accountId);
}
