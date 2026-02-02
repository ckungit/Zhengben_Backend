package com.zhangben.backend.service;

import com.zhangben.backend.model.Notification;

import java.util.List;

public interface NotificationService {

    /**
     * 创建通知
     */
    void createNotification(Integer userId, String type, String title, String content, Long relatedId, String relatedType);

    /**
     * 获取用户的通知列表
     */
    List<Notification> getNotifications(Integer userId, Integer limit);

    /**
     * 获取用户未读通知数量
     */
    int getUnreadCount(Integer userId);

    /**
     * 标记单个通知为已读
     */
    void markAsRead(Long notificationId, Integer userId);

    /**
     * 标记所有通知为已读
     */
    void markAllAsRead(Integer userId);
}
