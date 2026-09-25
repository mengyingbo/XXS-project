package com.xxs.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Child;
import com.xxs.game.entity.Prize;
import com.xxs.game.entity.RedeemOrder;
import com.xxs.game.mapper.ChildMapper;
import com.xxs.game.mapper.PrizeMapper;
import com.xxs.game.mapper.RedeemOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端奖品管理与兑换审核（需求文档 6.2 F-AD-07、F-AD-08）
 */
@Service
@RequiredArgsConstructor
public class AdminPrizeService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_DELIVERED = "DELIVERED";

    private final PrizeMapper prizeMapper;

    private final RedeemOrderMapper redeemOrderMapper;

    private final ChildMapper childMapper;

    private final PointService pointService;

    // ---------------- 奖品 ----------------

    public Map<String, Object> listPrizes(int page, int size, String keyword, Boolean enabled) {
        IPage<Prize> result = prizeMapper.selectPage(new Page<>(page, size),
                Wrappers.<Prize>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(), Prize::getName, keyword)
                        .eq(enabled != null, Prize::getEnabled, enabled)
                        .orderByAsc(Prize::getSortOrder)
                        .orderByAsc(Prize::getId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("records", result.getRecords());
        return data;
    }

    @Transactional
    public Prize createPrize(AdminDtos.PrizeSaveReq req) {
        Prize prize = new Prize();
        applyPrize(prize, req);
        prizeMapper.insert(prize);
        return prize;
    }

    @Transactional
    public Prize updatePrize(Long id, AdminDtos.PrizeSaveReq req) {
        Prize prize = prizeMapper.selectById(id);
        if (prize == null) {
            throw BizException.notFound("奖品不存在");
        }
        applyPrize(prize, req);
        prizeMapper.updateById(prize);
        return prize;
    }

    @Transactional
    public void deletePrize(Long id) {
        if (prizeMapper.selectById(id) == null) {
            throw BizException.notFound("奖品不存在");
        }
        if (redeemOrderMapper.selectCount(Wrappers.<RedeemOrder>lambdaQuery()
                .eq(RedeemOrder::getPrizeId, id)) > 0) {
            throw BizException.conflict("该奖品已有兑换记录，无法删除，可改为下架");
        }
        prizeMapper.deleteById(id);
    }

    private void applyPrize(Prize prize, AdminDtos.PrizeSaveReq req) {
        prize.setName(req.name().trim());
        prize.setImage(req.image() == null ? "" : req.image().trim());
        prize.setPointsCost(Math.max(0, req.pointsCost()));
        prize.setStock(Math.max(0, req.stock()));
        if (req.enabled() != null) {
            prize.setEnabled(req.enabled());
        } else if (prize.getId() == null) {
            prize.setEnabled(true);
        }
        prize.setDescription(req.description() == null ? "" : req.description().trim());
        prize.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    }

    // ---------------- 兑换审核 ----------------

    public Map<String, Object> listRedeems(int page, int size, String status, Long childId) {
        IPage<RedeemOrder> result = redeemOrderMapper.selectPage(new Page<>(page, size),
                Wrappers.<RedeemOrder>lambdaQuery()
                        .eq(status != null && !status.isBlank(), RedeemOrder::getStatus, status)
                        .eq(childId != null, RedeemOrder::getChildId, childId)
                        .orderByDesc(RedeemOrder::getId));

        List<Long> childIds = result.getRecords().stream().map(RedeemOrder::getChildId).distinct().toList();
        Map<Long, Child> childMap = childIds.isEmpty() ? Map.of()
                : childMapper.selectBatchIds(childIds).stream()
                .collect(Collectors.toMap(Child::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> records = new ArrayList<>();
        for (RedeemOrder order : result.getRecords()) {
            Child child = childMap.get(order.getChildId());
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", order.getId());
            node.put("childId", order.getChildId());
            node.put("childNickname", child == null ? "（已删除）" : child.getNickname());
            node.put("prizeId", order.getPrizeId());
            node.put("prizeName", order.getPrizeName());
            node.put("pointsCost", order.getPointsCost());
            node.put("status", order.getStatus());
            node.put("remark", order.getRemark());
            node.put("createdAt", order.getCreatedAt());
            node.put("handledAt", order.getHandledAt());
            records.add(node);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("records", records);
        return data;
    }

    /** 审核兑换：通过 / 拒绝（退回积分与库存）/ 标记已发放 */
    @Transactional
    public Map<String, Object> handleRedeem(Long id, AdminDtos.RedeemHandleReq req) {
        RedeemOrder order = redeemOrderMapper.selectById(id);
        if (order == null) {
            throw BizException.notFound("兑换申请不存在");
        }
        String action = req.action().trim().toUpperCase();
        String remark = req.remark() == null ? "" : req.remark().trim();
        int balance = 0;

        switch (action) {
            case "APPROVE" -> {
                if (!STATUS_PENDING.equals(order.getStatus())) {
                    throw BizException.conflict("只有待审核的申请可以通过");
                }
                order.setStatus(STATUS_APPROVED);
            }
            case "REJECT" -> {
                if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_APPROVED.equals(order.getStatus())) {
                    throw BizException.conflict("该申请已处理，无法再次拒绝");
                }
                order.setStatus(STATUS_REJECTED);
                prizeMapper.restoreStock(order.getPrizeId());
                balance = pointService.add(order.getChildId(), order.getPointsCost(), 0,
                        PointService.BIZ_REJECT_REFUND, order.getId(),
                        "兑换被拒绝退回：" + order.getPrizeName());
            }
            case "DELIVER" -> {
                if (STATUS_REJECTED.equals(order.getStatus()) || STATUS_DELIVERED.equals(order.getStatus())) {
                    throw BizException.conflict("该申请当前状态不能标记为已发放");
                }
                order.setStatus(STATUS_DELIVERED);
            }
            default -> throw BizException.badRequest("不支持的操作类型");
        }

        order.setRemark(remark);
        order.setHandledAt(LocalDateTime.now());
        redeemOrderMapper.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.getId());
        result.put("status", order.getStatus());
        result.put("points", balance);
        return result;
    }

    /** 各状态兑换申请数量（仪表盘用） */
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("PENDING", redeemOrderMapper.selectCount(Wrappers.<RedeemOrder>lambdaQuery()
                .eq(RedeemOrder::getStatus, STATUS_PENDING)));
        counts.put("APPROVED", redeemOrderMapper.selectCount(Wrappers.<RedeemOrder>lambdaQuery()
                .eq(RedeemOrder::getStatus, STATUS_APPROVED)));
        counts.put("DELIVERED", redeemOrderMapper.selectCount(Wrappers.<RedeemOrder>lambdaQuery()
                .eq(RedeemOrder::getStatus, STATUS_DELIVERED)));
        counts.put("REJECTED", redeemOrderMapper.selectCount(Wrappers.<RedeemOrder>lambdaQuery()
                .eq(RedeemOrder::getStatus, STATUS_REJECTED)));
        return counts;
    }
}