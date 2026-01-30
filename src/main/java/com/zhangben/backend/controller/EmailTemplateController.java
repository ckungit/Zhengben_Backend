package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.EmailTemplate;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 邮件模板管理 API（仅管理员可用）
 */
@RestController
@RequestMapping("/api/admin/email-templates")
public class EmailTemplateController {

    @Autowired
    private EmailTemplateService templateService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 检查管理员权限
     */
    private void checkAdmin() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null || !"admin".equals(user.getRole())) {
            throw new RuntimeException("需要管理员权限");
        }
    }

    /**
     * 获取所有模板代码
     */
    @GetMapping("/codes")
    public List<String> getCodes() {
        checkAdmin();
        return templateService.getAllTemplateCodes();
    }

    /**
     * 获取所有模板
     */
    @GetMapping
    public List<EmailTemplate> getAll() {
        checkAdmin();
        return templateService.getAllTemplates();
    }

    /**
     * 获取指定模板代码的所有语言版本
     */
    @GetMapping("/code/{code}")
    public List<EmailTemplate> getByCode(@PathVariable String code) {
        checkAdmin();
        return templateService.getTemplateAllLanguages(code);
    }

    /**
     * 获取单个模板
     */
    @GetMapping("/{code}/{language}")
    public ResponseEntity<?> getOne(@PathVariable String code, @PathVariable String language) {
        checkAdmin();
        EmailTemplate template = templateService.getTemplate(code, language);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    /**
     * 按ID获取模板
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        checkAdmin();
        EmailTemplate template = templateService.getTemplateById(id);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    /**
     * 保存模板（新增或更新）
     */
    @PostMapping
    public ResponseEntity<?> save(@RequestBody EmailTemplate template) {
        checkAdmin();

        if (template.getTemplateCode() == null || template.getTemplateCode().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "模板代码不能为空"));
        }
        if (template.getLanguage() == null || template.getLanguage().isEmpty()) {
            template.setLanguage("zh-CN");
        }

        boolean success = templateService.saveTemplate(template);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "保存成功"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "保存失败"));
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        checkAdmin();
        boolean success = templateService.deleteTemplate(id);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "删除失败"));
        }
    }

    /**
     * 预览模板
     */
    @GetMapping("/preview/{code}/{language}")
    public ResponseEntity<?> preview(@PathVariable String code, @PathVariable String language) {
        checkAdmin();
        String html = templateService.previewTemplate(code, language);
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "html", html,
            "variablesHint", templateService.getVariablesHint(code, language)
        ));
    }

    /**
     * 获取支持的语言列表
     */
    @GetMapping("/languages")
    public List<Map<String, String>> getLanguages() {
        checkAdmin();
        return List.of(
            Map.of("code", "zh-CN", "name", "简体中文"),
            Map.of("code", "en-US", "name", "English"),
            Map.of("code", "ja-JP", "name", "日本語"),
            Map.of("code", "zh-TW", "name", "繁體中文")
        );
    }
}
