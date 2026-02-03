package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.model.Notification;
import com.zhangben.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping("")
    public List<Notification> getNotifications(@RequestParam(required = false, defaultValue = "50") Integer limit) {
        Integer userId = StpUtil.getLoginIdAsInt();
        return notificationService.getNotifications(userId, limit);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Map<String, Integer> getUnreadCount() {
        Integer userId = StpUtil.getLoginIdAsInt();
        int count = notificationService.getUnreadCount(userId);
        Map<String, Integer> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    /**
     * 标记单个通知为已读
     */
    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable("id") Long notificationId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        notificationService.markAsRead(notificationId, userId);
        return "标记成功";
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public String markAllAsRead() {
        Integer userId = StpUtil.getLoginIdAsInt();
        notificationService.markAllAsRead(userId);
        return "全部标记为已读";
    }

    /**
     * V41: 删除所有通知（清除全部）
     */
    @DeleteMapping("/clear-all")
    public String clearAllNotifications() {
        Integer userId = StpUtil.getLoginIdAsInt();
        notificationService.deleteAllNotifications(userId);
        return "已清除全部通知";
    }
}
