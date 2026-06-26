package com.example.account.controller;

import com.example.common.Result;
import com.example.common.context.UserContext;
import com.example.entity.GameAccount;
import com.example.account.service.GameAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/account")
public class GameAccountController {

    @Autowired
    private GameAccountService gameAccountService;

    /** 管理员创建账号 */
    @PostMapping("/create")
    public Result<GameAccount> create(@RequestBody GameAccount account) {
        if (account.getOriginalPrice() == null) {
            account.setOriginalPrice(account.getPrice());
        }
        if (account.getAllowBargain() == null) {
            account.setAllowBargain(0);
        }
        if (account.getStatus() == null) {
            account.setStatus("LISTED");
        }
        account.setSellerId(1L);
        gameAccountService.createAccount(account);
        return Result.success("创建成功", account);
    }

    /** 首页：获取所有上架中的账号 */
    @GetMapping("/list")
    public Result<List<GameAccount>> list() {
        return Result.success(gameAccountService.listListed());
    }

    /** 获取账号详情 */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        GameAccount account = gameAccountService.getDetail(id);
        if (account == null) {
            return Result.error(404, "账号不存在");
        }

        Long userId = UserContext.getUserId();
        String role = UserContext.getRole();

        Map<String, Object> result = new HashMap<>();
        result.put("account", account);

        if (userId != null && "ADMIN".equals(role)) {
            return Result.success(result);
        }

        if (userId != null && userId.equals(account.getSellerId())) {
            BigDecimal bargainPrice = gameAccountService.getBargainPrice(id, userId);
            if (bargainPrice != null) {
                result.put("bargainPrice", bargainPrice);
            }
            return Result.success(result);
        }

        if (!"LISTED".equals(account.getStatus())) {
            return Result.error(404, "账号不存在或已下架");
        }

        if (userId != null) {
            BigDecimal bargainPrice = gameAccountService.getBargainPrice(id, userId);
            if (bargainPrice != null) {
                result.put("bargainPrice", bargainPrice);
            }
        }

        return Result.success(result);
    }

    /** 我的出售列表 */
    @GetMapping("/mySales")
    public Result<List<GameAccount>> mySales(@RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        return Result.success(gameAccountService.listMySales(userId, status));
    }

    /** 取回账号 */
    @PostMapping("/retrieve/{id}")
    public Result<Void> retrieve(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        gameAccountService.retrieve(userId, id);
        return Result.ok("取回成功");
    }

    /** 删除账号 */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        gameAccountService.delete(userId, id);
        return Result.ok("删除成功");
    }

    private static final String IMAGE_DIR = "uploads/account_images/";

    /** 上传账号图片 */
    @PostMapping("/uploadImages")
    public Result<List<String>> uploadImages(@RequestPart("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Result.error(400, "请选择要上传的图片");
        }
        try {
            Path dirPath = Paths.get(IMAGE_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            List<String> imagePaths = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return Result.error(400, "只支持图片格式");
                }
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";
                String filename = UUID.randomUUID().toString() + extension;
                Path filePath = dirPath.resolve(filename);
                Files.copy(file.getInputStream(), filePath);
                imagePaths.add("/api/account/image/" + filename);
            }
            return Result.success("上传成功", imagePaths);
        } catch (IOException e) {
            return Result.error(500, "上传失败：" + e.getMessage());
        }
    }

    /** 获取账号图片 */
    @GetMapping("/image/{filename}")
    public void getImage(@PathVariable String filename, jakarta.servlet.http.HttpServletResponse response) {
        String filePath = IMAGE_DIR + filename;
        File file = new File(filePath);
        if (!file.exists()) {
            response.setStatus(404);
            return;
        }
        try {
            String contentType = Files.probeContentType(file.toPath());
            response.setContentType(contentType != null ? contentType : "image/png");
            Files.copy(file.toPath(), response.getOutputStream());
        } catch (IOException e) {
            response.setStatus(500);
        }
    }

    // ========== Feign 内部调用端点 ==========

    @GetMapping("/feign/{id}")
    public Result<GameAccount> feignGetById(@PathVariable Long id) {
        return Result.success(gameAccountService.getById(id));
    }

    @PostMapping("/feign/mark-sold/{id}")
    public Result<Void> feignMarkAsSold(@PathVariable Long id) {
        gameAccountService.markAsSold(id);
        return Result.ok("ok");
    }

    @PutMapping("/feign/restore/{id}")
    public Result<Void> feignRestoreToListed(@PathVariable Long id) {
        gameAccountService.restoreToListed(id);
        return Result.ok("ok");
    }

    @GetMapping("/feign/bargain-price")
    public Result<BigDecimal> feignGetBargainPrice(@RequestParam Long accountId, @RequestParam Long buyerId) {
        return Result.success(gameAccountService.getBargainPrice(accountId, buyerId));
    }
}
