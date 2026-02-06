package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.NotificationMapper;
import com.zhangben.backend.mapper.PaymentNudgeMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.Notification;
import com.zhangben.backend.model.PaymentNudge;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.EmailService;
import com.zhangben.backend.service.EmailTemplateService;
import com.zhangben.backend.service.NudgeService;
import com.zhangben.backend.service.email.EmailProviderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * V41: 催促还账服务实现
 */
@Service
public class NudgeServiceImpl implements NudgeService {

    private static final Logger logger = LoggerFactory.getLogger(NudgeServiceImpl.class);

    // 催促频率限制：24小时
    private static final int NUDGE_COOLDOWN_HOURS = 24;

    // 通知类型常量
    private static final String NOTIFICATION_TYPE_NUDGE = "payment_nudge";

    // 邮件模板代码
    public static final String TEMPLATE_PAYMENT_NUDGE = "PAYMENT_NUDGE";

    @Autowired
    private PaymentNudgeMapper nudgeMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailTemplateService templateService;

    @Autowired
    private EmailProviderManager providerManager;

    @Value("${app.base-url:https://www.aabillpay.com}")
    private String baseUrl;

    @Override
    @Transactional
    public NudgeResult sendNudge(Integer creditorId, Integer debtorId, boolean anonymous) {
        logger.info("催促还账: creditor={}, debtor={}, anonymous={}", creditorId, debtorId, anonymous);

        // 1. 检查频率限制
        if (!canNudge(creditorId, debtorId)) {
            long nextTime = getNextNudgeTime(creditorId, debtorId);
            logger.info("催促频率限制: creditor={}, debtor={}, nextTime={}", creditorId, debtorId, nextTime);
            return NudgeResult.rateLimited(nextTime);
        }

        // 2. 获取用户信息
        User creditor = userMapper.selectByPrimaryKey(creditorId);
        User debtor = userMapper.selectByPrimaryKey(debtorId);

        if (creditor == null || debtor == null) {
            logger.error("用户不存在: creditor={}, debtor={}", creditorId, debtorId);
            return NudgeResult.error("USER_NOT_FOUND");
        }

        // 3. 检查债务人是否已注销
        if (debtor.getIsDeleted() != null && debtor.getIsDeleted() == 1) {
            logger.warn("债务人已注销: debtorId={}", debtorId);
            return NudgeResult.error("USER_DELETED");
        }

        // 4. 记录催促
        PaymentNudge nudge = new PaymentNudge();
        nudge.setCreditorId(creditorId);
        nudge.setDebtorId(debtorId);
        nudgeMapper.insert(nudge);

        // 5. 创建系统通知
        createNudgeNotification(creditor, debtor, anonymous);

        // 6. 发送邮件（异步，不阻塞）
        sendNudgeEmailAsync(creditor, debtor, anonymous);

        logger.info("催促成功: creditor={}, debtor={}, anonymous={}", creditorId, debtorId, anonymous);
        NudgeResult result = NudgeResult.success();
        result.setAnonymous(anonymous);
        return result;
    }

    @Override
    public boolean canNudge(Integer creditorId, Integer debtorId) {
        LocalDateTime since = LocalDateTime.now().minusHours(NUDGE_COOLDOWN_HOURS);
        int count = nudgeMapper.countRecentNudges(creditorId, debtorId, since);
        return count == 0;
    }

    @Override
    public long getNextNudgeTime(Integer creditorId, Integer debtorId) {
        PaymentNudge latestNudge = nudgeMapper.selectLatestNudge(creditorId, debtorId);
        if (latestNudge == null || latestNudge.getCreatedAt() == null) {
            return 0; // 可以立即催促
        }

        LocalDateTime nextAllowedTime = latestNudge.getCreatedAt().plusHours(NUDGE_COOLDOWN_HOURS);
        if (nextAllowedTime.isBefore(LocalDateTime.now())) {
            return 0; // 已过冷却期
        }

        return nextAllowedTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 创建系统通知
     */
    private void createNudgeNotification(User creditor, User debtor, boolean anonymous) {
        Notification notification = new Notification();
        notification.setUserId(debtor.getId());
        notification.setType(NOTIFICATION_TYPE_NUDGE);

        // 根据债务人语言设置通知内容
        String lang = debtor.getPreferredLanguage() != null ? debtor.getPreferredLanguage() : "zh-CN";
        String title;
        String content;
        if (anonymous) {
            title = getAnonymousNudgeTitle(lang);
            content = getAnonymousNudgeContent(lang);
        } else {
            title = getNudgeTitle(lang, creditor.getNickname());
            content = getNudgeContent(lang, creditor.getNickname());
        }

        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedType("debt");
        notification.setIsRead((byte) 0);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insertSelective(notification);
        logger.debug("创建催促通知: userId={}, title={}, anonymous={}", debtor.getId(), title, anonymous);
    }

    /**
     * 获取匿名催促通知标题（多语言）
     */
    private String getAnonymousNudgeTitle(String lang) {
        switch (lang) {
            case "en-US":
                return "Someone sent you a payment reminder";
            case "ja-JP":
                return "支払いリマインダーが届きました";
            default: // zh-CN
                return "有人提醒您结算欠款";
        }
    }

    /**
     * 获取匿名催促通知内容（多语言）
     */
    private String getAnonymousNudgeContent(String lang) {
        switch (lang) {
            case "en-US":
                return "Please check your debts and settle when convenient.";
            case "ja-JP":
                return "お手すきの際にご確認ください。";
            default: // zh-CN
                return "请在方便时查看并结算。";
        }
    }

    /**
     * 异步发送催促邮件
     */
    private void sendNudgeEmailAsync(User creditor, User debtor, boolean anonymous) {
        if (!providerManager.isMailEnabled()) {
            logger.debug("邮件服务未启用，跳过催促邮件");
            return;
        }

        if (debtor.getEmail() == null || debtor.getEmail().isEmpty()) {
            logger.warn("债务人邮箱为空: debtorId={}", debtor.getId());
            return;
        }

        String lang = debtor.getPreferredLanguage() != null ? debtor.getPreferredLanguage() : "zh-CN";

        String creditorName;
        if (anonymous) {
            switch (lang) {
                case "en-US": creditorName = "Someone"; break;
                case "ja-JP": creditorName = "どなたか"; break;
                default: creditorName = "有人"; break;
            }
        } else {
            creditorName = creditor.getNickname() != null ? creditor.getNickname() : "您的好友";
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("debtorName", debtor.getNickname() != null ? debtor.getNickname() : "用户");
        variables.put("creditorName", creditorName);
        variables.put("loginUrl", baseUrl + "/debts");
        variables.put("year", String.valueOf(Year.now().getValue()));

        // 渲染模板
        String htmlContent = templateService.renderTemplate(TEMPLATE_PAYMENT_NUDGE, lang, variables);
        String subject = templateService.renderSubject(TEMPLATE_PAYMENT_NUDGE, lang, variables);

        if (htmlContent == null) {
            // 模板不存在时使用内置模板
            htmlContent = buildFallbackNudgeEmail(creditor, debtor, lang, anonymous);
            subject = getNudgeEmailSubject(lang, creditorName);
        }

        // 异步发送
        providerManager.sendEmailFireAndForget(debtor.getEmail(), debtor.getNickname(), subject, htmlContent);
        logger.debug("发送催促邮件: to={}", debtor.getEmail());
    }

    /**
     * 获取催促通知标题（多语言）
     */
    private String getNudgeTitle(String lang, String creditorName) {
        switch (lang) {
            case "en-US":
                return creditorName + " sent you a payment reminder";
            case "ja-JP":
                return creditorName + "さんから支払いリマインダーが届きました";
            default: // zh-CN
                return creditorName + " 提醒您结算欠款";
        }
    }

    /**
     * 获取催促通知内容（多语言）
     */
    private String getNudgeContent(String lang, String creditorName) {
        switch (lang) {
            case "en-US":
                return "Please check your debts and settle when convenient.";
            case "ja-JP":
                return "お手すきの際にご確認ください。";
            default: // zh-CN
                return "请在方便时查看并结算。";
        }
    }

    /**
     * 获取催促邮件主题（多语言）
     */
    private String getNudgeEmailSubject(String lang, String creditorName) {
        switch (lang) {
            case "en-US":
                return "Payment Reminder from " + creditorName;
            case "ja-JP":
                return creditorName + "さんからの支払いリマインダー";
            default: // zh-CN
                return creditorName + " 发来还款提醒";
        }
    }

    /**
     * 构建内置催促邮件模板（当数据库模板不存在时使用）
     */
    private String buildFallbackNudgeEmail(User creditor, User debtor, String lang, boolean anonymous) {
        String debtorName = debtor.getNickname() != null ? debtor.getNickname() : "用户";
        String creditorName;
        if (anonymous) {
            switch (lang) {
                case "en-US": creditorName = "Someone"; break;
                case "ja-JP": creditorName = "どなたか"; break;
                default: creditorName = "有人"; break;
            }
        } else {
            creditorName = creditor.getNickname() != null ? creditor.getNickname() : "您的好友";
        }

        String greeting, body, cta, footer, privacy;

        switch (lang) {
            case "en-US":
                greeting = "Hi " + debtorName + ",";
                body = creditorName + " has sent you a gentle reminder about a pending payment.";
                cta = "View Details";
                footer = "Pay友 - Your AA Bill Splitting Companion";
                privacy = "Essential Cookie Only · No Tracking · Privacy First";
                break;
            case "ja-JP":
                greeting = debtorName + "さん、こんにちは";
                body = creditorName + "さんから支払いのリマインダーが届きました。";
                cta = "詳細を見る";
                footer = "Pay友 - あなたの割り勘パートナー";
                privacy = "必要最小限Cookie · 追跡なし · プライバシー優先";
                break;
            default: // zh-CN
                greeting = debtorName + "，您好";
                body = creditorName + " 提醒您有一笔账款待结算。";
                cta = "查看详情";
                footer = "Pay友 - 您的 AA 制记账好朋友";
                privacy = "仅必要 Cookie · 零追踪 · 隐私优先";
                break;
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'></head>" +
                "<body style='margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Helvetica,Arial,sans-serif;background-color:#f5f5f7;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:40px 20px;'>" +
                // Header
                "<div style='text-align:center;margin-bottom:32px;'>" +
                "<div style='font-size:32px;margin-bottom:8px;'>💰</div>" +
                "</div>" +
                // Card
                "<div style='background:#ffffff;border-radius:16px;padding:32px;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>" +
                "<p style='font-size:18px;color:#1d1d1f;margin:0 0 16px;'>" + greeting + "</p>" +
                "<p style='font-size:16px;color:#424245;line-height:1.6;margin:0 0 24px;'>" + body + "</p>" +
                "<div style='text-align:center;'>" +
                "<a href='" + baseUrl + "/debts' style='display:inline-block;background:#ff9500;color:#fff;text-decoration:none;padding:14px 32px;border-radius:24px;font-size:16px;font-weight:500;'>" + cta + "</a>" +
                "</div>" +
                "</div>" +
                // Footer
                "<div style='text-align:center;margin-top:32px;'>" +
                "<p style='font-size:14px;color:#86868b;margin:0 0 8px;'>" + footer + "</p>" +
                "<p style='font-size:12px;color:#86868b;margin:0;'>" + privacy + "</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
