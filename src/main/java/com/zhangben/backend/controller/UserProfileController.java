package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @PostMapping("/password")
    public String updatePassword(@RequestParam String oldPwd,
                                 @RequestParam String newPwd) {

        Integer userId = StpUtil.getLoginIdAsInt();
        userProfileService.updatePassword(userId, oldPwd, newPwd);
        return "OK";
    }

    @PostMapping("/name")
    public String updateName(@RequestParam String firstname,
                             @RequestParam String secondname) {

        Integer userId = StpUtil.getLoginIdAsInt();
        userProfileService.updateName(userId, firstname, secondname);
        return "OK";
    }

    @PostMapping("/nickname")
    public String updateNickname(@RequestParam String nickname) {

        Integer userId = StpUtil.getLoginIdAsInt();
        userProfileService.updateNickname(userId, nickname);
        return "OK";
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

        return "OK";
    }
}