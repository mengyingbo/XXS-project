package com.xxs.game.controller;

import com.xxs.game.common.Result;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Prize;
import com.xxs.game.service.AdminPrizeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端奖品管理与兑换审核（需求文档 8.2）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPrizeController {

    private final AdminPrizeService adminPrizeService;

    // ---------------- 奖品 ----------------

    @GetMapping("/prize")
    public Result<Map<String, Object>> listPrizes(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Boolean enabled) {
        return Result.ok(adminPrizeService.listPrizes(page, size, keyword, enabled));
    }

    @PostMapping("/prize")
    public Result<Prize> createPrize(@Valid @RequestBody AdminDtos.PrizeSaveReq req) {
        return Result.ok(adminPrizeService.createPrize(req));
    }

    @PutMapping("/prize/{id}")
    public Result<Prize> updatePrize(@PathVariable Long id, @Valid @RequestBody AdminDtos.PrizeSaveReq req) {
        return Result.ok(adminPrizeService.updatePrize(id, req));
    }

    @DeleteMapping("/prize/{id}")
    public Result<Void> deletePrize(@PathVariable Long id) {
        adminPrizeService.deletePrize(id);
        return Result.ok();
    }

    // ---------------- 兑换审核 ----------------

    @GetMapping("/redeem")
    public Result<Map<String, Object>> listRedeems(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) Long childId) {
        return Result.ok(adminPrizeService.listRedeems(page, size, status, childId));
    }

    /** 通过 / 拒绝（退回积分与库存）/ 标记已发放 */
    @PutMapping("/redeem/{id}")
    public Result<Map<String, Object>> handleRedeem(@PathVariable Long id,
                                                    @Valid @RequestBody AdminDtos.RedeemHandleReq req) {
        return Result.ok(adminPrizeService.handleRedeem(id, req));
    }
}