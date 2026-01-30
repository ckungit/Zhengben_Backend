package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.zhangben.backend.dto.GoogleLoginRequest;
import com.zhangben.backend.dto.GoogleSSOResponse;
import com.zhangben.backend.dto.CompleteProfileRequest;
import com.zhangben.backend.dto.LinkAccountRequest;
import com.zhangben.backend.dto.LinkExistingAccountRequest;
import com.zhangben.backend.dto.LoginResponse;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${google.client-id:}")
    private String googleClientId;

    /**
     * Google SSO 登录
     */
    @PostMapping("/login")
    public GoogleSSOResponse googleLogin(@RequestBody GoogleLoginRequest request) throws Exception {
        if (googleClientId == null || googleClientId.isEmpty()) {
            throw new RuntimeException("Google SSO 未配置");
        }

        // 验证 Google ID Token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), 
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(request.getCredential());
        if (idToken == null) {
            throw new RuntimeException("无效的 Google 凭证");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String googleId = payload.getSubject();

        GoogleSSOResponse response = new GoogleSSOResponse();
        response.setEmail(email);

        // 1. 先按 googleId 查找（已绑定 Google 的用户）
        UserExample googleIdExample = new UserExample();
        googleIdExample.createCriteria().andGoogleIdEqualTo(googleId);
        List<User> googleUsers = userMapper.selectByExample(googleIdExample);

        if (!googleUsers.isEmpty()) {
            // 已绑定 Google 的用户 - 直接登录
            User existingUser = googleUsers.get(0);
            
            // 检查资料是否完善
            if (existingUser.getProfileCompleted() == null || !existingUser.getProfileCompleted()) {
                // 需要完善资料
                StpUtil.login(existingUser.getId(), "google-sso");
                
                response.setToken(StpUtil.getTokenValue());
                response.setIsNewUser(false);
                response.setNeedsProfileCompletion(true);
                response.setEmail(existingUser.getEmail()); // 使用绑定账号的邮箱
            } else {
                // 正常登录
                StpUtil.login(existingUser.getId(), "google-sso");
                
                response.setToken(StpUtil.getTokenValue());
                response.setIsNewUser(false);
                response.setNeedsProfileCompletion(false);
                response.setNickname(existingUser.getNickname());
                response.setRole(existingUser.getRole());
                response.setEmail(existingUser.getEmail()); // 使用绑定账号的邮箱
            }
            return response;
        }

        // 2. 按邮箱查找（邮箱相同但未绑定 Google 的用户）
        UserExample emailExample = new UserExample();
        emailExample.createCriteria().andEmailEqualTo(email);
        List<User> emailUsers = userMapper.selectByExample(emailExample);

        if (emailUsers.isEmpty()) {
            // 新用户 - 临时登录，让用户选择：绑定已有账号 or 注册新账号
            StpUtil.login(googleId, "google-temp");
            StpUtil.getSession().set("googleId", googleId);
            StpUtil.getSession().set("googleEmail", email);

            response.setToken(StpUtil.getTokenValue()); // 关键：返回token
            response.setIsNewUser(true);
            response.setNeedsProfileCompletion(true);
            return response;

        } else {
            User existingUser = emailUsers.get(0);
            
            // 邮箱相同但未绑定 Google - 让用户验证密码后关联
            StpUtil.login(existingUser.getId(), "google-link-pending");
            StpUtil.getSession().set("googleId", googleId);
            StpUtil.getSession().set("googleEmail", email);

            response.setToken(StpUtil.getTokenValue());
            response.setIsNewUser(false);
            response.setNeedsProfileCompletion(false);
            // 不设置 nickname，前端会跳转到关联页面
        }

        return response;
    }

    /**
     * 完善 Google 用户资料（新用户在此创建，老用户在此更新）
     */
    @PostMapping("/complete-profile")
    public LoginResponse completeProfile(@RequestBody CompleteProfileRequest request) {
        StpUtil.checkLogin();
        
        String loginDevice = StpUtil.getLoginDevice();
        
        // 验证输入
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new RuntimeException("昵称不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new RuntimeException("密码至少8位");
        }

        User user;
        
        if ("google-temp".equals(loginDevice)) {
            // 新用户注册流程 - 从 session 获取 Google 信息并创建用户
            String googleId = (String) StpUtil.getSession().get("googleId");
            String googleEmail = (String) StpUtil.getSession().get("googleEmail");

            if (googleId == null || googleEmail == null) {
                throw new RuntimeException("Google 登录信息缺失，请重新登录");
            }

            // 检查是否已存在（防止重复）
            UserExample googleIdExample = new UserExample();
            googleIdExample.createCriteria().andGoogleIdEqualTo(googleId);
            List<User> existingUsers = userMapper.selectByExample(googleIdExample);

            if (!existingUsers.isEmpty()) {
                // 已存在，更新资料
                user = existingUsers.get(0);
                User updateUser = new User();
                updateUser.setId(user.getId());
                updateUser.setNickname(request.getNickname().trim());
                updateUser.setPassword(passwordEncoder.encode(request.getPassword()));
                updateUser.setSecondname(request.getSecondname());
                updateUser.setFirstname(request.getFirstname());
                updateUser.setPaypayFlag(request.getPaypayFlag() != null ? request.getPaypayFlag().byteValue() : 0);
                updateUser.setBankFlag(request.getBankFlag() != null ? request.getBankFlag().byteValue() : 0);
                updateUser.setProfileCompleted(true);
                userMapper.updateByPrimaryKeySelective(updateUser);
            } else {
                // 创建新用户
                user = new User();
                user.setEmail(googleEmail);
                user.setGoogleId(googleId);
                user.setNickname(request.getNickname().trim());
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setSecondname(request.getSecondname());
                user.setFirstname(request.getFirstname());
                user.setPaypayFlag(request.getPaypayFlag() != null ? request.getPaypayFlag().byteValue() : 0);
                user.setBankFlag(request.getBankFlag() != null ? request.getBankFlag().byteValue() : 0);
                user.setRole("user");
                user.setProfileCompleted(true);
                userMapper.insertSelective(user);
            }

            // 清除临时登录态
            StpUtil.logout();

            // 正式登录
            StpUtil.login(user.getId(), "google-sso");

        } else {
            // 老用户完善资料流程
            Integer userId = StpUtil.getLoginIdAsInt();
            user = userMapper.selectByPrimaryKey(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            // 更新用户信息
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setNickname(request.getNickname().trim());
            updateUser.setPassword(passwordEncoder.encode(request.getPassword()));
            updateUser.setSecondname(request.getSecondname());
            updateUser.setFirstname(request.getFirstname());
            updateUser.setPaypayFlag(request.getPaypayFlag() != null ? request.getPaypayFlag().byteValue() : 0);
            updateUser.setBankFlag(request.getBankFlag() != null ? request.getBankFlag().byteValue() : 0);
            updateUser.setProfileCompleted(true);
            userMapper.updateByPrimaryKeySelective(updateUser);
        }

        // 返回登录信息
        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setNickname(request.getNickname().trim());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        return response;
    }

    /**
     * 关联已有账号（Google邮箱和已有账号邮箱相同）
     */
    @PostMapping("/link-account")
    public LoginResponse linkAccount(@RequestBody LinkAccountRequest request) {

        // 必须是 google-link-pending 状态
        StpUtil.checkLogin();
        if (!"google-link-pending".equals(StpUtil.getLoginDevice())) {
            throw new RuntimeException("当前不是 Google 绑定流程");
        }

        Integer userId = StpUtil.getLoginIdAsInt();

        // 从 session 取 Google 信息
        String googleId = (String) StpUtil.getSession().get("googleId");
        String googleEmail = (String) StpUtil.getSession().get("googleEmail");

        if (googleId == null || googleEmail == null) {
            throw new RuntimeException("登录信息已过期，请重新登录");
        }

        // 查找用户
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        // 验证密码 - 不透露具体错误
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 绑定 Google ID
        User update = new User();
        update.setId(userId);
        update.setGoogleId(googleId);
        update.setProfileCompleted(true);
        userMapper.updateByPrimaryKeySelective(update);

        // 清除临时登录态
        StpUtil.logout();

        // 正式登录
        StpUtil.login(userId, "google-sso");

        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setNickname(user.getNickname());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        return response;
    }

    /**
     * Google 注册新账号 - 确认进入注册流程（不创建用户）
     */
    @PostMapping("/register-with-google")
    public LoginResponse registerWithGoogle() {

        // 必须是 google-temp 状态
        StpUtil.checkLogin();
        if (!"google-temp".equals(StpUtil.getLoginDevice())) {
            throw new RuntimeException("当前不是 Google 注册流程");
        }

        // 从 session 取 Google 信息（只是确认信息存在，不创建用户）
        String googleId = (String) StpUtil.getSession().get("googleId");
        String googleEmail = (String) StpUtil.getSession().get("googleEmail");

        if (googleId == null || googleEmail == null) {
            throw new RuntimeException("Google 登录信息缺失");
        }

        // 不创建用户，只返回确认信息
        // 用户数据将在 complete-profile 时才创建
        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue()); // 保持当前临时登录
        response.setNickname("");
        response.setEmail(googleEmail);
        response.setRole("user");

        return response;
    }
    
    /**
     * 绑定已有账号（Google邮箱和已有账号邮箱不同）
     */
    @PostMapping("/link-existing-account")
    public LoginResponse linkExistingAccount(@RequestBody LinkExistingAccountRequest request) {

        // 必须是 google-temp 状态（邮箱不匹配时的临时登录）
        StpUtil.checkLogin();
        if (!"google-temp".equals(StpUtil.getLoginDevice())) {
            throw new RuntimeException("当前不是 Google 绑定流程");
        }

        // 从 session 获取 Google 信息
        String googleId = (String) StpUtil.getSession().get("googleId");
        String googleEmail = (String) StpUtil.getSession().get("googleEmail");

        if (googleId == null || googleEmail == null) {
            throw new RuntimeException("登录信息已过期，请重新登录");
        }

        // 1. 用户输入的邮箱查找账号
        UserExample example = new UserExample();
        example.createCriteria().andEmailEqualTo(request.getEmail());
        List<User> users = userMapper.selectByExample(example);

        // 合并错误信息，防止暴力破解
        if (users.isEmpty() || !passwordEncoder.matches(request.getPassword(), users.get(0).getPassword())) {
            throw new RuntimeException("邮箱或密码错误");
        }

        User existingUser = users.get(0);

        // 3. 检查是否已绑定 Google
        if (existingUser.getGoogleId() != null && !existingUser.getGoogleId().isEmpty()) {
            throw new RuntimeException("该账号已绑定其他 Google 账号");
        }

        // 4. 绑定 Google ID
        User update = new User();
        update.setId(existingUser.getId());
        update.setGoogleId(googleId);
        update.setProfileCompleted(true); // 绑定后视为资料已完善
        userMapper.updateByPrimaryKeySelective(update);

        // 5. 清除临时登录态
        StpUtil.logout();

        // 6. 正式登录
        StpUtil.login(existingUser.getId(), "google-sso");

        // 7. 返回登录信息
        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setNickname(existingUser.getNickname());
        response.setEmail(existingUser.getEmail());
        response.setRole(existingUser.getRole());

        return response;
    }    

    /**
     * 检查用户资料是否完善
     */
    @GetMapping("/check-profile")
    public Map<String, Object> checkProfile() {
        StpUtil.checkLogin();

        String loginDevice = StpUtil.getLoginDevice();
        Map<String, Object> result = new HashMap<>();

        // 如果是临时登录（google-temp），需要完善资料
        if ("google-temp".equals(loginDevice)) {
            String googleEmail = (String) StpUtil.getSession().get("googleEmail");
            result.put("needsCompletion", true);
            result.put("email", googleEmail);
            return result;
        }

        // 正常用户登录
        try {
            Integer userId = StpUtil.getLoginIdAsInt();
            User user = userMapper.selectByPrimaryKey(userId);

            if (user == null) {
                result.put("needsCompletion", true);
                result.put("email", "");
                return result;
            }

            result.put("needsCompletion", user.getProfileCompleted() == null || !user.getProfileCompleted());
            result.put("email", user.getEmail());
        } catch (Exception e) {
            // 如果无法解析用户ID，说明是临时登录状态
            result.put("needsCompletion", true);
            result.put("email", "");
        }

        return result;
    }
}