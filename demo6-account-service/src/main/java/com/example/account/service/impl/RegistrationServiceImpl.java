package com.example.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.account.mapper.RegistrationMapper;
import com.example.account.service.GameAccountService;
import com.example.account.service.RegistrationService;
import com.example.entity.GameAccount;
import com.example.entity.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    @Autowired
    private GameAccountService gameAccountService;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Override
    @Transactional
    public Registration submit(Long userId, Registration registration) {
        registration.setUserId(userId);
        registration.setStatus("PENDING");
        save(registration);
        return registration;
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long registrationId) {
        Registration registration = getById(registrationId);
        if (registration == null || !registration.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"PENDING".equals(registration.getStatus())) {
            throw new RuntimeException("仅待审批的登记可取消");
        }
        registration.setStatus("CANCELLED");
        updateById(registration);
    }

    @Override
    public List<Registration> listMyRegistrations(Long userId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
               .orderByDesc(Registration::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Registration> listPending() {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getStatus, "PENDING")
               .orderByAsc(Registration::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Registration> listAll() {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Registration::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void approve(Long registrationId) {
        Registration registration = getById(registrationId);
        if (registration == null || !"PENDING".equals(registration.getStatus())) {
            throw new RuntimeException("登记状态异常");
        }

        GameAccount account = new GameAccount();
        account.setSellerId(registration.getUserId());
        account.setGameName(registration.getGameName());
        account.setServer(registration.getServer());
        account.setAccountLevel(registration.getAccountLevel());
        account.setSkinCount(registration.getSkinCount());
        account.setPrice(registration.getPrice());
        account.setOriginalPrice(registration.getPrice());
        account.setDescription(registration.getDescription());
        account.setAllowBargain(registration.getAllowBargain());
        account.setImages(registration.getImages());
        account.setStatus("LISTED");
        gameAccountService.save(account);

        registration.setStatus("APPROVED");
        registration.setAccountId(account.getId());
        updateById(registration);

        // 手动清除缓存
        if (cacheManager != null) {
            org.springframework.cache.Cache cache = cacheManager.getCache("account:listed:all");
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @Override
    @Transactional
    public void reject(Long registrationId) {
        Registration registration = getById(registrationId);
        if (registration == null || !"PENDING".equals(registration.getStatus())) {
            throw new RuntimeException("登记状态异常");
        }
        registration.setStatus("REJECTED");
        updateById(registration);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long registrationId) {
        Registration registration = getById(registrationId);
        if (registration == null || !registration.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        String status = registration.getStatus();
        if (!"APPROVED".equals(status) && !"CANCELLED".equals(status) && !"REJECTED".equals(status)) {
            throw new RuntimeException("仅已通过、已取消或已驳回的登记可删除");
        }
        removeById(registrationId);
    }
}
