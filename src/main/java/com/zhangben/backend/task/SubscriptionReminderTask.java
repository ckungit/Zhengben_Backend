package com.zhangben.backend.task;

import com.zhangben.backend.model.User;
import com.zhangben.backend.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * V42: 订阅续费提醒定时任务
 * 每天检查 90 天内到期的订阅，发送提醒
 */
@Component
public class SubscriptionReminderTask {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionReminderTask.class);

    @Autowired
    private SubscriptionService subscriptionService;

    /**
     * 每天早上 9:00 执行（东京时间）
     * 检查 90 天内到期的订阅并发送提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendRenewalReminders() {
        logger.info("Starting subscription renewal reminder task...");

        try {
            // 查找 90 天内到期的用户
            List<User> expiringUsers = subscriptionService.findExpiringSubscriptions(90);

            logger.info("Found {} users with expiring subscriptions", expiringUsers.size());

            for (User user : expiringUsers) {
                try {
                    // 计算距到期天数
                    int daysUntilExpiry = (int) ChronoUnit.DAYS.between(
                        LocalDateTime.now(),
                        user.getSubscriptionExpiryDate()
                    );

                    // 发送提醒
                    subscriptionService.sendRenewalReminder(user, daysUntilExpiry);

                    // 标记已发送
                    subscriptionService.markReminderSent(user.getId());

                    logger.info("Sent renewal reminder to user {} (expires in {} days)",
                        user.getId(), daysUntilExpiry);

                } catch (Exception e) {
                    logger.error("Failed to send reminder to user {}: {}",
                        user.getId(), e.getMessage());
                }
            }

            logger.info("Subscription renewal reminder task completed");

        } catch (Exception e) {
            logger.error("Subscription renewal reminder task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 每月 1 日凌晨 3:00 重置提醒状态
     * 允许下个月再次发送提醒
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void resetReminderStatus() {
        logger.info("Resetting subscription reminder status...");
        try {
            subscriptionService.resetReminderStatus();
            logger.info("Subscription reminder status reset completed");
        } catch (Exception e) {
            logger.error("Failed to reset reminder status: {}", e.getMessage(), e);
        }
    }
}
