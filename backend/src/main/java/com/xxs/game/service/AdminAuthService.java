package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.common.BizException;
import com.xxs.game.dto.AdminDtos;
import com.xxs.game.entity.AdminUser;
import com.xxs.game.mapper.AdminUserMapper;
import com.xxs.game.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端登录与改密（需求文档 6.2 F-AD-01）
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    public Map<String, Object> login(AdminDtos.LoginReq req) {
        AdminUser admin = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, req.username().trim()));
        if (admin == null || !passwordEncoder.matches(req.password(), admin.getPasswordHash())) {
            throw BizException.unauthorized("账号或密码错误");
        }

        AdminUser update = new AdminUser();
        update.setId(admin.getId());
        update.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(update);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", admin.getId());
        info.put("username", admin.getUsername());
        info.put("nickname", admin.getNickname());
        info.put("mustChangePwd", admin.getMustChangePwd());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.create(admin.getId(), JwtUtil.AUD_ADMIN));
        result.put("expiresIn", jwtUtil.getExpireSeconds());
        result.put("admin", info);
        return result;
    }

    @Transactional
    public void changePassword(Long adminId, AdminDtos.ChangePwdReq req) {
        AdminUser admin = require(adminId);
        if (!passwordEncoder.matches(req.oldPassword(), admin.getPasswordHash())) {
            throw BizException.badRequest("原密码不正确");
        }
        if (req.oldPassword().equals(req.newPassword())) {
            throw BizException.badRequest("新密码不能与原密码相同");
        }
        AdminUser update = new AdminUser();
        update.setId(adminId);
        update.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        update.setMustChangePwd(false);
        adminUserMapper.updateById(update);
    }

    public AdminUser require(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw BizException.unauthorized("管理员不存在");
        }
        return admin;
    }
}