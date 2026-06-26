package com.example.bargain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.Result;
import com.example.dto.BargainDTO;
import com.example.entity.Bargain;
import com.example.entity.GameAccount;
import com.example.bargain.feign.AccountFeignClient;
import com.example.bargain.feign.OrderFeignClient;
import com.example.bargain.mapper.BargainMapper;
import com.example.bargain.service.BargainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BargainServiceImpl extends ServiceImpl<BargainMapper, Bargain> implements BargainService {

    @Autowired
    private AccountFeignClient accountFeignClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Override
    @Transactional
    @CacheEvict(value = {"bargain:buyer", "bargain:seller"}, allEntries = true)
    public Bargain createBargain(Long buyerId, Long accountId, BigDecimal buyerPrice) {
        Result<GameAccount> result = accountFeignClient.getById(accountId);
        if (result == null || result.getData() == null) {
            throw new RuntimeException("账号不存在");
        }
        GameAccount account = result.getData();
        if (!"LISTED".equals(account.getStatus())) {
            throw new RuntimeException("该账号不可还价");
        }
        if (account.getAllowBargain() == null || account.getAllowBargain() != 1) {
            throw new RuntimeException("该账号不支持还价");
        }
        if (account.getSellerId().equals(buyerId)) {
            throw new RuntimeException("不能对自己的账号还价");
        }

        BigDecimal minPrice = account.getPrice().multiply(new BigDecimal("0.8"));
        if (buyerPrice.compareTo(minPrice) < 0) {
            throw new RuntimeException("还价不能低于原价的80%（最低：" + minPrice + "）");
        }

        LambdaQueryWrapper<Bargain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bargain::getAccountId, accountId)
               .eq(Bargain::getBuyerId, buyerId)
               .eq(Bargain::getStatus, "PENDING");
        if (count(wrapper) > 0) {
            throw new RuntimeException("您已有进行中的还价");
        }

        Bargain bargain = new Bargain();
        bargain.setAccountId(accountId);
        bargain.setBuyerId(buyerId);
        bargain.setSellerId(account.getSellerId());
        bargain.setBuyerPrice(buyerPrice);
        bargain.setStatus("PENDING");
        save(bargain);
        return bargain;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"bargain:buyer", "bargain:seller"}, allEntries = true)
    public void acceptBargain(Long sellerId, Long bargainId) {
        Bargain bargain = getById(bargainId);
        if (bargain == null || !bargain.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"PENDING".equals(bargain.getStatus())) {
            throw new RuntimeException("还价状态异常");
        }

        // 1. 本地事务：更新还价状态
        bargain.setStatus("ACCEPTED");
        bargain.setSellerPrice(bargain.getBuyerPrice());
        updateById(bargain);

        // 2. Feign 调用 order-service 创建订单（补偿模式）
        try {
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("buyerId", bargain.getBuyerId());
            orderRequest.put("accountId", bargain.getAccountId());
            orderRequest.put("sellerId", bargain.getSellerId());
            orderRequest.put("price", bargain.getBuyerPrice());
            orderFeignClient.createFromBargain(orderRequest);
        } catch (Exception e) {
            log.error("还价接受后创建订单失败 (bargainId={}): {}", bargainId, e.getMessage());
            // 补偿：回滚还价状态
            bargain.setStatus("PENDING");
            bargain.setSellerPrice(null);
            updateById(bargain);
            throw new RuntimeException("订单创建失败，已回滚还价状态");
        }

        // 3. Feign 调用 account-service 标记售出
        try {
            accountFeignClient.markAsSold(bargain.getAccountId());
        } catch (Exception e) {
            log.error("标记账号售出失败 (accountId={}): {}", bargain.getAccountId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"bargain:buyer", "bargain:seller"}, allEntries = true)
    public void counterBargain(Long sellerId, Long bargainId, BigDecimal sellerPrice) {
        Bargain bargain = getById(bargainId);
        if (bargain == null || !bargain.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"PENDING".equals(bargain.getStatus())) {
            throw new RuntimeException("还价状态异常");
        }

        Result<GameAccount> result = accountFeignClient.getById(bargain.getAccountId());
        GameAccount account = result != null ? result.getData() : null;

        BigDecimal minPrice = bargain.getBuyerPrice().multiply(new BigDecimal("0.8"));
        if (sellerPrice.compareTo(minPrice) < 0) {
            throw new RuntimeException("还价不得低于买家出价的80%");
        }
        if (account != null && sellerPrice.compareTo(account.getPrice()) > 0) {
            throw new RuntimeException("还价不能高于原价");
        }

        bargain.setSellerPrice(sellerPrice);
        updateById(bargain);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"bargain:buyer", "bargain:seller"}, allEntries = true)
    public void buyerAccept(Long buyerId, Long bargainId) {
        Bargain bargain = getById(bargainId);
        if (bargain == null || !bargain.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"PENDING".equals(bargain.getStatus())) {
            throw new RuntimeException("还价状态异常");
        }
        if (bargain.getSellerPrice() == null) {
            throw new RuntimeException("卖家尚未还价");
        }

        // 1. 本地事务更新
        bargain.setStatus("ACCEPTED");
        updateById(bargain);

        // 2. Feign 调用 order-service 创建订单
        try {
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("buyerId", bargain.getBuyerId());
            orderRequest.put("accountId", bargain.getAccountId());
            orderRequest.put("sellerId", bargain.getSellerId());
            orderRequest.put("price", bargain.getSellerPrice());
            orderFeignClient.createFromBargain(orderRequest);
        } catch (Exception e) {
            log.error("买家接受还价后创建订单失败 (bargainId={}): {}", bargainId, e.getMessage());
            bargain.setStatus("PENDING");
            updateById(bargain);
            throw new RuntimeException("订单创建失败，已回滚还价状态");
        }

        // 3. Feign 标记售出
        try {
            accountFeignClient.markAsSold(bargain.getAccountId());
        } catch (Exception e) {
            log.error("标记账号售出失败 (accountId={}): {}", bargain.getAccountId(), e.getMessage());
        }
    }

    @Override
    public List<BargainDTO> listMyBargains(Long buyerId) {
        LambdaQueryWrapper<Bargain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bargain::getBuyerId, buyerId)
               .orderByDesc(Bargain::getCreateTime);
        return list(wrapper).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<BargainDTO> listReceivedBargains(Long sellerId) {
        LambdaQueryWrapper<Bargain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bargain::getSellerId, sellerId)
               .orderByDesc(Bargain::getCreateTime);
        return list(wrapper).stream().map(this::toDTO).collect(Collectors.toList());
    }

    private BargainDTO toDTO(Bargain bargain) {
        BargainDTO dto = new BargainDTO();
        dto.setId(bargain.getId());
        dto.setAccountId(bargain.getAccountId());
        dto.setBuyerId(bargain.getBuyerId());
        dto.setSellerId(bargain.getSellerId());
        dto.setBuyerPrice(bargain.getBuyerPrice());
        dto.setSellerPrice(bargain.getSellerPrice());
        dto.setStatus(bargain.getStatus());
        dto.setCreateTime(bargain.getCreateTime());

        try {
            Result<GameAccount> result = accountFeignClient.getById(bargain.getAccountId());
            if (result != null && result.getData() != null) {
                GameAccount account = result.getData();
                dto.setGameName(account.getGameName());
                dto.setAccountLevel(account.getAccountLevel());
                dto.setOriginalPrice(account.getPrice());
            }
        } catch (Exception e) {
            log.warn("获取账号信息失败 (accountId={}): {}", bargain.getAccountId(), e.getMessage());
        }

        return dto;
    }

    @Override
    public Bargain getByAccountAndBuyer(Long accountId, Long buyerId) {
        LambdaQueryWrapper<Bargain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bargain::getAccountId, accountId)
               .eq(Bargain::getBuyerId, buyerId)
               .eq(Bargain::getStatus, "ACCEPTED");
        return getOne(wrapper);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"bargain:buyer", "bargain:seller"}, allEntries = true)
    public void updateBargainStatusByAccount(Long accountId, String status) {
        LambdaQueryWrapper<Bargain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bargain::getAccountId, accountId)
               .in(Bargain::getStatus, "PENDING", "ACCEPTED");
        List<Bargain> bargains = list(wrapper);
        for (Bargain bargain : bargains) {
            bargain.setStatus(status);
            updateById(bargain);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"bargain:buyer", "bargain:seller"}, allEntries = true)
    public void deleteBargain(Long userId, Long bargainId) {
        Bargain bargain = getById(bargainId);
        if (bargain == null) {
            throw new RuntimeException("还价记录不存在");
        }
        if (!bargain.getBuyerId().equals(userId) && !bargain.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        removeById(bargainId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"bargain:buyer", "bargain:seller"}, allEntries = true)
    public void deleteByAccountId(Long accountId) {
        LambdaQueryWrapper<Bargain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bargain::getAccountId, accountId);
        remove(wrapper);
    }
}
