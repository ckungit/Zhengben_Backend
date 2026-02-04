package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.zhangben.backend.config.FeatureConfig;
import com.zhangben.backend.dto.SubscriptionInfoResponse;
import com.zhangben.backend.dto.UserProfileResponse;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.service.R2StorageService;
import com.zhangben.backend.service.SubscriptionService;
import com.zhangben.backend.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.zhangben.backend.util.CurrencyUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private R2StorageService r2StorageService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private FeatureConfig featureConfig;

    @Value("${avatar.max-file-size:51200}")
    private long maxFileSize;

    @Value("${google.client-id:}")
    private String googleClientId;

    /**
     * 获取当前用户详细信息
     */
    @GetMapping("/detail")
    public UserProfileResponse getProfile() {
        Integer userId = StpUtil.getLoginIdAsInt();
        User user = userMapper.selectByPrimaryKey(userId);

        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setNickname(user.getNickname());
        resp.setRole(user.getRole());
        resp.setFirstname(user.getFirstname());
        resp.setSecondname(user.getSecondname());
        resp.setPaypayFlag(user.getPaypayFlag());
        resp.setPaypayAccount(user.getPaypayAccount());
        resp.setBankFlag(user.getBankFlag());
        resp.setBankName(user.getBankName());
        resp.setBankBranch(user.getBankBranch());
        resp.setBankAccount(user.getBankAccount());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setPreferredLanguage(user.getPreferredLanguage());
        resp.setPrimaryCurrency(user.getPrimaryCurrency() != null ? user.getPrimaryCurrency() : "JPY");
        // V43: Google/Microsoft 绑定状态
        resp.setGoogleId(user.getGoogleId());
        resp.setMicrosoftId(user.getMicrosoftId());

        return resp;
    }

    @PostMapping("/password")
    public String updatePassword(@RequestParam String oldPwd,
                                 @RequestParam String newPwd) {

        Integer userId = StpUtil.getLoginIdAsInt();
        userProfileService.updatePassword(userId, oldPwd, newPwd);
        return "密码修改成功";
    }

    @PostMapping("/name")
    public String updateName(@RequestParam String firstname,
                             @RequestParam String secondname) {

        Integer userId = StpUtil.getLoginIdAsInt();
        userProfileService.updateName(userId, firstname, secondname);
        return "姓名修改成功";
    }

    @PostMapping("/nickname")
    public String updateNickname(@RequestParam String nickname) {

        Integer userId = StpUtil.getLoginIdAsInt();
        userProfileService.updateNickname(userId, nickname);
        return "昵称修改成功";
    }

    @PostMapping("/pay-method")
    public String updatePayMethod(@RequestParam Byte paypayFlag,
                                  @RequestParam(required = false) String paypayAccount,
                                  @RequestParam Byte bankFlag,
                                  @RequestParam(required = false) String bankName,
                                  @RequestParam(required = false) String bankBranch,
                                  @RequestParam(required = false) String bankAccount) {

        Integer userId = StpUtil.getLoginIdAsInt();
        userProfileService.updatePayMethods(userId, paypayFlag, paypayAccount,
                bankFlag, bankName, bankBranch, bankAccount);

        return "收款方式修改成功";
    }

    /**
     * V26: 更新语言偏好
     */
    @PostMapping("/language")
    public String updateLanguage(@RequestParam String language) {
        Integer userId = StpUtil.getLoginIdAsInt();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPreferredLanguage(language);
        updateUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(updateUser);

        return "语言设置已更新";
    }

    /**
     * V39: 更新主要货币
     */
    @PostMapping("/currency")
    public ResponseEntity<?> updateCurrency(@RequestParam String currency) {
        Integer userId = StpUtil.getLoginIdAsInt();

        // 验证货币代码
        if (!CurrencyUtils.isValidCurrency(currency)) {
            return ResponseEntity.badRequest().body("不支持的货币类型");
        }

        // 更新数据库
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPrimaryCurrency(currency.toUpperCase());
        updateUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(updateUser);

        // 同步更新 Session
        CurrencyUtils.updateSessionCurrency(currency.toUpperCase());

        return ResponseEntity.ok(Map.of(
            "message", "货币设置已更新",
            "currency", currency.toUpperCase()
        ));
    }

    /**
     * V24: 上传头像
     * 前端需要在上传前压缩图片到 50KB 以下
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Integer userId = StpUtil.getLoginIdAsInt();

        // 验证文件
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择文件");
        }

        // 验证文件大小（服务端双重验证）
        if (file.getSize() > maxFileSize) {
            return ResponseEntity.badRequest().body("文件大小超过限制（最大 " + (maxFileSize / 1024) + "KB），请压缩后重试");
        }

        try {
            // 获取当前用户，用于删除旧头像
            User user = userMapper.selectByPrimaryKey(userId);
            String oldAvatarUrl = user.getAvatarUrl();

            // 流式上传到 R2
            String avatarUrl = r2StorageService.uploadAvatar(
                    file.getInputStream(),
                    file.getContentType(),
                    file.getSize(),
                    userId
            );

            // 更新数据库
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setAvatarUrl(avatarUrl);
            updateUser.setUpdatedAt(LocalDateTime.now());
            userMapper.updateByPrimaryKeySelective(updateUser);

            // 异步删除旧头像（不阻塞响应）
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                new Thread(() -> r2StorageService.deleteAvatar(oldAvatarUrl)).start();
            }

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("上传失败，请重试");
        }
    }

    /**
     * V24: 删除头像
     */
    @DeleteMapping("/avatar")
    public String deleteAvatar() {
        Integer userId = StpUtil.getLoginIdAsInt();
        User user = userMapper.selectByPrimaryKey(userId);

        if (user.getAvatarUrl() != null) {
            // 删除 R2 上的文件
            r2StorageService.deleteAvatar(user.getAvatarUrl());

            // 清空数据库中的头像URL
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setAvatarUrl("");
            updateUser.setUpdatedAt(LocalDateTime.now());
            userMapper.updateByPrimaryKeySelective(updateUser);
        }

        return "头像已删除";
    }

    /**
     * V42: 获取当前用户订阅信息
     */
    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscriptionInfo() {
        // 功能未启用时返回默认免费用户信息
        if (!featureConfig.isSubscriptionEnabled()) {
            return ResponseEntity.ok(createDefaultSubscriptionResponse());
        }

        try {
            Integer userId = StpUtil.getLoginIdAsInt();
            SubscriptionInfoResponse info = subscriptionService.getSubscriptionInfo(userId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            // 数据库字段可能不存在，返回默认值
            return ResponseEntity.ok(createDefaultSubscriptionResponse());
        }
    }

    private SubscriptionInfoResponse createDefaultSubscriptionResponse() {
        SubscriptionInfoResponse defaultResponse = new SubscriptionInfoResponse();
        defaultResponse.setTier("FREE");
        defaultResponse.setType(null);
        defaultResponse.setPermanent(false);
        defaultResponse.setInRenewalWindow(false);
        defaultResponse.setTierDisplayName("Free");
        defaultResponse.setTypeDisplayName(null);
        defaultResponse.setDaysUntilExpiry(null);
        defaultResponse.setAutoRenew(false);
        return defaultResponse;
    }

    /**
     * V43: 绑定 Google 账号
     */
    @PostMapping("/google/bind")
    public ResponseEntity<?> bindGoogleAccount(@RequestBody Map<String, String> request) {
        if (googleClientId == null || googleClientId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Google SSO 未配置"));
        }

        Integer userId = StpUtil.getLoginIdAsInt();
        User currentUser = userMapper.selectByPrimaryKey(userId);

        // 检查是否已绑定
        if (currentUser.getGoogleId() != null && !currentUser.getGoogleId().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "已绑定 Google 账号"));
        }

        String credential = request.get("credential");
        if (credential == null || credential.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "凭证无效"));
        }

        try {
            // 验证 Google ID Token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "无效的 Google 凭证"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();

            // 检查该 Google 账号是否已被其他用户绑定
            UserExample googleIdExample = new UserExample();
            googleIdExample.createCriteria().andGoogleIdEqualTo(googleId);
            List<User> existingUsers = userMapper.selectByExample(googleIdExample);

            if (!existingUsers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "该 Google 账号已被其他用户绑定"));
            }

            // 绑定
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setGoogleId(googleId);
            updateUser.setUpdatedAt(LocalDateTime.now());
            userMapper.updateByPrimaryKeySelective(updateUser);

            return ResponseEntity.ok(Map.of("message", "绑定成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "绑定失败：" + e.getMessage()));
        }
    }

    /**
     * V43: 绑定 Microsoft 账号
     */
    @PostMapping("/microsoft/bind")
    public ResponseEntity<?> bindMicrosoftAccount(@RequestBody Map<String, String> request) {
        Integer userId = StpUtil.getLoginIdAsInt();
        User currentUser = userMapper.selectByPrimaryKey(userId);

        if (currentUser.getMicrosoftId() != null && !currentUser.getMicrosoftId().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "已绑定 Microsoft 账号"));
        }

        String microsoftId = request.get("microsoftId");
        if (microsoftId == null || microsoftId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "缺少 Microsoft ID"));
        }

        // 检查该 Microsoft 账号是否已被其他用户绑定
        User existingUser = userMapper.selectByMicrosoftId(microsoftId);
        if (existingUser != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "该 Microsoft 账号已被其他用户绑定"));
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setMicrosoftId(microsoftId);
        updateUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(updateUser);

        return ResponseEntity.ok(Map.of("message", "绑定成功"));
    }

    /**
     * V43: 解绑 Microsoft 账号
     */
    @PostMapping("/microsoft/unbind")
    public ResponseEntity<?> unbindMicrosoftAccount() {
        Integer userId = StpUtil.getLoginIdAsInt();
        User currentUser = userMapper.selectByPrimaryKey(userId);

        if (currentUser.getMicrosoftId() == null || currentUser.getMicrosoftId().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "尚未绑定 Microsoft 账号"));
        }

        if ((currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) &&
            (currentUser.getGoogleId() == null || currentUser.getGoogleId().isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("message", "请先设置密码或绑定其他登录方式后再解绑"));
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setMicrosoftId("");
        updateUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(updateUser);

        return ResponseEntity.ok(Map.of("message", "解绑成功"));
    }

    /**
     * V43: 解绑 Google 账号
     */
    @PostMapping("/google/unbind")
    public ResponseEntity<?> unbindGoogleAccount() {
        Integer userId = StpUtil.getLoginIdAsInt();
        User currentUser = userMapper.selectByPrimaryKey(userId);

        // 检查是否已绑定
        if (currentUser.getGoogleId() == null || currentUser.getGoogleId().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "尚未绑定 Google 账号"));
        }

        // 检查是否有密码（防止解绑后无法登录）
        if (currentUser.getPassword() == null || currentUser.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请先设置密码后再解绑 Google 账号"));
        }

        // 解绑
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setGoogleId("");
        updateUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(updateUser);

        return ResponseEntity.ok(Map.of("message", "解绑成功"));
    }
}
