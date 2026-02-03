package com.zhangben.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.CurrentUserResponse;
import com.zhangben.backend.dto.LoginRequest;
import com.zhangben.backend.dto.RegisterRequest;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.R2StorageService;
import com.zhangben.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.zhangben.backend.util.CurrencyUtils;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private R2StorageService r2StorageService;

    // 登录
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        User user = userService.findByEmail(req.getEmail());
        
        // 合并错误信息，防止暴力破解
        if (user == null || !userService.checkPassword(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("邮箱或密码错误");
        }

        // 根据 rememberMe 设置不同的过期时间
        if (req.getRememberMe() != null && req.getRememberMe()) {
            // 记住我：30天过期
            StpUtil.login(user.getId(), new SaLoginModel()
                    .setTimeout(60 * 60 * 24 * 30)  // 30天（秒）
            );
        } else {
            // 不记住：使用较短的过期时间（浏览器关闭后需重新登录）
            // 设置为 -1 表示临时 token，关闭浏览器即失效
            // 但由于前端使用 sessionStorage，实际效果由前端控制
            StpUtil.login(user.getId(), new SaLoginModel()
                    .setTimeout(60 * 60 * 24)  // 1天
            );
        }

        // V39: 登录成功后将货币存入 Session
        CurrencyUtils.setSessionCurrency(user.getPrimaryCurrency());

        return ResponseEntity.ok(
                java.util.Map.of(
                        "token", StpUtil.getTokenValue(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "rememberMe", req.getRememberMe() != null && req.getRememberMe(),
                        "primaryCurrency", user.getPrimaryCurrency() != null ? user.getPrimaryCurrency() : "JPY"
                )
        );
    }

    // 注册
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        // 验证密码长度
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body("密码至少8位");
        }

        // V39: 验证货币字段
        if (req.getPrimaryCurrency() == null || req.getPrimaryCurrency().isEmpty()) {
            return ResponseEntity.badRequest().body("请选择主要货币");
        }
        if (!CurrencyUtils.isValidCurrency(req.getPrimaryCurrency())) {
            return ResponseEntity.badRequest().body("不支持的货币类型");
        }

        if (userService.findByEmail(req.getEmail()) != null) {
            return ResponseEntity.badRequest().body("邮箱已被注册");
        }

        req.setRole("user"); // 默认角色

        User user = userService.createUser(req);

        // V24: 处理头像上传 (Base64)
        if (req.getAvatarBase64() != null && !req.getAvatarBase64().isEmpty()) {
            try {
                String avatarUrl = uploadAvatarFromBase64(req.getAvatarBase64(), user.getId());
                if (avatarUrl != null) {
                    User updateUser = new User();
                    updateUser.setId(user.getId());
                    updateUser.setAvatarUrl(avatarUrl);
                    updateUser.setUpdatedAt(LocalDateTime.now());
                    userMapper.updateByPrimaryKeySelective(updateUser);
                }
            } catch (Exception e) {
                // 头像上传失败不影响注册，仅记录日志
                System.err.println("注册时头像上传失败: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("注册成功");
    }

    /**
     * 从 Base64 数据上传头像到 R2
     */
    private String uploadAvatarFromBase64(String base64Data, Integer userId) {
        try {
            // 解析 Base64 数据 (格式: data:image/webp;base64,xxxxx)
            String[] parts = base64Data.split(",");
            if (parts.length != 2) {
                return null;
            }

            // 提取 MIME 类型
            String mimeType = "image/webp";
            if (parts[0].contains("image/jpeg")) {
                mimeType = "image/jpeg";
            } else if (parts[0].contains("image/png")) {
                mimeType = "image/png";
            }

            // 解码 Base64
            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);

            // 检查大小 (最大 50KB)
            if (imageBytes.length > 51200) {
                System.err.println("头像文件过大: " + imageBytes.length + " bytes");
                return null;
            }

            // 上传到 R2
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            return r2StorageService.uploadAvatar(inputStream, mimeType, imageBytes.length, userId);

        } catch (Exception e) {
            System.err.println("头像 Base64 解析失败: " + e.getMessage());
            return null;
        }
    }

    // 当前用户信息
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        StpUtil.checkLogin();

        int userId = StpUtil.getLoginIdAsInt();
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body("用户不存在");
        }

        CurrentUserResponse resp = new CurrentUserResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setNickname(user.getNickname());
        resp.setRole(user.getRole());
        resp.setFirstname(user.getFirstname());
        resp.setSecondname(user.getSecondname());
        resp.setPaypayFlag(user.getPaypayFlag());
        resp.setBankFlag(user.getBankFlag());
        resp.setPrimaryCurrency(user.getPrimaryCurrency() != null ? user.getPrimaryCurrency() : "JPY");

        return ResponseEntity.ok(resp);
    }

    // 登出
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        StpUtil.logout();
        return ResponseEntity.ok("已登出");
    }

    // 测试接口（仅管理员）
    @SaCheckRole("admin")
    @GetMapping("/admin/test")
    public ResponseEntity<?> adminTest() {
        return ResponseEntity.ok("欢迎管理员！你有 admin 权限");
    }
}