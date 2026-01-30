package com.zhangben.backend.service;

import com.zhangben.backend.mapper.EmailTemplateMapper;
import com.zhangben.backend.model.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 邮件模板服务 - 使用 Thymeleaf 渲染数据库中的模板
 * 支持多语言、动态变量替换
 */
@Service
public class EmailTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateService.class);

    private static final String DEFAULT_LANGUAGE = "zh-CN";

    @Autowired
    private EmailTemplateMapper templateMapper;

    private TemplateEngine templateEngine;

    @PostConstruct
    public void init() {
        // 配置 Thymeleaf 模板引擎（用于字符串模板）
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false); // 禁用缓存，便于动态更新模板

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        logger.info("EmailTemplateService 初始化完成");
    }

    /**
     * 渲染邮件模板
     * @param templateCode 模板代码
     * @param language 语言代码（如 zh-CN, en-US, ja-JP）
     * @param variables 模板变量
     * @return 渲染后的 HTML 内容，如果模板不存在返回 null
     */
    public String renderTemplate(String templateCode, String language, Map<String, Object> variables) {
        // 尝试获取指定语言的模板
        EmailTemplate template = templateMapper.selectByCodeAndLanguage(templateCode, language);

        // 如果没有指定语言的模板，尝试默认语言
        if (template == null && !DEFAULT_LANGUAGE.equals(language)) {
            logger.info("未找到 {} 语言的模板 {}，尝试默认语言", language, templateCode);
            template = templateMapper.selectByCodeAndLanguage(templateCode, DEFAULT_LANGUAGE);
        }

        if (template == null) {
            logger.error("邮件模板不存在: {} ({})", templateCode, language);
            return null;
        }

        try {
            Context context = new Context(parseLocale(language));
            if (variables != null) {
                context.setVariables(variables);
            }

            return templateEngine.process(template.getHtmlContent(), context);
        } catch (Exception e) {
            logger.error("渲染邮件模板失败 [{}/{}]: {}", templateCode, language, e.getMessage());
            return null;
        }
    }

    /**
     * 渲染邮件主题
     * @param templateCode 模板代码
     * @param language 语言代码
     * @param variables 模板变量
     * @return 渲染后的主题，如果模板不存在返回默认主题
     */
    public String renderSubject(String templateCode, String language, Map<String, Object> variables) {
        EmailTemplate template = templateMapper.selectByCodeAndLanguage(templateCode, language);

        if (template == null && !DEFAULT_LANGUAGE.equals(language)) {
            template = templateMapper.selectByCodeAndLanguage(templateCode, DEFAULT_LANGUAGE);
        }

        if (template == null) {
            return "通知";
        }

        try {
            Context context = new Context(parseLocale(language));
            if (variables != null) {
                context.setVariables(variables);
            }

            return templateEngine.process(template.getSubject(), context);
        } catch (Exception e) {
            logger.error("渲染邮件主题失败 [{}/{}]: {}", templateCode, language, e.getMessage());
            return template.getSubject(); // 返回原始主题
        }
    }

    /**
     * 获取模板的变量说明
     */
    public String getVariablesHint(String templateCode, String language) {
        EmailTemplate template = templateMapper.selectByCodeAndLanguage(templateCode, language);
        if (template == null) {
            template = templateMapper.selectByCodeAndLanguage(templateCode, DEFAULT_LANGUAGE);
        }
        return template != null ? template.getVariablesHint() : null;
    }

    /**
     * 获取所有模板（管理用）
     */
    public List<EmailTemplate> getAllTemplates() {
        return templateMapper.selectAll();
    }

    /**
     * 获取指定模板代码的所有语言版本
     */
    public List<EmailTemplate> getTemplateAllLanguages(String templateCode) {
        return templateMapper.selectAllLanguagesByCode(templateCode);
    }

    /**
     * 获取所有不同的模板代码
     */
    public List<String> getAllTemplateCodes() {
        return templateMapper.selectDistinctCodes();
    }

    /**
     * 获取单个模板
     */
    public EmailTemplate getTemplate(String templateCode, String language) {
        return templateMapper.selectByCodeAndLanguage(templateCode, language);
    }

    /**
     * 按ID获取模板
     */
    public EmailTemplate getTemplateById(Integer id) {
        return templateMapper.selectById(id);
    }

    /**
     * 保存模板（新增或更新）
     */
    public boolean saveTemplate(EmailTemplate template) {
        try {
            EmailTemplate existing = templateMapper.selectByCodeAndLanguage(
                template.getTemplateCode(), template.getLanguage());

            if (existing != null) {
                return templateMapper.updateByCodeAndLanguage(template) > 0;
            } else {
                return templateMapper.insert(template) > 0;
            }
        } catch (Exception e) {
            logger.error("保存邮件模板失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 删除模板
     */
    public boolean deleteTemplate(Integer id) {
        return templateMapper.deleteById(id) > 0;
    }

    /**
     * 预览模板（使用示例数据渲染）
     */
    public String previewTemplate(String templateCode, String language) {
        Map<String, Object> sampleData = Map.of(
            "recipientName", "测试用户",
            "creatorName", "张三",
            "amount", "100.00",
            "perAmount", "50.00",
            "styleName", "餐饮",
            "comment", "这是一条测试备注",
            "activityName", "周末聚餐",
            "isUpdate", false,
            "loginUrl", "https://www.aabillpay.com/login",
            "year", String.valueOf(java.time.Year.now().getValue())
        );
        return renderTemplate(templateCode, language, sampleData);
    }

    /**
     * 解析语言代码为 Locale
     */
    private Locale parseLocale(String language) {
        if (language == null) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return switch (language) {
            case "en-US" -> Locale.US;
            case "ja-JP" -> Locale.JAPAN;
            case "zh-TW" -> Locale.TRADITIONAL_CHINESE;
            default -> Locale.SIMPLIFIED_CHINESE;
        };
    }
}
