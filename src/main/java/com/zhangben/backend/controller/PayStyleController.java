package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.model.PayStyle;
import com.zhangben.backend.model.PayStyleExample;
import com.zhangben.backend.mapper.PayStyleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pay-style")
public class PayStyleController {

    @Autowired
    private PayStyleMapper payStyleMapper;

    /**
     * 获取所有支付分类
     */
    @GetMapping("/list")
    public List<PayStyle> getAllStyles() {
        StpUtil.checkLogin();
        PayStyleExample example = new PayStyleExample();
        example.setOrderByClause("id ASC");
        return payStyleMapper.selectByExample(example);
    }
}
