package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.UserSearchResult;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.service.AccountDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AccountDeletionService accountDeletionService;

    /**
     * 搜索用户（通过昵称或邮箱）
     * @param keyword 搜索关键词
     */
    @GetMapping("/search")
    public List<UserSearchResult> searchUsers(@RequestParam String keyword) {
        StpUtil.checkLogin();
        Integer currentUserId = StpUtil.getLoginIdAsInt();

        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchKey = "%" + keyword.trim() + "%";

        UserExample example = new UserExample();
        example.or().andNicknameLike(searchKey);
        example.or().andEmailLike(searchKey);
        example.or().andFirstnameLike(searchKey);
        example.or().andSecondnameLike(searchKey);

        List<User> users = userMapper.selectByExample(example);

        List<UserSearchResult> result = new ArrayList<>();
        for (User u : users) {
            // 排除自己
            if (u.getId().equals(currentUserId)) {
                continue;
            }

            UserSearchResult item = new UserSearchResult();
            item.setId(u.getId());
            item.setNickname(u.getNickname());
            item.setEmail(u.getEmail());
            item.setFullName(buildFullName(u.getSecondname(), u.getFirstname()));
            item.setAvatarUrl(u.getAvatarUrl());
            item.setPaypaySupported(u.getPaypayFlag() != null && u.getPaypayFlag() == 1);
            item.setBankSupported(u.getBankFlag() != null && u.getBankFlag() == 1);
            result.add(item);
        }

        return result;
    }

    /**
     * 获取所有用户（用于选择参与者）
     */
    @GetMapping("/all")
    public List<UserSearchResult> getAllUsers() {
        StpUtil.checkLogin();
        Integer currentUserId = StpUtil.getLoginIdAsInt();

        UserExample example = new UserExample();
        example.setOrderByClause("nickname ASC");
        List<User> users = userMapper.selectByExample(example);

        List<UserSearchResult> result = new ArrayList<>();
        for (User u : users) {
            // 排除自己
            if (u.getId().equals(currentUserId)) {
                continue;
            }

            UserSearchResult item = new UserSearchResult();
            item.setId(u.getId());
            item.setNickname(u.getNickname());
            item.setEmail(u.getEmail());
            item.setFullName(buildFullName(u.getSecondname(), u.getFirstname()));
            item.setAvatarUrl(u.getAvatarUrl());
            item.setPaypaySupported(u.getPaypayFlag() != null && u.getPaypayFlag() == 1);
            item.setBankSupported(u.getBankFlag() != null && u.getBankFlag() == 1);
            result.add(item);
        }

        return result;
    }

    private String buildFullName(String secondname, String firstname) {
        if (secondname == null && firstname == null) {
            return "";
        }
        return (secondname != null ? secondname : "") + (firstname != null ? firstname : "");
    }

    /**
     * V40: GDPR Compliant Account Deletion
     * Permanently deletes user account and all associated data
     * Implements "Right to be Forgotten"
     */
    @DeleteMapping("/account/delete")
    public ResponseEntity<String> deleteAccount() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        logger.info("User {} requested account deletion (GDPR)", userId);

        try {
            accountDeletionService.deleteAccountPermanently(userId);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            logger.error("Account deletion failed for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body("Account deletion failed: " + e.getMessage());
        }
    }
}
