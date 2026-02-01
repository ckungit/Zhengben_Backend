package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.UserProfileResponse;
import com.zhangben.backend.model.User;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.service.R2StorageService;
import com.zhangben.backend.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
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

    @Value("${avatar.max-file-size:51200}")
    private long maxFileSize;

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
}
