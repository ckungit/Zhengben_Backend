package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.UserPaymentMethodDto;
import com.zhangben.backend.dto.UserSearchResult;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.FavoredUserService;
import com.zhangben.backend.service.UserPaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favored")
public class FavoredUserController {

    @Autowired
    private FavoredUserService favoredUserService;

    @Autowired
    private UserPaymentMethodService userPaymentMethodService;

    @PostMapping("/add")
    public String addFavored(@RequestParam Integer favoredUserId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        favoredUserService.addFavored(userId, favoredUserId);
        return "OK";
    }

    @PostMapping("/delete")
    public String deleteFavored(@RequestParam Integer favoredUserId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        favoredUserService.deleteFavored(userId, favoredUserId);
        return "OK";
    }

    @GetMapping("/list")
    public List<UserSearchResult> listFavored() {
        Integer userId = StpUtil.getLoginIdAsInt();
        List<User> users = favoredUserService.listFavored(userId);
        return enrichUsersWithPaymentInfo(users);
    }

    /**
     * V39: 将用户列表转换为包含支付方式和货币信息的 DTO
     */
    private List<UserSearchResult> enrichUsersWithPaymentInfo(List<User> users) {
        List<UserSearchResult> results = new ArrayList<>();

        for (User u : users) {
            UserSearchResult item = new UserSearchResult();
            item.setId(u.getId());
            item.setNickname(u.getNickname());
            item.setEmail(u.getEmail());
            item.setFullName(buildFullName(u.getSecondname(), u.getFirstname()));
            item.setAvatarUrl(u.getAvatarUrl());

            // 旧字段（保持兼容）
            item.setPaypaySupported(u.getPaypayFlag() != null && u.getPaypayFlag() == 1);
            item.setBankSupported(u.getBankFlag() != null && u.getBankFlag() == 1);

            // V39: 新字段 - 主要货币
            item.setPrimaryCurrency(u.getPrimaryCurrency());

            // V39: 新字段 - 支付方式列表
            List<UserPaymentMethodDto> enabledMethods = userPaymentMethodService.getEnabledMethods(u.getId());
            List<String> methodCodes = enabledMethods.stream()
                    .map(UserPaymentMethodDto::getMethodCode)
                    .collect(Collectors.toList());
            item.setPaymentMethods(methodCodes);

            results.add(item);
        }

        return results;
    }

    private String buildFullName(String secondname, String firstname) {
        if (secondname == null && firstname == null) {
            return "";
        }
        return (secondname != null ? secondname : "") + (firstname != null ? firstname : "");
    }

    /**
     * V25: 获取待处理的好友请求（谁加了我但我没加他们）
     */
    @GetMapping("/pending")
    public List<UserSearchResult> getPendingRequests() {
        Integer userId = StpUtil.getLoginIdAsInt();
        List<User> users = favoredUserService.getPendingFriendRequests(userId);
        return enrichUsersWithPaymentInfo(users);
    }
}