package com.zhangben.backend.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.SaLoginModel;
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
            return ResponseEntity.status(401).body("用户不存在");
        }

        if (!userService.checkPassword(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("密码错误");
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

        return ResponseEntity.ok(
                java.util.Map.of(
                        "token", StpUtil.getTokenValue(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "rememberMe", req.getRememberMe() != null && req.getRememberMe()
                )
        );
    }

    // 注册
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        if (userService.findByEmail(req.getEmail()) != null) {
            return ResponseEntity.badRequest().body("邮箱已被注册");
        }

        req.setRole("user"); // 默认角色

        userService.createUser(req);

        return ResponseEntity.ok("注册成功");
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
        return ResponseEntity.ok("仅管理员可见");
    }

    // 登出
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        StpUtil.logout();
        return ResponseEntity.ok("已退出登录");
    }
}
