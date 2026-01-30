package com.zhangben.backend.service;

import com.zhangben.backend.mapper.EmailTemplateMapper;
import com.zhangben.backend.model.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import jakarta.annotation.PostConstruct;
import java.time.Year;
import java.util.*;

/**
 * 邮件服务 - 使用 Brevo SDK 发送邮件
 * 模板使用 EmailTemplateService 渲染
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
    private EmailTemplateMapper emailTemplateMapper;

    @Autowired
    private EmailTemplateService templateService;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:noreply@aabillpay.com}")
    private String senderEmail;

    @Value("${brevo.sender-name:Pay友}")
    private String senderName;

    @Value("${app.base-url:https://www.aabillpay.com}")
    private String baseUrl;

    private TransactionalEmailsApi emailApi;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (brevoApiKey != null && !brevoApiKey.isEmpty() && !"no_use_email".equals(brevoApiKey)) {
            try {
                ApiClient defaultClient = Configuration.getDefaultApiClient();
                ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
                apiKey.setApiKey(brevoApiKey);
                emailApi = new TransactionalEmailsApi();
                initialized = true;
                logger.info("Brevo 邮件服务初始化成功");
            } catch (Exception e) {
                logger.error("Brevo 邮件服务初始化失败: {}", e.getMessage());
            }
        } else {
            logger.warn("Brevo API Key 未配置，邮件服务不可用");
        }
    }

    /**
     * 发送密码重置邮件
     */
    public boolean sendPasswordResetEmail(String toEmail, String toName, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        Map<String, Object> variables = new HashMap<>();
        variables.put("USER_NAME", toName != null ? toName : "用户");
        variables.put("RESET_LINK", resetLink);
        variables.put("EXPIRE_HOURS", "1");
        variables.put("APP_NAME", senderName);
        variables.put("YEAR", String.valueOf(Year.now().getValue()));

        return sendEmailWithTemplate(toEmail, toName, TEMPLATE_PASSWORD_RESET, DEFAULT_LANGUAGE, variables);
    }

    /**
     * 发送欢迎邮件
     */
    public boolean sendWelcomeEmail(String toEmail, String toName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("USER_NAME", toName != null ? toName : "新用户");
        variables.put("LOGIN_LINK", baseUrl + "/login");
        variables.put("APP_NAME", senderName);
        variables.put("YEAR", String.valueOf(Year.now().getValue()));

        return sendEmailWithTemplate(toEmail, toName, TEMPLATE_WELCOME, DEFAULT_LANGUAGE, variables);
    }

    /**
     * 发送账单通知邮件（异步，失败不影响业务）
     * @param toEmail 收件人邮箱
     * @param toName 收件人昵称
     * @param language 用户首选语言
     * @param creatorName 账单创建者昵称
     * @param amount 金额（分）
     * @param perAmount 人均金额（分）
     * @param comment 备注
     * @param styleName 分类名称
     * @param activityName 活动名称（可选）
     * @param isUpdate 是否是更新通知
     */
    public void sendBillNotificationAsync(String toEmail, String toName, String language,
                                          String creatorName, Long amount, Long perAmount,
                                          String comment, String styleName, String activityName,
                                          boolean isUpdate) {
        // 异步发送，失败不影响业务
        new Thread(() -> {
            try {
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

                boolean success = sendEmailWithTemplate(toEmail, toName, TEMPLATE_BILL_NOTIFICATION, lang, variables);
                if (!success) {
                    logger.warn("账单通知邮件发送失败: {}", toEmail);
                }
            } catch (Exception e) {
                logger.error("发送账单通知邮件异常 [{}]: {}", toEmail, e.getMessage());
            }
        }).start();
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
     * 使用数据库模板发送邮件（兼容旧接口，使用 String 变量）
     * @deprecated 推荐使用 sendEmailWithTemplate(String, String, String, String, Map<String, Object>)
     */
    @Deprecated
    public boolean sendEmailWithTemplate(String toEmail, String toName, String templateCode, Map<String, String> variables) {
        // 转换为 Object 类型的 Map
        Map<String, Object> objectVariables = new HashMap<>();
        if (variables != null) {
            objectVariables.putAll(variables);
        }
        return sendEmailWithTemplate(toEmail, toName, templateCode, DEFAULT_LANGUAGE, objectVariables);
    }

    /**
     * 发送邮件（核心方法）
     */
    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (!initialized) {
            logger.warn("邮件服务未初始化，跳过发送邮件到: {}", toEmail);
            return false;
        }

        try {
            SendSmtpEmail email = new SendSmtpEmail();

            // 发件人
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName(senderName);
            sender.setEmail(senderEmail);
            email.setSender(sender);

            // 收件人
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(toEmail);
            if (toName != null && !toName.isEmpty()) {
                recipient.setName(toName);
            }
            email.setTo(Collections.singletonList(recipient));

            // 主题和内容
            email.setSubject(subject);
            email.setHtmlContent(htmlContent);

            // 发送
            emailApi.sendTransacEmail(email);
            logger.info("邮件发送成功: {} -> {}", subject, toEmail);
            return true;

        } catch (Exception e) {
            logger.error("邮件发送失败 [{}]: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * 检查邮件服务是否可用
     */
    public boolean isAvailable() {
        return initialized;
    }
}
