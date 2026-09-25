package com.xxs.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxs.game.entity.Progress;
import com.xxs.game.mapper.ProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端重置闯关进度（需求文档 6.2 F-AD-06）
 * 只清理 progress（解锁与星级），保留 game_session / answer_record 历史，统计不受影响
 */
@Service
@RequiredArgsConstructor
public class AdminProgressService {

    private final ProgressMapper progressMapper;

    private final AdminChildService adminChildService;

    @Transactional
    public Map<String, Object> resetLevel(Long childId, Long levelId) {
        adminChildService.get(childId);
        int rows = progressMapper.delete(Wrappers.<Progress>lambdaQuery()
                .eq(Progress::getChildId, childId)
                .eq(Progress::getLevelId, levelId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("childId", childId);
        result.put("levelId", levelId);
        result.put("reset", rows);
        return result;
    }

    @Transactional
    public Map<String, Object> resetAll(Long childId) {
        adminChildService.get(childId);
        int rows = progressMapper.delete(Wrappers.<Progress>lambdaQuery()
                .eq(Progress::getChildId, childId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("childId", childId);
        result.put("reset", rows);
        return result;
    }
}