package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.SubscriptionInfoResponse;
import com.zhangben.backend.mapper.SystemConfigMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.SystemConfig;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private SubscriptionService subscriptionService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 管理员重置用户密码
     * 生成一个随机临时密码，用户登录后自行修改
     */
    @PostMapping("/reset-user-password")
    public ResponseEntity<?> resetUserPassword(@RequestParam String email) {
        
        // 检查当前用户是否是管理员
        StpUtil.checkLogin();
        Integer currentUserId = StpUtil.getLoginIdAsInt();
        User currentUser = userMapper.selectByPrimaryKey(currentUserId);
        
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "无权限，仅管理员可操作");
            return ResponseEntity.status(403).body(error);
        }

        // 查找目标用户
        UserExample example = new UserExample();
        example.createCriteria().andEmailEqualTo(email);
        List<User> users = userMapper.selectByExample(example);

        if (users.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "用户不存在");
            return ResponseEntity.badRequest().body(error);
        }

        // 生成随机密码（8位字母数字）
        String tempPassword = generateTempPassword();

        // 更新密码
        User targetUser = users.get(0);
        targetUser.setPassword(passwordEncoder.encode(tempPassword));
        userMapper.updateByPrimaryKeySelective(targetUser);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "密码已重置");
        result.put("email", email);
        result.put("nickname", targetUser.getNickname());
        result.put("tempPassword", tempPassword);  // 返回临时密码，管理员告知用户

        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有用户列表（管理员用）
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        
        StpUtil.checkLogin();
        Integer currentUserId = StpUtil.getLoginIdAsInt();
        User currentUser = userMapper.selectByPrimaryKey(currentUserId);
        
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "无权限，仅管理员可操作");
            return ResponseEntity.status(403).body(error);
        }

        UserExample example = new UserExample();
        example.setOrderByClause("id ASC");
        List<User> users = userMapper.selectByExample(example);

        // 移除敏感信息
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("nickname", u.getNickname());
            map.put("role", u.getRole());
            map.put("fullName", (u.getSecondname() != null ? u.getSecondname() : "") 
                              + (u.getFirstname() != null ? u.getFirstname() : ""));
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ==================== 系统配置管理 ====================

    /**
     * 获取反馈配置
     */
    @GetMapping("/feedback-config")
    public ResponseEntity<?> getFeedbackConfig() {
        if (!checkAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可操作"));
        }

        SystemConfig enabledConfig = systemConfigMapper.selectByKey("feedback.enabled");
        SystemConfig emailConfig = systemConfigMapper.selectByKey("feedback.target_email");

        Map<String, Object> result = new HashMap<>();
        result.put("enabled", enabledConfig != null ? "true".equals(enabledConfig.getConfigValue()) : false);
        result.put("targetEmail", emailConfig != null ? emailConfig.getConfigValue() : "");

        return ResponseEntity.ok(result);
    }

    /**
     * 更新反馈配置
     */
    @PutMapping("/feedback-config")
    public ResponseEntity<?> updateFeedbackConfig(@RequestBody Map<String, Object> body) {
        if (!checkAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可操作"));
        }

        // 更新启用状态
        if (body.containsKey("enabled")) {
            SystemConfig config = systemConfigMapper.selectByKey("feedback.enabled");
            if (config != null) {
                config.setConfigValue(Boolean.TRUE.equals(body.get("enabled")) ? "true" : "false");
                systemConfigMapper.updateValue(config);
            }
        }

        // 更新目标邮箱
        if (body.containsKey("targetEmail")) {
            SystemConfig config = systemConfigMapper.selectByKey("feedback.target_email");
            if (config != null) {
                config.setConfigValue((String) body.get("targetEmail"));
                systemConfigMapper.updateValue(config);
            }
        }

        return ResponseEntity.ok(Map.of("message", "反馈配置已更新"));
    }

    // ==================== V42: 订阅管理 ====================

    /**
     * V42: 将用户提升为永久 PRO 会员
     */
    @PostMapping("/user/promote")
    public ResponseEntity<?> promoteUserToPermanentPro(@RequestParam Integer userId) {
        if (!checkAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可操作"));
        }

        User targetUser = userMapper.selectByPrimaryKey(userId);
        if (targetUser == null) {
            return ResponseEntity.status(404).body(Map.of("error", "用户不存在"));
        }

        Integer adminId = StpUtil.getLoginIdAsInt();
        subscriptionService.promoteToPermenentPro(userId, adminId);

        return ResponseEntity.ok(Map.of(
            "message", "已将用户提升为永久 PRO 会员",
            "userId", userId,
            "nickname", targetUser.getNickname()
        ));
    }

    /**
     * V42: 修改用户订阅信息
     */
    @PutMapping("/user/{userId}/subscription")
    public ResponseEntity<?> updateUserSubscription(
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> body) {
        if (!checkAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可操作"));
        }

        User targetUser = userMapper.selectByPrimaryKey(userId);
        if (targetUser == null) {
            return ResponseEntity.status(404).body(Map.of("error", "用户不存在"));
        }

        String tier = (String) body.get("tier");
        String type = (String) body.get("type");
        Integer durationDays = body.get("durationDays") != null
            ? ((Number) body.get("durationDays")).intValue()
            : null;

        Integer adminId = StpUtil.getLoginIdAsInt();
        subscriptionService.updateSubscription(userId, tier, type, durationDays, adminId);

        return ResponseEntity.ok(Map.of("message", "订阅信息已更新"));
    }

    /**
     * V42: 获取用户订阅信息
     */
    @GetMapping("/user/{userId}/subscription")
    public ResponseEntity<?> getUserSubscription(@PathVariable Integer userId) {
        if (!checkAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可操作"));
        }

        SubscriptionInfoResponse info = subscriptionService.getSubscriptionInfo(userId);
        return ResponseEntity.ok(info);
    }

    /**
     * 检查当前用户是否是管理员
     */
    private boolean checkAdmin() {
        if (!StpUtil.isLogin()) {
            return false;
        }
        Integer currentUserId = StpUtil.getLoginIdAsInt();
        User currentUser = userMapper.selectByPrimaryKey(currentUserId);
        return currentUser != null && "admin".equals(currentUser.getRole());
    }
}
