package com.zhangben.backend.service.impl;

import com.zhangben.backend.dto.SubscriptionInfoResponse;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.NotificationService;
import com.zhangben.backend.service.SubscriptionService;
import com.zhangben.backend.service.email.EmailProviderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * V42: 订阅服务实现
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private static final int RENEWAL_WINDOW_DAYS = 90;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailProviderManager emailProviderManager;

    @Override
    public SubscriptionInfoResponse getSubscriptionInfo(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        SubscriptionInfoResponse response = new SubscriptionInfoResponse();
        response.setTier(user.getSubscriptionTier() != null ? user.getSubscriptionTier() : "FREE");
        response.setType(user.getSubscriptionType());
        response.setStartDate(user.getSubscriptionStartDate());
        response.setExpiryDate(user.getSubscriptionExpiryDate());
        response.setAutoRenew(user.getSubscriptionAutoRenew() != null && user.getSubscriptionAutoRenew() == 1);
        response.setPermanent("PERMANENT".equals(user.getSubscriptionType()));

        // 计算到期天数和续费窗口
        if (user.getSubscriptionExpiryDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), user.getSubscriptionExpiryDate());
            response.setDaysUntilExpiry((int) daysUntil);
            response.setInRenewalWindow(daysUntil <= RENEWAL_WINDOW_DAYS && daysUntil > 0);
        } else {
            response.setDaysUntilExpiry(null);
            response.setInRenewalWindow(false);
        }

        // 设置显示名称
        response.setTierDisplayName(getTierDisplayName(response.getTier()));
        response.setTypeDisplayName(getTypeDisplayName(response.getType()));

        return response;
    }

    @Override
    public void promoteToPermenentPro(Integer userId, Integer adminId) {
        updateSubscription(userId, "PRO", "PERMANENT", null, adminId);
    }

    @Override
    public void updateSubscription(Integer userId, String tier, String type, Integer durationDays, Integer adminId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        String previousTier = user.getSubscriptionTier();
        String previousType = user.getSubscriptionType();

        user.setSubscriptionTier(tier);
        user.setSubscriptionType(type);
        user.setSubscriptionStartDate(LocalDateTime.now());

        if ("PERMANENT".equals(type)) {
            user.setSubscriptionExpiryDate(null);
            user.setSubscriptionAutoRenew((byte) 0);
        } else if (durationDays != null && durationDays > 0) {
            user.setSubscriptionExpiryDate(LocalDateTime.now().plusDays(durationDays));
        }

        user.setSubscriptionReminderSent((byte) 0);
        userMapper.updateByPrimaryKeySelective(user);

        // 记录变更历史
        logSubscriptionChange(userId, previousTier, tier, previousType, type, "ADMIN_PROMOTE", adminId);

        logger.info("Admin {} promoted user {} to {} {}", adminId, userId, tier, type);
    }

    @Override
    public List<User> findExpiringSubscriptions(int daysBeforeExpiry) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryThreshold = now.plusDays(daysBeforeExpiry);
        return userMapper.selectExpiringSubscriptions(now, expiryThreshold);
    }

    @Override
    public void sendRenewalReminder(User user, int daysBeforeExpiry) {
        // 发送站内通知
        String title = String.format("您的 %s 会员即将到期", getTierDisplayName(user.getSubscriptionTier()));
        String content = String.format("您的会员将在 %d 天后到期，请及时续费以继续享受服务。", daysBeforeExpiry);

        notificationService.createNotification(
            user.getId(),
            "SUBSCRIPTION_EXPIRY",
            title,
            content,
            null,
            null
        );

        // 异步发送邮件
        sendRenewalEmailAsync(user, daysBeforeExpiry);

        logger.info("Sent renewal reminder to user {} ({} days before expiry)", user.getId(), daysBeforeExpiry);
    }

    private void sendRenewalEmailAsync(User user, int daysBeforeExpiry) {
        CompletableFuture.runAsync(() -> {
            try {
                String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "zh-CN";
                String subject = getEmailSubject(language, daysBeforeExpiry);
                String htmlContent = buildRenewalEmailHtml(user, daysBeforeExpiry, language);

                emailProviderManager.sendEmail(
                    user.getEmail(),
                    user.getNickname(),
                    subject,
                    htmlContent
                );
            } catch (Exception e) {
                logger.error("Failed to send renewal email to user {}: {}", user.getId(), e.getMessage());
            }
        });
    }

    private String getEmailSubject(String language, int daysBeforeExpiry) {
        return switch (language) {
            case "en-US" -> String.format("Your membership expires in %d days", daysBeforeExpiry);
            case "ja-JP" -> String.format("会員資格が%d日後に期限切れになります", daysBeforeExpiry);
            default -> String.format("您的会员将在 %d 天后到期", daysBeforeExpiry);
        };
    }

    private String buildRenewalEmailHtml(User user, int daysBeforeExpiry, String language) {
        boolean isAutoRenew = user.getSubscriptionAutoRenew() != null && user.getSubscriptionAutoRenew() == 1;

        String actionText = switch (language) {
            case "en-US" -> isAutoRenew ? "Your subscription will renew automatically." : "Please renew manually to continue your membership.";
            case "ja-JP" -> isAutoRenew ? "サブスクリプションは自動的に更新されます。" : "会員資格を継続するには、手動で更新してください。";
            default -> isAutoRenew ? "您的订阅将自动续费。" : "请手动续费以继续享受会员服务。";
        };

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background-color:#f5f5f7;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;margin-top:20px;margin-bottom:20px;">
                    <tr>
                        <td style="padding:40px 30px;text-align:center;">
                            <!-- Golden Badge -->
                            <div style="display:inline-block;background:linear-gradient(135deg,#F5D061 0%%,#E6AF2E 50%%,#CF9E1D 100%%);padding:12px 24px;border-radius:20px;margin-bottom:24px;">
                                <span style="color:#ffffff;font-weight:600;font-size:14px;text-shadow:0 1px 2px rgba(0,0,0,0.2);">%s Member</span>
                            </div>

                            <h1 style="color:#1d1d1f;font-size:24px;font-weight:600;margin:0 0 16px;">
                                %s
                            </h1>

                            <p style="color:#86868b;font-size:16px;line-height:1.5;margin:0 0 24px;">
                                %s
                            </p>

                            <a href="https://aabillpay.com/subscription" style="display:inline-block;background:linear-gradient(135deg,#F5D061 0%%,#E6AF2E 50%%,#CF9E1D 100%%);color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:12px;font-weight:600;font-size:16px;text-shadow:0 1px 2px rgba(0,0,0,0.2);">
                                %s
                            </a>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:20px 30px;background:#f5f5f7;text-align:center;">
                            <p style="color:#86868b;font-size:12px;margin:0;">
                                Pay友 (AA Bill Pay) · Essential Cookie Only, Privacy First
                            </p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
            user.getSubscriptionTier(),
            getEmailSubject(language, daysBeforeExpiry),
            actionText,
            language.equals("en-US") ? "Renew Now" : (language.equals("ja-JP") ? "今すぐ更新" : "立即续费")
        );
    }

    @Override
    public void markReminderSent(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null) {
            user.setSubscriptionReminderSent((byte) 1);
            userMapper.updateByPrimaryKeySelective(user);
        }
    }

    @Override
    public boolean isInRenewalWindow(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null || user.getSubscriptionExpiryDate() == null) {
            return false;
        }
        long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), user.getSubscriptionExpiryDate());
        return daysUntil <= RENEWAL_WINDOW_DAYS && daysUntil > 0;
    }

    @Override
    public void resetReminderStatus() {
        userMapper.resetSubscriptionReminderStatus();
        logger.info("Reset subscription reminder status for all users");
    }

    private void logSubscriptionChange(Integer userId, String prevTier, String newTier,
                                       String prevType, String newType, String reason, Integer changedBy) {
        // 可以在这里插入 subscription_history 表记录
        // 简化实现，只记录日志
        logger.info("Subscription change: userId={}, tier: {} -> {}, type: {} -> {}, reason={}, by={}",
            userId, prevTier, newTier, prevType, newType, reason, changedBy);
    }

    private String getTierDisplayName(String tier) {
        if (tier == null) return "免费用户";
        return switch (tier) {
            case "PRO" -> "专业会员";
            case "NORMAL" -> "普通会员";
            default -> "免费用户";
        };
    }

    private String getTypeDisplayName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "PERMANENT" -> "永久";
            case "YEARLY" -> "年付";
            case "MONTHLY" -> "月付";
            default -> type;
        };
    }
}
