package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.SystemConfigMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.SystemConfig;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 用户反馈接口
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 检查反馈功能是否启用
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        SystemConfig enabledConfig = systemConfigMapper.selectByKey("feedback.enabled");
        boolean isEnabled = enabledConfig != null && "true".equals(enabledConfig.getConfigValue());

        return ResponseEntity.ok(Map.of("enabled", isEnabled));
    }

    /**
     * 提交反馈
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request) {
        // 检查功能是否启用
        SystemConfig enabledConfig = systemConfigMapper.selectByKey("feedback.enabled");
        if (enabledConfig == null || !"true".equals(enabledConfig.getConfigValue())) {
            return ResponseEntity.badRequest().body(Map.of("error", "反馈功能暂未开放"));
        }

        // 获取目标邮箱
        SystemConfig targetEmailConfig = systemConfigMapper.selectByKey("feedback.target_email");
        if (targetEmailConfig == null || targetEmailConfig.getConfigValue() == null || targetEmailConfig.getConfigValue().isEmpty()) {
            logger.error("反馈目标邮箱未配置");
            return ResponseEntity.internalServerError().body(Map.of("error", "反馈服务配置错误"));
        }

        String targetEmail = targetEmailConfig.getConfigValue();

        // 验证请求
        if (request.title == null || request.title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入标题"));
        }
        if (request.content == null || request.content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入反馈内容"));
        }

        // 获取当前用户信息（如果已登录）
        String userInfo = "未登录用户";
        if (StpUtil.isLogin()) {
            try {
                Integer userId = StpUtil.getLoginIdAsInt();
                User user = userMapper.selectByPrimaryKey(userId);
                if (user != null) {
                    userInfo = String.format("%s (%s, ID: %d)",
                        user.getNickname(), user.getEmail(), user.getId());
                }
            } catch (Exception e) {
                logger.warn("获取用户信息失败: {}", e.getMessage());
            }
        }

        // 构建邮件内容
        String categoryText = getCategoryText(request.category);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String subject = String.format("[Pay友反馈] %s: %s", categoryText, request.title.trim());

        String htmlContent = buildFeedbackEmailHtml(
            request.title.trim(),
            categoryText,
            request.content.trim(),
            request.contact,
            userInfo,
            timestamp
        );

        // 发送邮件
        boolean success = emailService.sendEmail(targetEmail, "Pay友管理员", subject, htmlContent);

        if (success) {
            logger.info("用户反馈已发送: {} -> {}", subject, targetEmail);
            return ResponseEntity.ok(Map.of("message", "反馈提交成功，感谢您的意见！"));
        } else {
            logger.error("用户反馈发送失败: {}", subject);
            return ResponseEntity.internalServerError().body(Map.of("error", "发送失败，请稍后重试"));
        }
    }

    /**
     * 获取分类文本
     */
    private String getCategoryText(String category) {
        if (category == null) return "其他";
        return switch (category) {
            case "bug" -> "问题反馈";
            case "feature" -> "功能建议";
            case "improvement" -> "体验改进";
            default -> "其他";
        };
    }

    /**
     * 构建反馈邮件 HTML 内容
     */
    private String buildFeedbackEmailHtml(String title, String category, String content,
                                           String contact, String userInfo, String timestamp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #FF9500 0%, #E68600 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 8px 8px; }
                    .field { margin-bottom: 16px; }
                    .label { font-weight: 600; color: #666; margin-bottom: 4px; }
                    .value { background: white; padding: 12px; border-radius: 6px; border: 1px solid #e0e0e0; }
                    .category { display: inline-block; background: #FF9500; color: white; padding: 4px 12px; border-radius: 12px; font-size: 14px; }
                    .footer { margin-top: 20px; padding-top: 16px; border-top: 1px solid #e0e0e0; font-size: 12px; color: #999; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2 style="margin: 0;">📬 用户反馈</h2>
                        <p style="margin: 8px 0 0 0; opacity: 0.9;">%s</p>
                    </div>
                    <div class="content">
                        <div class="field">
                            <div class="label">分类</div>
                            <div><span class="category">%s</span></div>
                        </div>
                        <div class="field">
                            <div class="label">标题</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">详细内容</div>
                            <div class="value" style="white-space: pre-wrap;">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">联系方式</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="field">
                            <div class="label">用户信息</div>
                            <div class="value">%s</div>
                        </div>
                        <div class="footer">
                            此邮件由 Pay友 系统自动发送
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                timestamp,
                category,
                escapeHtml(title),
                escapeHtml(content),
                contact != null && !contact.isEmpty() ? escapeHtml(contact) : "未提供",
                escapeHtml(userInfo)
            );
    }

    /**
     * 转义 HTML 特殊字符
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * 反馈请求
     */
    public static class FeedbackRequest {
        public String title;
        public String category;
        public String content;
        public String contact;
    }
}
