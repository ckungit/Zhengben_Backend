package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.UserProfileResponse;
import com.zhangben.backend.model.User;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserMapper userMapper;

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
}
