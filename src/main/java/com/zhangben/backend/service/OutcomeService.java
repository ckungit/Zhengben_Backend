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

    /**
     * 获取单个支出记录详情
     * @param outcomeId 记录ID
     * @param userId 当前用户ID（用于权限校验）
     */
    RecentOutcomeItem getOutcomeById(Integer outcomeId, Integer userId);

    /**
     * 更新支出记录
     * @param outcomeId 记录ID
     * @param userId 当前用户ID（用于权限校验）
     * @param req 更新请求
     */
    void updateOutcome(Integer outcomeId, Integer userId, OutcomeCreateRequest req);

    /**
     * 搜索历史账单
     * @param userId 用户ID
     * @param keyword 搜索关键词（搜索备注）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param styleId 分类ID（可选）
     * @param limit 返回数量
     * @param offset 偏移量
     */
    List<RecentOutcomeItem> searchOutcomes(Integer userId, String keyword,
            java.time.LocalDate startDate, java.time.LocalDate endDate,
            Integer styleId, Integer limit, Integer offset);

    /**
     * 获取搜索结果总数
     */
    Integer countSearchOutcomes(Integer userId, String keyword,
            java.time.LocalDate startDate, java.time.LocalDate endDate, Integer styleId);

    /**
     * Seek-based 分页获取用户历史记录
     * @param userId 用户ID
     * @param lastId 上一页最后一条记录的ID（首页传 null）
     * @param limit 每页数量
     * @param month 月份筛选 yyyy-MM（可选）
     * @return 包含 list, hasMore, monthlyTotal 的结果 Map
     */
    java.util.Map<String, Object> getPagedHistory(Integer userId, Integer lastId, Integer limit, String month);

    /**
     * Seek-based 分页获取用户历史记录（支持按天筛选）
     */
    java.util.Map<String, Object> getPagedHistory(Integer userId, Integer lastId, Integer limit, String month, String day);

    /**
     * 获取用户有记录的月份列表
     */
    java.util.List<String> getAvailableMonths(Integer userId);

    /**
     * 获取用户某月有记录的日期列表
     */
    java.util.List<Integer> getActiveDays(Integer userId, String month);
}
