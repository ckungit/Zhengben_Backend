package com.zhangben.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.CurrentUserResponse;
import com.zhangben.backend.dto.LoginRequest;
import com.zhangben.backend.dto.RegisterRequest;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // 登录
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        User user = userService.findByEmail(req.getEmail());
        if (user == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        if (!userService.checkPassword(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Wrong password");
        }

        StpUtil.login(user.getId());

        return ResponseEntity.ok(
                java.util.Map.of(
                        "token", StpUtil.getTokenValue(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail(),
                        "role", user.getRole()
                )
        );
    }

    // 注册
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        if (userService.findByEmail(req.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        req.setRole("user"); // 默认角色

        userService.createUser(req);

        return ResponseEntity.ok("Registered");
    }

    // 当前用户信息
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        StpUtil.checkLogin();

        Integer userId = StpUtil.getLoginIdAsInt();
        User user = userService.findById(userId);

        return ResponseEntity.ok(
                new CurrentUserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getNickname(),
                        user.getRole()
                )
        );
    }

    // 只有 admin 能访问
    @SaCheckRole("admin")
    @GetMapping("/admin-only")
    public ResponseEntity<?> adminOnly() {
        return ResponseEntity.ok("Only admin can see this");
    }

    // 登出
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        StpUtil.logout();
        return ResponseEntity.ok("Logged out");
    }
}