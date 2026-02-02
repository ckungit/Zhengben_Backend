package com.zhangben.backend.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * V34: Mock 邮件提供商
 * 仅记录日志，不发送真实邮件
 * 用于开发环境或所有真实供应商不可用时的保底方案
 */
@Component
public class MockEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailProvider.class);

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        // Mock 提供商永远可用
        return true;
    }

    @Override
    public EmailResult sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        logger.info("【Mock邮件】模拟发送邮件:");
        logger.info("  收件人: {} <{}>", toName != null ? toName : "N/A", toEmail);
        logger.info("  主题: {}", subject);
        logger.info("  内容长度: {} 字符", htmlContent != null ? htmlContent.length() : 0);

        // 模拟发送成功
        return EmailResult.success("mock", "mock-" + System.currentTimeMillis());
    }

    @Override
    public EmailResult sendEmail(String fromEmail, String fromName,
                                  String toEmail, String toName,
                                  String subject, String htmlContent) {
        logger.info("【Mock邮件】模拟发送邮件:");
        logger.info("  发件人: {} <{}>", fromName != null ? fromName : "N/A", fromEmail);
        logger.info("  收件人: {} <{}>", toName != null ? toName : "N/A", toEmail);
        logger.info("  主题: {}", subject);
        logger.info("  内容长度: {} 字符", htmlContent != null ? htmlContent.length() : 0);

        return EmailResult.success("mock", "mock-" + System.currentTimeMillis());
    }
}
