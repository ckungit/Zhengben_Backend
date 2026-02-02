package com.zhangben.backend.service.email;

/**
 * V34: 邮件发送结果
 */
public class EmailResult {

    private final boolean success;
    private final String message;
    private final String providerUsed;
    private final String messageId;

    private EmailResult(boolean success, String message, String providerUsed, String messageId) {
        this.success = success;
        this.message = message;
        this.providerUsed = providerUsed;
        this.messageId = messageId;
    }

    public static EmailResult success(String provider) {
        return new EmailResult(true, "发送成功", provider, null);
    }

    public static EmailResult success(String provider, String messageId) {
        return new EmailResult(true, "发送成功", provider, messageId);
    }

    public static EmailResult failure(String provider, String message) {
        return new EmailResult(false, message, provider, null);
    }

    public static EmailResult skipped(String reason) {
        return new EmailResult(true, reason, "none", null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getProviderUsed() {
        return providerUsed;
    }

    public String getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return String.format("EmailResult{success=%s, provider=%s, message=%s}",
                success, providerUsed, message);
    }
}
