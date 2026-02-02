package com.zhangben.backend.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * V34: 邮件提供商管理器
 * 负责：
 * 1. 根据配置选择主要供应商
 * 2. 自动降级到备用供应商
 * 3. 提供异步发送能力
 */
@Service
public class EmailProviderManager {

    private static final Logger logger = LoggerFactory.getLogger(EmailProviderManager.class);

    // 邮件总开关
    @Value("${mail.enabled:true}")
    private boolean mailEnabled;

    // 首选供应商配置
    @Value("${app.mail.provider:brevo}")
    private String preferredProvider;

    // 是否启用自动降级
    @Value("${app.mail.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Autowired
    private BrevoEmailProvider brevoProvider;

    @Autowired
    private ResendEmailProvider resendProvider;

    @Autowired
    private MockEmailProvider mockProvider;

    // 供应商映射表
    private Map<String, EmailProvider> providerMap;

    // 降级顺序
    private List<EmailProvider> fallbackChain;

    @PostConstruct
    public void init() {
        // 构建供应商映射
        providerMap = Map.of(
            "brevo", brevoProvider,
            "resend", resendProvider,
            "mock", mockProvider
        );

        // 构建降级链
        fallbackChain = new ArrayList<>();

        // 首选供应商放在第一位
        EmailProvider primary = providerMap.get(preferredProvider.toLowerCase());
        if (primary != null && !"mock".equals(preferredProvider.toLowerCase())) {
            fallbackChain.add(primary);
        }

        // 添加其他可用供应商（除 mock 外）
        for (EmailProvider provider : List.of(brevoProvider, resendProvider)) {
            if (!fallbackChain.contains(provider)) {
                fallbackChain.add(provider);
            }
        }

        // Mock 永远作为最后的保底
        fallbackChain.add(mockProvider);

        // 输出初始化状态
        logger.info("【邮件管理器】初始化完成");
        logger.info("  - 邮件功能: {}", mailEnabled ? "启用" : "禁用");
        logger.info("  - 首选供应商: {}", preferredProvider);
        logger.info("  - 自动降级: {}", fallbackEnabled ? "启用" : "禁用");
        logger.info("  - 降级链: {}", fallbackChain.stream()
                .map(p -> p.getProviderName() + "(" + (p.isAvailable() ? "可用" : "不可用") + ")")
                .collect(Collectors.joining(" -> ")));
    }

    /**
     * 同步发送邮件（带自动降级）
     */
    public EmailResult sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        // 检查总开关
        if (!mailEnabled) {
            logger.debug("【邮件管理器】mail.enabled=false，跳过发送: {}", toEmail);
            return EmailResult.skipped("邮件功能已禁用");
        }

        // 如果首选是 mock，直接使用
        if ("mock".equalsIgnoreCase(preferredProvider)) {
            return mockProvider.sendEmail(toEmail, toName, subject, htmlContent);
        }

        // 尝试按降级链发送
        EmailResult lastResult = null;
        for (EmailProvider provider : fallbackChain) {
            if (!provider.isAvailable()) {
                logger.debug("【邮件管理器】跳过不可用供应商: {}", provider.getProviderName());
                continue;
            }

            lastResult = provider.sendEmail(toEmail, toName, subject, htmlContent);

            if (lastResult.isSuccess()) {
                return lastResult;
            }

            // 发送失败，检查是否允许降级
            if (!fallbackEnabled) {
                logger.warn("【邮件管理器】{} 发送失败，降级已禁用，不再尝试其他供应商", provider.getProviderName());
                return lastResult;
            }

            logger.warn("【邮件管理器】{} 发送失败，尝试下一个供应商...", provider.getProviderName());
        }

        // 所有供应商都失败了
        return lastResult != null ? lastResult : EmailResult.failure("none", "所有邮件供应商都不可用");
    }

    /**
     * 异步发送邮件
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<EmailResult> sendEmailAsync(String toEmail, String toName,
                                                          String subject, String htmlContent) {
        EmailResult result = sendEmail(toEmail, toName, subject, htmlContent);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 异步发送邮件（Fire-and-forget 模式，不关心结果）
     */
    @Async("emailTaskExecutor")
    public void sendEmailFireAndForget(String toEmail, String toName, String subject, String htmlContent) {
        try {
            EmailResult result = sendEmail(toEmail, toName, subject, htmlContent);
            if (!result.isSuccess()) {
                logger.warn("【邮件管理器】异步发送失败: {} -> {}", toEmail, result.getMessage());
            }
        } catch (Exception e) {
            logger.error("【邮件管理器】异步发送异常: {} -> {}", toEmail, e.getMessage());
        }
    }

    /**
     * 获取当前可用的供应商
     */
    public EmailProvider getAvailableProvider() {
        if (!mailEnabled) {
            return mockProvider;
        }

        for (EmailProvider provider : fallbackChain) {
            if (provider.isAvailable()) {
                return provider;
            }
        }
        return mockProvider;
    }

    /**
     * 获取供应商状态报告
     */
    public Map<String, Boolean> getProviderStatus() {
        return Map.of(
            "brevo", brevoProvider.isAvailable(),
            "resend", resendProvider.isAvailable(),
            "mock", mockProvider.isAvailable()
        );
    }

    /**
     * 检查邮件功能是否可用
     */
    public boolean isMailEnabled() {
        return mailEnabled;
    }

    /**
     * 获取当前首选供应商
     */
    public String getPreferredProvider() {
        return preferredProvider;
    }
}
