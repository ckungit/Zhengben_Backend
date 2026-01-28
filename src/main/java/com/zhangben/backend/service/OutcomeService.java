package com.zhangben.backend.service;

import com.zhangben.backend.dto.OutcomeCreateRequest;
import com.zhangben.backend.dto.RecentOutcomeItem;

import java.util.List;

public interface OutcomeService {

    /**
     * 创建支出记录
     */
    void createOutcome(OutcomeCreateRequest req);

    /**
     * 获取用户最近的支出记录
     * @param userId 用户ID
     * @param limit 返回数量
     */
    List<RecentOutcomeItem> getRecentOutcomes(Integer userId, Integer limit);

    /**
     * 删除支出记录（软删除）
     * @param outcomeId 记录ID
     * @param userId 当前用户ID（用于权限校验）
     */
    void deleteOutcome(Integer outcomeId, Integer userId);
}
