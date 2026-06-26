package com.example.account.controller;

import com.example.common.Result;
import com.example.entity.GameAccount;
import com.example.entity.Registration;
import com.example.account.service.GameAccountService;
import com.example.account.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员端点 — 账号管理 + 审批
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private GameAccountService gameAccountService;

    // ========== 登记审批 ==========

    @GetMapping("/registrations")
    public Result<List<Registration>> registrations(@RequestParam(required = false) String status) {
        if ("PENDING".equals(status)) {
            return Result.success(registrationService.listPending());
        }
        return Result.success(registrationService.listAll());
    }

    @PostMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable Long id) {
        registrationService.approve(id);
        return Result.ok("审批通过，账号已上架");
    }

    @PostMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id) {
        registrationService.reject(id);
        return Result.ok("已驳回");
    }

    @GetMapping("/registration/detail/{id}")
    public Result<Registration> getRegistrationDetail(@PathVariable Long id) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.error(404, "登记记录不存在");
        }
        return Result.success(registration);
    }

    @DeleteMapping("/registration/delete/{id}")
    public Result<Void> deleteRegistration(@PathVariable Long id) {
        registrationService.removeById(id);
        return Result.ok("删除成功");
    }

    // ========== 账号管理 ==========

    @GetMapping("/account/detail/{id}")
    public Result<GameAccount> getAccountDetail(@PathVariable Long id) {
        GameAccount account = gameAccountService.getDetail(id);
        if (account == null) {
            return Result.error(404, "账号不存在");
        }
        return Result.success(account);
    }

    @PutMapping("/account/update")
    public Result<GameAccount> updateAccount(@RequestBody GameAccount request) {
        try {
            GameAccount existing = gameAccountService.getById(request.getId());
            if (existing == null) {
                return Result.error(404, "账号不存在");
            }
            existing.setGameName(request.getGameName());
            existing.setServer(request.getServer());
            existing.setAccountLevel(request.getAccountLevel());
            existing.setPrice(request.getPrice());
            existing.setOriginalPrice(request.getOriginalPrice());
            existing.setSkinCount(request.getSkinCount());
            existing.setStatus(request.getStatus());
            existing.setDescription(request.getDescription());
            existing.setAllowBargain(request.getAllowBargain());
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                existing.setImages(request.getImages());
            }
            gameAccountService.updateAccount(existing);
            return Result.success("更新成功", existing);
        } catch (Exception e) {
            return Result.error(500, "更新失败：" + e.getMessage());
        }
    }

    @PostMapping("/account/offline/{id}")
    public Result<Void> offlineAccount(@PathVariable Long id) {
        GameAccount existing = gameAccountService.getById(id);
        if (existing == null) {
            return Result.error(404, "账号不存在");
        }
        existing.setStatus("OFFLINE");
        gameAccountService.updateAccount(existing);
        return Result.ok("下架成功");
    }

    @PostMapping("/account/online/{id}")
    public Result<Void> onlineAccount(@PathVariable Long id) {
        GameAccount existing = gameAccountService.getById(id);
        if (existing == null) {
            return Result.error(404, "账号不存在");
        }
        existing.setStatus("LISTED");
        gameAccountService.updateAccount(existing);
        return Result.ok("上架成功");
    }
}
