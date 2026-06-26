package com.example.account.controller;

import com.example.common.Result;
import com.example.common.context.UserContext;
import com.example.entity.Registration;
import com.example.account.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/submit")
    public Result<Registration> submit(@RequestBody Registration registration) {
        Long userId = UserContext.getUserId();
        Registration result = registrationService.submit(userId, registration);
        return Result.success("登记申请已提交，等待管理员审批", result);
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        registrationService.cancel(userId, id);
        return Result.ok("已取消登记");
    }

    @GetMapping("/myRegistrations")
    public Result<List<Registration>> myRegistrations() {
        Long userId = UserContext.getUserId();
        return Result.success(registrationService.listMyRegistrations(userId));
    }

    @GetMapping("/detail/{id}")
    public Result<Registration> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        try {
            Registration registration = registrationService.getById(id);
            if (registration == null) {
                return Result.error(404, "登记记录不存在");
            }
            if (!registration.getUserId().equals(userId)) {
                return Result.error(403, "无权查看此登记记录");
            }
            return Result.success(registration);
        } catch (Exception e) {
            return Result.error(500, "获取详情失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        registrationService.delete(userId, id);
        return Result.ok("删除成功");
    }
}
