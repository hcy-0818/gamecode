package com.example.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.account.mapper.GameAccountMapper;
import com.example.account.service.GameAccountService;
import com.example.common.Result;
import com.example.dto.BargainDTO;
import com.example.entity.GameAccount;
import com.example.account.feign.BargainFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GameAccountServiceImpl extends ServiceImpl<GameAccountMapper, GameAccount> implements GameAccountService {

    @Autowired
    private BargainFeignClient bargainFeignClient;

    @Autowired(required = false)
    private CacheManager cacheManager;

    /** 清除账号相关缓存 */
    private void evictAccountCache() {
        if (cacheManager != null) {
            for (String name : List.of("account:listed:all", "account:detail")) {
                org.springframework.cache.Cache cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }

    @Override
    @Cacheable(value = "account:listed:all", unless = "#result == null || #result.isEmpty()")
    public List<GameAccount> listListed() {
        LambdaQueryWrapper<GameAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GameAccount::getStatus, "LISTED")
               .orderByDesc(GameAccount::getCreateTime);
        return list(wrapper).stream().map(account -> {
            GameAccount vo = new GameAccount();
            vo.setId(account.getId());
            vo.setSellerId(account.getSellerId());
            vo.setGameName(account.getGameName());
            vo.setAccountLevel(account.getAccountLevel());
            vo.setPrice(account.getPrice());
            vo.setOriginalPrice(account.getOriginalPrice());
            vo.setDescription(account.getDescription());
            vo.setAllowBargain(account.getAllowBargain());
            vo.setImages(account.getImages());
            vo.setStatus(account.getStatus());
            vo.setCreateTime(account.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "account:detail", key = "#id", unless = "#result == null")
    public GameAccount getDetail(Long id) {
        return getById(id);
    }

    @Override
    public List<GameAccount> listMySales(Long userId, String status) {
        LambdaQueryWrapper<GameAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GameAccount::getSellerId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GameAccount::getStatus, status);
        }
        wrapper.orderByDesc(GameAccount::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void markAsSold(Long accountId) {
        GameAccount account = getById(accountId);
        if (account == null || !"LISTED".equals(account.getStatus())) {
            throw new RuntimeException("账号状态异常，无法出售");
        }
        account.setStatus("SOLD");
        updateById(account);
        evictAccountCache();
    }

    @Override
    @Transactional
    public void restoreToListed(Long accountId) {
        GameAccount account = getById(accountId);
        if (account != null && "SOLD".equals(account.getStatus())) {
            account.setStatus("LISTED");
            updateById(account);
            evictAccountCache();
        }
    }

    @Override
    @Transactional
    public void retrieve(Long userId, Long accountId) {
        GameAccount account = getById(accountId);
        if (account == null || !account.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"LISTED".equals(account.getStatus())) {
            throw new RuntimeException("仅上架中账号可取回");
        }
        account.setStatus("RETRIEVED");
        updateById(account);
        evictAccountCache();
    }

    @Override
    @Transactional
    public void delete(Long userId, Long accountId) {
        GameAccount account = getById(accountId);
        if (account == null || !account.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        // 调用 bargain-service 删除关联还价
        try {
            bargainFeignClient.deleteByAccountId(accountId);
        } catch (Exception e) {
            // 补偿失败记录日志，不阻断主流程
            System.err.println("删除还价记录失败 (accountId=" + accountId + "): " + e.getMessage());
        }
        removeById(accountId);
        evictAccountCache();
    }

    @Override
    public void updatePrice(Long accountId, Long buyerId, BigDecimal newPrice) {
        // 价格通过还价表关联，此处不更新实际DB
    }

    @Override
    public void updateAccount(GameAccount account) {
        updateById(account);
        evictAccountCache();
    }

    @Override
    public void createAccount(GameAccount account) {
        save(account);
        evictAccountCache();
    }

    @Override
    public BigDecimal getBargainPrice(Long accountId, Long buyerId) {
        try {
            Result<BargainDTO> result = bargainFeignClient.getByAccountAndBuyer(accountId, buyerId);
            if (result != null && result.getData() != null) {
                BargainDTO bargain = result.getData();
                if ("ACCEPTED".equals(bargain.getStatus())) {
                    if (bargain.getSellerPrice() != null) {
                        return bargain.getSellerPrice();
                    }
                    return bargain.getBuyerPrice();
                }
            }
        } catch (Exception e) {
            System.err.println("获取还价价格失败: " + e.getMessage());
        }
        return null;
    }
}
