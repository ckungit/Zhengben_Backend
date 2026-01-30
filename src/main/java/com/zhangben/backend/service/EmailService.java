package com.zhangben.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:noreply@aabillpay.com}")
    private String senderEmail;

    @Value("${brevo.sender-name:Pay友}")
    private String senderName;

    @Value("${app.base-url:https://www.aabillpay.com}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送密码重置邮件
     */
    public boolean sendPasswordResetEmail(String toEmail, String toName, String resetToken) {
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        String subject = "【Pay友】密码重置";
        String htmlContent = buildPasswordResetHtml(toName, resetLink);

        return sendEmail(toEmail, toName, subject, htmlContent);
    }

    /**
     * 发送邮件（通用方法）
     */
    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            logger.error("Brevo API key not configured");
            return false;
        }

        try {
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = new HashMap<>();
            
            // 发件人
            Map<String, String> sender = new HashMap<>();
            sender.put("name", senderName);
            sender.put("email", senderEmail);
            body.put("sender", sender);

            // 收件人
            List<Map<String, String>> to = new ArrayList<>();
            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail);
            if (toName != null && !toName.isEmpty()) {
                recipient.put("name", toName);
            }
            to.add(recipient);
            body.put("to", to);

            // 主题和内容
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Email sent successfully to {}", toEmail);
                return true;
            } else {
                logger.error("Failed to send email: {}", response.getBody());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * 构建密码重置邮件HTML
     */
    private String buildPasswordResetHtml(String userName, String resetLink) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "</head>" +
            "<body style='margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;'>" +
            "<div style='max-width:600px;margin:0 auto;padding:40px 20px;'>" +
            
            // Logo 和标题
            "<div style='text-align:center;margin-bottom:32px;'>" +
            "<h1 style='color:#FFA726;font-size:28px;margin:0 0 8px 0;'>Pay友</h1>" +
            "<p style='color:#666;font-size:14px;margin:0;'>AA记账分账神器</p>" +
            "</div>" +
            
            // 内容卡片
            "<div style='background:#fff;border-radius:16px;padding:32px;box-shadow:0 4px 20px rgba(255,152,0,0.1);'>" +
            "<h2 style='color:#333;font-size:20px;margin:0 0 16px 0;'>密码重置</h2>" +
            "<p style='color:#666;font-size:15px;line-height:1.6;margin:0 0 24px 0;'>" +
            "你好" + (userName != null ? " " + userName : "") + "，<br><br>" +
            "我们收到了你的密码重置请求。请点击下方按钮重置密码：" +
            "</p>" +
            
            // 重置按钮
            "<div style='text-align:center;margin:32px 0;'>" +
            "<a href='" + resetLink + "' style='display:inline-block;padding:14px 40px;background:linear-gradient(135deg,#FFA726 0%,#FF9800 100%);color:#fff;text-decoration:none;border-radius:10px;font-size:16px;font-weight:600;box-shadow:0 4px 15px rgba(255,152,0,0.3);'>重置密码</a>" +
            "</div>" +
            
            // 备用链接
            "<p style='color:#999;font-size:13px;line-height:1.6;margin:24px 0 0 0;'>" +
            "如果按钮无法点击，请复制以下链接到浏览器：<br>" +
            "<a href='" + resetLink + "' style='color:#FFA726;word-break:break-all;'>" + resetLink + "</a>" +
            "</p>" +
            
            // 警告
            "<div style='margin-top:24px;padding:16px;background:#fff8f0;border-radius:8px;border-left:4px solid #FFA726;'>" +
            "<p style='color:#666;font-size:13px;margin:0;'>" +
            "⏰ 此链接将在 <strong>1小时</strong> 后失效<br>" +
            "🔒 如果这不是你本人的操作，请忽略此邮件" +
            "</p>" +
            "</div>" +
            "</div>" +
            
            // 页脚
            "<div style='text-align:center;margin-top:32px;color:#999;font-size:12px;'>" +
            "<p style='margin:0 0 8px 0;'>© 2025 Pay友 Paybill</p>" +
            "<p style='margin:0;'>这是一封自动发送的邮件，请勿直接回复</p>" +
            "</div>" +
            
            "</div>" +
            "</body>" +
            "</html>";
    }
}
