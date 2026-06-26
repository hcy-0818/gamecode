package com.example.bargain.controller;

import com.example.common.Result;
import com.example.common.context.UserContext;
import com.example.dto.BargainDTO;
import com.example.entity.Bargain;
import com.example.bargain.service.BargainService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bargain")
public class BargainController {

    @Autowired
    private BargainService bargainService;

    @PostMapping("/create")
    public Result<Bargain> create(@Valid @RequestBody CreateBargainRequest req) {
        Long userId = UserContext.getUserId();
        Bargain bargain = bargainService.createBargain(userId, req.getAccountId(), req.getPrice());
        return Result.success("还价成功，等待卖家回复", bargain);
    }

    @PostMapping("/accept/{id}")
    public Result<Void> accept(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        bargainService.acceptBargain(userId, id);
        return Result.ok("已同意还价");
    }

    @PostMapping("/counter/{id}")
    public Result<Void> counter(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        Long userId = UserContext.getUserId();
        BigDecimal price = body.get("price");
        bargainService.counterBargain(userId, id, price);
        return Result.ok("已还价，等待买家确认");
    }

    @PostMapping("/buyerAccept/{id}")
    public Result<Void> buyerAccept(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        bargainService.buyerAccept(userId, id);
        return Result.ok("已接受还价");
    }

    @GetMapping("/myBargains")
    public Result<List<BargainDTO>> myBargains() {
        Long userId = UserContext.getUserId();
        return Result.success(bargainService.listMyBargains(userId));
    }

    @GetMapping("/receivedBargains")
    public Result<List<BargainDTO>> receivedBargains() {
        Long userId = UserContext.getUserId();
        return Result.success(bargainService.listReceivedBargains(userId));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        bargainService.deleteBargain(userId, id);
        return Result.ok("删除成功");
    }

    // ========== Feign 内部调用端点 ==========

    @GetMapping("/feign/{id}")
    public Result<BargainDTO> feignGetById(@PathVariable Long id) {
        Bargain bargain = bargainService.getById(id);
        if (bargain == null) {
            return Result.error(404, "还价记录不存在");
        }
        BargainDTO dto = new BargainDTO();
        dto.setId(bargain.getId());
        dto.setAccountId(bargain.getAccountId());
        dto.setBuyerId(bargain.getBuyerId());
        dto.setSellerId(bargain.getSellerId());
        dto.setBuyerPrice(bargain.getBuyerPrice());
        dto.setSellerPrice(bargain.getSellerPrice());
        dto.setStatus(bargain.getStatus());
        dto.setCreateTime(bargain.getCreateTime());
        return Result.success(dto);
    }

    @GetMapping("/feign/by-account-and-buyer")
    public Result<BargainDTO> feignGetByAccountAndBuyer(@RequestParam Long accountId, @RequestParam Long buyerId) {
        Bargain bargain = bargainService.getByAccountAndBuyer(accountId, buyerId);
        if (bargain == null) {
            return Result.success(null);
        }
        BargainDTO dto = new BargainDTO();
        dto.setId(bargain.getId());
        dto.setAccountId(bargain.getAccountId());
        dto.setBuyerId(bargain.getBuyerId());
        dto.setSellerId(bargain.getSellerId());
        dto.setBuyerPrice(bargain.getBuyerPrice());
        dto.setSellerPrice(bargain.getSellerPrice());
        dto.setStatus(bargain.getStatus());
        dto.setCreateTime(bargain.getCreateTime());
        return Result.success(dto);
    }

    @PutMapping("/feign/update-status-by-account")
    public Result<Void> feignUpdateStatusByAccount(@RequestParam Long accountId, @RequestParam String status) {
        bargainService.updateBargainStatusByAccount(accountId, status);
        return Result.ok("ok");
    }

    @DeleteMapping("/feign/by-account/{accountId}")
    public Result<Void> feignDeleteByAccountId(@PathVariable Long accountId) {
        bargainService.deleteByAccountId(accountId);
        return Result.ok("ok");
    }
}

@Data
class CreateBargainRequest {
    @NotNull(message = "账号ID不能为空")
    private Long accountId;
    @NotNull(message = "还价金额不能为空")
    @Min(value = 0, message = "金额必须大于0")
    private BigDecimal price;
}
