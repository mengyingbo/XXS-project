package com.xxs.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.Child;
import com.xxs.game.mapper.ChildMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端孩子档案管理（需求文档 6.2 F-AD-03）
 */
@Service
@RequiredArgsConstructor
public class AdminChildService {

    private final ChildMapper childMapper;

    private final PasswordEncoder passwordEncoder;

    private final PointService pointService;

    public Map<String, Object> list(int page, int size, String keyword) {
        Page<Child> pageParam = new Page<>(page, size);
        IPage<Child> result = childMapper.selectPage(pageParam, Wrappers.<Child>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), Child::getNickname, keyword)
                .orderByAsc(Child::getId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        data.put("records", result.getRecords());
        return data;
    }

    public Child get(Long id) {
        Child child = childMapper.selectById(id);
        if (child == null) {
            throw BizException.notFound("孩子档案不存在");
        }
        return child;
    }

    @Transactional
    public Child create(AdminDtos.ChildSaveReq req) {
        if (req.pin() == null || req.pin().isBlank()) {
            throw BizException.badRequest("新建档案必须设置 4 位 PIN");
        }
        Child child = new Child();
        child.setNickname(req.nickname().trim());
        child.setAvatar(req.avatar() == null ? "" : req.avatar().trim());
        child.setPinHash(passwordEncoder.encode(req.pin()));
        child.setTotalPoints(0);
        child.setTotalEarned(0);
        child.setPinFailCount(0);
        child.setEnabled(req.enabled() == null || req.enabled());
        childMapper.insert(child);
        return child;
    }

    @Transactional
    public Child update(Long id, AdminDtos.ChildSaveReq req) {
        Child child = get(id);
        child.setNickname(req.nickname().trim());
        child.setAvatar(req.avatar() == null ? "" : req.avatar().trim());
        if (req.enabled() != null) {
            child.setEnabled(req.enabled());
        }
        childMapper.updateById(child);
        return child;
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        try {
            childMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw BizException.conflict("该孩子已有答题或兑换记录，无法删除，可改为「停用」");
        }
    }

    @Transactional
    public void resetPin(Long id, String pin) {
        get(id);
        Child update = new Child();
        update.setId(id);
        update.setPinHash(passwordEncoder.encode(pin));
        update.setPinFailCount(0);
        childMapper.updateById(update);
        // 重置 PIN 时同时解除锁定
        childMapper.update(null, Wrappers.<Child>lambdaUpdate()
                .eq(Child::getId, id)
                .set(Child::getLockedUntil, null));
    }

    /** 手动调分：写积分流水，备注必填提示 */
    @Transactional
    public Map<String, Object> adjustPoints(Long id, AdminDtos.AdjustPointsReq req) {
        Child child = get(id);
        int delta = req.delta();
        if (delta == 0) {
            throw BizException.badRequest("积分变动值不能为 0");
        }
        int balance = child.getTotalPoints() == null ? 0 : child.getTotalPoints();
        if (balance + delta < 0) {
            throw BizException.badRequest("调整后积分不能为负数（当前 " + balance + " 分）");
        }
        String remark = req.remark() == null || req.remark().isBlank() ? "家长手动调整" : req.remark().trim();
        // 手动调分不计入「累计获得积分」
        int after = pointService.add(id, delta, 0, PointService.BIZ_ADMIN_ADJUST, null, remark);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("childId", id);
        result.put("delta", delta);
        result.put("points", after);
        return result;
    }
}