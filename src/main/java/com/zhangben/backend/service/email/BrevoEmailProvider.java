package com.zhangben.backend.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.CreateSmtpEmail;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

/**
 * V34: Brevo (原 Sendinblue) 邮件提供商
 * 封装 Brevo SDK 的发信逻辑
 */
@Component
public class BrevoEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailProvider.class);

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:noreply@aabillpay.com}")
    private String senderEmail;

    @Value("${brevo.sender-name:Pay友}")
    private String senderName;

    private TransactionalEmailsApi emailApi;
    private boolean initialized = false;
    private String initError = null;

    @PostConstruct
    public void init() {
        // 检查 API Key 是否配置
        if (brevoApiKey == null || brevoApiKey.isEmpty() || "no_use_email".equals(brevoApiKey)) {
            initError = "Brevo API Key 未配置";
            logger.warn("【Brevo】{}", initError);
            return;
        }

        // 尝试初始化 Brevo SDK - 完全容错
        try {
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
            apiKey.setApiKey(brevoApiKey);
            emailApi = new TransactionalEmailsApi();
            initialized = true;
            logger.info("【Brevo】初始化成功");
        } catch (Exception e) {
            initError = e.getMessage();
            logger.warn("【Brevo】初始化失败（账号可能被封禁）: {}", initError);
            initialized = false;
        }
    }

    @Override
    public String getProviderName() {
        return "brevo";
    }

    @Override
    public boolean isAvailable() {
        return initialized;
    }

    @Override
    public EmailResult sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        return sendEmail(senderEmail, senderName, toEmail, toName, subject, htmlContent);
    }

    @Override
    public EmailResult sendEmail(String fromEmail, String fromName,
                                  String toEmail, String toName,
                                  String subject, String htmlContent) {
        if (!initialized) {
            return EmailResult.failure("brevo", "Brevo 未初始化: " + initError);
        }

        try {
            SendSmtpEmail email = new SendSmtpEmail();

            // 发件人
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName(fromName != null ? fromName : senderName);
            sender.setEmail(fromEmail != null ? fromEmail : senderEmail);
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
            CreateSmtpEmail response = emailApi.sendTransacEmail(email);
            String messageId = response != null ? response.getMessageId() : null;

            logger.info("【Brevo】发送成功: {} -> {}", subject, toEmail);
            return EmailResult.success("brevo", messageId);

        } catch (Exception e) {
            logger.error("【Brevo】发送失败 [{}]: {}", toEmail, e.getMessage());
            return EmailResult.failure("brevo", e.getMessage());
        }
    }
}
