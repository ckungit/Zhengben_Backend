package com.zhangben.backend.service.email;

/**
 * V34: 邮件服务提供商接口
 * 定义统一的邮件发送契约，支持多供应商实现
 */
public interface EmailProvider {

    /**
     * 获取供应商名称
     */
    String getProviderName();

    /**
     * 检查供应商是否可用（初始化成功）
     */
    boolean isAvailable();

    /**
     * 发送邮件
     * @param toEmail 收件人邮箱
     * @param toName 收件人名称（可为null）
     * @param subject 邮件主题
     * @param htmlContent HTML内容
     * @return 发送结果
     */
    EmailResult sendEmail(String toEmail, String toName, String subject, String htmlContent);

    /**
     * 发送邮件（带发件人信息）
     * @param fromEmail 发件人邮箱
     * @param fromName 发件人名称
     * @param toEmail 收件人邮箱
     * @param toName 收件人名称
     * @param subject 邮件主题
     * @param htmlContent HTML内容
     * @return 发送结果
     */
    default EmailResult sendEmail(String fromEmail, String fromName,
                                   String toEmail, String toName,
                                   String subject, String htmlContent) {
        // 默认实现忽略发件人信息，使用配置的默认发件人
        return sendEmail(toEmail, toName, subject, htmlContent);
    }
}
