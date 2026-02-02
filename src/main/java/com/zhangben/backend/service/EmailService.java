package com.zhangben.backend.service;

import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.service.email.EmailProviderManager;
import com.zhangben.backend.service.email.EmailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.*;

/**
 * V34: 邮件服务 - 重构为使用 EmailProviderManager
 *
 * 功能特性：
 * 1. 多供应商支持（Brevo、Resend、Mock）
 * 2. 自动降级机制
 * 3. 完全容错，不影响系统启动
 * 4. 异步发送，不阻塞业务流程
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // 模板代码常量
    public static final String TEMPLATE_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String TEMPLATE_WELCOME = "WELCOME";
    public static final String TEMPLATE_BILL_NOTIFICATION = "BILL_NOTIFICATION";

    // 默认语言
    public static final String DEFAULT_LANGUAGE = "zh-CN";

    @Autowired
    private EmailProviderManager providerManager;

    @Autowired
    private EmailTemplateService templateService;

    @Autowired
    private UserMapper userMapper;

    @Value("${brevo.sender-name:Pay友}")
    private String senderName;

    @Value("${app.base-url:https://www.aabillpay.com}")
    private String baseUrl;

    /**
     * 获取用户的语言偏好
     */
    private String getUserLanguagePreference(String email) {
        if (email == null) {
            return DEFAULT_LANGUAGE;
        }
        try {
            UserExample example = new UserExample();
            example.createCriteria().andEmailEqualTo(email);
            List<User> users = userMapper.selectByExample(example);
            if (!users.isEmpty() && users.get(0).getPreferredLanguage() != null) {
                return users.get(0).getPreferredLanguage();
            }
        } catch (Exception e) {
            logger.warn("获取用户语言偏好失败: {}", e.getMessage());
        }
        return DEFAULT_LANGUAGE;
    }

    /**
     * 发送密码重置邮件
     */
    public boolean sendPasswordResetEmail(String toEmail, String toName, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        String language = getUserLanguagePreference(toEmail);

        Map<String, Object> variables = new HashMap<>();
        variables.put("USER_NAME", toName != null ? toName : "用户");
        variables.put("RESET_LINK", resetLink);
        variables.put("EXPIRE_HOURS", "1");
        variables.put("APP_NAME", senderName);
        variables.put("YEAR", String.valueOf(Year.now().getValue()));

        return sendEmailWithTemplate(toEmail, toName, TEMPLATE_PASSWORD_RESET, language, variables);
    }

    /**
     * 发送欢迎邮件
     */
    public boolean sendWelcomeEmail(String toEmail, String toName) {
        String language = getUserLanguagePreference(toEmail);

        Map<String, Object> variables = new HashMap<>();
        variables.put("USER_NAME", toName != null ? toName : "新用户");
        variables.put("LOGIN_LINK", baseUrl + "/login");
        variables.put("APP_NAME", senderName);
        variables.put("YEAR", String.valueOf(Year.now().getValue()));

        return sendEmailWithTemplate(toEmail, toName, TEMPLATE_WELCOME, language, variables);
    }

    /**
     * 发送账单通知邮件（异步，失败不影响业务）
     */
    public void sendBillNotificationAsync(String toEmail, String toName, String language,
                                          String creatorName, Long amount, Long perAmount,
                                          String comment, String styleName, String activityName,
                                          boolean isUpdate) {
        // 检查邮件功能是否启用
        if (!providerManager.isMailEnabled()) {
            logger.debug("【邮件服务】mail.enabled=false，跳过账单通知: {}", toEmail);
            return;
        }

        // 构建变量
        String lang = language != null ? language : DEFAULT_LANGUAGE;
        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", toName != null ? toName : "用户");
        variables.put("creatorName", creatorName != null ? creatorName : "某用户");
        variables.put("amount", String.format("%.2f", amount / 100.0));
        variables.put("perAmount", String.format("%.2f", perAmount / 100.0));
        variables.put("styleName", styleName != null ? styleName : "未分类");
        variables.put("comment", comment);
        variables.put("activityName", activityName);
        variables.put("isUpdate", isUpdate);
        variables.put("loginUrl", baseUrl + "/login");
        variables.put("year", String.valueOf(Year.now().getValue()));

        // 渲染模板
        String htmlContent = templateService.renderTemplate(TEMPLATE_BILL_NOTIFICATION, lang, variables);
        String subject = templateService.renderSubject(TEMPLATE_BILL_NOTIFICATION, lang, variables);

        if (htmlContent == null) {
            logger.warn("账单通知邮件模板渲染失败: {}", toEmail);
            return;
        }

        // 异步发送（Fire-and-forget）
        providerManager.sendEmailFireAndForget(toEmail, toName, subject, htmlContent);
    }

    /**
     * 使用数据库模板发送邮件（支持多语言）
     */
    public boolean sendEmailWithTemplate(String toEmail, String toName, String templateCode,
                                         String language, Map<String, Object> variables) {
        // 使用 Thymeleaf 渲染模板
        String htmlContent = templateService.renderTemplate(templateCode, language, variables);
        String subject = templateService.renderSubject(templateCode, language, variables);

        if (htmlContent == null) {
            logger.error("邮件模板渲染失败: {}", templateCode);
            return false;
        }

        return sendEmail(toEmail, toName, subject, htmlContent);
    }

    /**
     * 使用数据库模板发送邮件（兼容旧接口）
     * @deprecated 推荐使用 sendEmailWithTemplate(String, String, String, String, Map)
     */
    @Deprecated
    public boolean sendEmailWithTemplate(String toEmail, String toName, String templateCode,
                                         Map<String, String> variables) {
        Map<String, Object> objectVariables = new HashMap<>();
        if (variables != null) {
            objectVariables.putAll(variables);
        }
        return sendEmailWithTemplate(toEmail, toName, templateCode, DEFAULT_LANGUAGE, objectVariables);
    }

    /**
     * 发送邮件（核心方法）
     * 使用 EmailProviderManager 实现自动降级
     */
    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            EmailResult result = providerManager.sendEmail(toEmail, toName, subject, htmlContent);
            return result.isSuccess();
        } catch (Exception e) {
            // 捕获所有异常，确保不影响业务
            logger.error("【邮件服务】发送异常 [{}]: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * 检查邮件服务是否可用
     */
    public boolean isAvailable() {
        return providerManager.isMailEnabled() &&
               providerManager.getAvailableProvider().isAvailable();
    }

    /**
     * 检查邮件功能是否启用
     */
    public boolean isEnabled() {
        return providerManager.isMailEnabled();
    }

    /**
     * 获取当前使用的供应商名称
     */
    public String getCurrentProvider() {
        return providerManager.getAvailableProvider().getProviderName();
    }

    /**
     * 获取所有供应商的状态
     */
    public Map<String, Boolean> getProviderStatus() {
        return providerManager.getProviderStatus();
    }
}
