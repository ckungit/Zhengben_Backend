package com.zhangben.backend.service;

import com.zhangben.backend.dto.SubscriptionInfoResponse;
import com.zhangben.backend.model.User;

import java.util.List;

/**
 * V42: 订阅服务接口
 */
public interface SubscriptionService {

    /**
     * 获取用户订阅信息
     */
    SubscriptionInfoResponse getSubscriptionInfo(Integer userId);

    /**
     * Admin: 将用户提升为永久 PRO 会员
     */
    void promoteToPermenentPro(Integer userId, Integer adminId);

    /**
     * Admin: 修改用户订阅等级
     */
    void updateSubscription(Integer userId, String tier, String type, Integer durationDays, Integer adminId);

    /**
     * 查找 90 天内到期的订阅
     */
    List<User> findExpiringSubscriptions(int daysBeforeExpiry);

    /**
     * 发送续费提醒
     */
    void sendRenewalReminder(User user, int daysBeforeExpiry);

    /**
     * 标记提醒已发送
     */
    void markReminderSent(Integer userId);

    /**
     * 检查是否在续费窗口内（到期前 90 天）
     */
    boolean isInRenewalWindow(Integer userId);

    /**
     * 重置提醒状态（每月初）
     */
    void resetReminderStatus();
}
