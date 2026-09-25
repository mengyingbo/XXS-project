package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.common.BizException;
import com.xxs.game.entity.Child;
import com.xxs.game.entity.Prize;
import com.xxs.game.entity.RedeemOrder;
import com.xxs.game.mapper.PrizeMapper;
import com.xxs.game.mapper.RedeemOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 孩子端积分商城：奖品列表 + 兑换申请（需求文档 6.1 F-H5-07、F-H5-08）
 */
@Service
@RequiredArgsConstructor
public class ChildPrizeService {

    private final PrizeMapper prizeMapper;

    private final RedeemOrderMapper redeemOrderMapper;

    private final ChildAuthService childAuthService;

    private final PointService pointService;

    /** 上架中的奖品列表 */
    public Map<String, Object> list(Long childId) {
        Child child = childAuthService.require(childId);
        List<Prize> prizes = prizeMapper.selectList(Wrappers.<Prize>lambdaQuery()
                .eq(Prize::getEnabled, true)
                .orderByAsc(Prize::getSortOrder)
                .orderByAsc(Prize::getId));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Prize prize : prizes) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", prize.getId());
            node.put("name", prize.getName());
            node.put("image", prize.getImage());
            node.put("pointsCost", prize.getPointsCost());
            node.put("stock", prize.getStock());
            node.put("description", prize.getDescription());
            node.put("enoughPoints", child.getTotalPoints() != null && child.getTotalPoints() >= prize.getPointsCost());
            node.put("canRedeem", child.getTotalPoints() != null
                    && child.getTotalPoints() >= prize.getPointsCost()
                    && prize.getStock() != null && prize.getStock() > 0);
            items.add(node);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", child.getTotalPoints());
        result.put("prizes", items);
        return result;
    }

    /** 提交兑换申请：扣积分 + 扣库存 + 生成待审核订单 */
    @Transactional
    public Map<String, Object> redeem(Long childId, Long prizeId) {
        Child child = childAuthService.require(childId);
        Prize prize = prizeMapper.selectById(prizeId);
        if (prize == null || !Boolean.TRUE.equals(prize.getEnabled())) {
            throw BizException.notFound("奖品不存在或已下架");
        }
        if (prize.getStock() == null || prize.getStock() <= 0) {
            throw BizException.conflict("这个奖品已经被兑完啦，换一个试试吧");
        }
        if (child.getTotalPoints() == null || child.getTotalPoints() < prize.getPointsCost()) {
            throw BizException.conflict("积分还不够哦，再答几关吧");
        }

        RedeemOrder order = new RedeemOrder();
        order.setChildId(childId);
        order.setPrizeId(prize.getId());
        order.setPrizeName(prize.getName());
        order.setPointsCost(prize.getPointsCost());
        order.setStatus("PENDING");
        order.setRemark("");
        redeemOrderMapper.insert(order);

        if (prizeMapper.deductStock(prize.getId()) == 0) {
            throw BizException.conflict("这个奖品已经被兑完啦，换一个试试吧");
        }
        int balance = pointService.deduct(childId, prize.getPointsCost(), PointService.BIZ_REDEEM,
                order.getId(), "兑换奖品：" + prize.getName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("prizeName", prize.getName());
        result.put("pointsCost", prize.getPointsCost());
        result.put("status", order.getStatus());
        result.put("points", balance);
        return result;
    }
}