package com.zhangben.backend.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V34: Resend 邮件提供商
 * 基于 Resend API (https://resend.com) 实现
 * 使用 REST API 调用，无需额外 SDK 依赖
 */
@Component
public class ResendEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailProvider.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.sender-email:noreply@aabillpay.com}")
    private String senderEmail;

    @Value("${resend.sender-name:Pay友}")
    private String senderName;

    private RestTemplate restTemplate;
    private boolean initialized = false;
    private String initError = null;

    @PostConstruct
    public void init() {
        // 检查 API Key 是否配置
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            initError = "Resend API Key 未配置";
            logger.info("【Resend】{}", initError);
            return;
        }

        // Resend 不需要预初始化，只要有 API Key 就可以尝试发送
        try {
            restTemplate = new RestTemplate();
            initialized = true;
            logger.info("【Resend】初始化成功");
        } catch (Exception e) {
            initError = e.getMessage();
            logger.warn("【Resend】初始化失败: {}", initError);
            initialized = false;
        }
    }

    @Override
    public String getProviderName() {
        return "resend";
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
            return EmailResult.failure("resend", "Resend 未初始化: " + initError);
        }

        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();

            // 发件人格式: "Name <email@example.com>"
            String from = fromName != null && !fromName.isEmpty()
                    ? fromName + " <" + (fromEmail != null ? fromEmail : senderEmail) + ">"
                    : (fromEmail != null ? fromEmail : senderEmail);
            requestBody.put("from", from);

            // 收件人格式: "Name <email@example.com>" 或 "email@example.com"
            String to = toName != null && !toName.isEmpty()
                    ? toName + " <" + toEmail + ">"
                    : toEmail;
            requestBody.put("to", List.of(to));

            requestBody.put("subject", subject);
            requestBody.put("html", htmlContent);

            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(RESEND_API_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = (String) response.getBody().get("id");
                logger.info("【Resend】发送成功: {} -> {}, messageId={}", subject, toEmail, messageId);
                return EmailResult.success("resend", messageId);
            } else {
                String error = response.getBody() != null ? response.getBody().toString() : "Unknown error";
                logger.error("【Resend】发送失败 [{}]: {}", toEmail, error);
                return EmailResult.failure("resend", error);
            }

        } catch (Exception e) {
            logger.error("【Resend】发送异常 [{}]: {}", toEmail, e.getMessage());
            return EmailResult.failure("resend", e.getMessage());
        }
    }
}
