package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.model.PayStyle;
import com.zhangben.backend.model.User;
import com.zhangben.backend.mapper.PayStyleMapper;
import com.zhangben.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pay-style")
public class PayStyleController {

    @Autowired
    private PayStyleMapper payStyleMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取所有支付分类（本地化）
     * 根据用户的语言偏好返回对应语言的分类名称
     */
    @GetMapping("/list")
    public List<PayStyle> getAllStyles() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        // 获取用户语言偏好，默认中文
        String language = "zh-CN";
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null && user.getPreferredLanguage() != null && !user.getPreferredLanguage().isEmpty()) {
            language = user.getPreferredLanguage();
        }

        return payStyleMapper.selectAllLocalized(language);
    }
}
