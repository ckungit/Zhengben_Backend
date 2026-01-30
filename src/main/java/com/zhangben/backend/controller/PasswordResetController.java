package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.PasswordResetTokenMapper;
import com.zhangben.backend.model.PasswordResetToken;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordResetTokenMapper tokenMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 请求密码重置（发送邮件）
     */
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("请输入邮箱");
        }

        email = email.trim().toLowerCase();

        // 查找用户
        UserExample example = new UserExample();
        example.createCriteria().andEmailEqualTo(email);
        List<User> users = userMapper.selectByExample(example);

        // 无论用户是否存在，都返回成功（防止邮箱枚举攻击）
        Map<String, Object> result = new HashMap<>();
        result.put("message", "如果该邮箱已注册，你将收到一封密码重置邮件");

        if (users.isEmpty()) {
            return result;
        }

        User user = users.get(0);

        // 删除该用户之前未使用的重置token
        tokenMapper.deleteUnusedByUserId(user.getId());

        // 生成新的重置token
        String token = generateToken();
        
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(token);
        
        // 1小时后过期
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1);
        resetToken.setExpiresAt(cal.getTime());

        tokenMapper.insert(resetToken);

        // 发送邮件
        emailService.sendPasswordResetEmail(user.getEmail(), user.getNickname(), token);

        return result;
    }

    /**
     * 验证重置token是否有效
     */
    @GetMapping("/verify-reset-token")
    public Map<String, Object> verifyResetToken(@RequestParam String token) {
        PasswordResetToken resetToken = tokenMapper.selectByToken(token);

        Map<String, Object> result = new HashMap<>();

        if (resetToken == null) {
            result.put("valid", false);
            result.put("message", "无效的重置链接");
            return result;
        }

        if (resetToken.isUsed()) {
            result.put("valid", false);
            result.put("message", "该链接已被使用");
            return result;
        }

        if (resetToken.isExpired()) {
            result.put("valid", false);
            result.put("message", "该链接已过期，请重新申请");
            return result;
        }

        result.put("valid", true);
        return result;
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");

        if (token == null || token.isEmpty()) {
            throw new RuntimeException("缺少重置token");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("密码至少8位");
        }

        // 验证token
        PasswordResetToken resetToken = tokenMapper.selectByToken(token);

        if (resetToken == null) {
            throw new RuntimeException("无效的重置链接");
        }

        if (resetToken.isUsed()) {
            throw new RuntimeException("该链接已被使用");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("该链接已过期，请重新申请");
        }

        // 更新密码
        User user = userMapper.selectByPrimaryKey(resetToken.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateByPrimaryKeySelective(update);

        // 标记token已使用
        tokenMapper.markAsUsed(resetToken.getId());

        // ★★★ 踢出该用户所有已登录的会话 ★★★
        StpUtil.logout(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("message", "密码重置成功，请使用新密码登录");
        return result;
    }

    /**
     * 生成随机token
     */
    private String generateToken() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
