package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.NotificationMapper;
import com.zhangben.backend.model.Notification;
import com.zhangben.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public void createNotification(Integer userId, String type, String title, String content, Long relatedId, String relatedType) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead((byte) 0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insertSelective(notification);
    }

    @Override
    public List<Notification> getNotifications(Integer userId, Integer limit) {
        return notificationMapper.selectByUserId(userId, limit);
    }

    @Override
    public int getUnreadCount(Integer userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Integer userId) {
        // 先验证通知属于该用户
        Notification notification = notificationMapper.selectByPrimaryKey(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("通知不存在或无权操作");
        }
        notificationMapper.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(Integer userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Override
    public void deleteAllNotifications(Integer userId) {
        notificationMapper.deleteByUserId(userId);
    }
}
