package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.UserPaymentMethodDto;
import com.zhangben.backend.service.UserPaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V38: 用户支付方式管理 API
 */
@RestController
@RequestMapping("/api/user/payment-methods")
public class UserPaymentMethodController {

    @Autowired
    private UserPaymentMethodService paymentMethodService;

    /**
     * 获取用户已启用的支付方式
     */
    @GetMapping
    public List<UserPaymentMethodDto> getEnabledMethods() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return paymentMethodService.getEnabledMethods(userId);
    }

    /**
     * 获取用户所有支付方式配置
     */
    @GetMapping("/all")
    public List<UserPaymentMethodDto> getAllMethods() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return paymentMethodService.getAllMethods(userId);
    }

    /**
     * 批量更新支付方式
     */
    @PutMapping("/batch")
    public String batchUpdate(@RequestBody List<UserPaymentMethodDto> methods) {
        Integer userId = StpUtil.getLoginIdAsInt();
        paymentMethodService.batchUpdateMethods(userId, methods);
        return "更新成功";
    }

    /**
     * 更新单个支付方式的启用状态
     */
    @PatchMapping("/{methodCode}")
    public String updateStatus(
            @PathVariable String methodCode,
            @RequestBody Map<String, Boolean> body) {
        Integer userId = StpUtil.getLoginIdAsInt();
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("enabled 参数必须提供");
        }
        paymentMethodService.updateMethodStatus(userId, methodCode, enabled);
        return "更新成功";
    }

    /**
     * 更新支付方式详细配置
     */
    @PutMapping("/{methodCode}/config")
    public String updateConfig(
            @PathVariable String methodCode,
            @RequestBody String configJson) {
        Integer userId = StpUtil.getLoginIdAsInt();
        paymentMethodService.updateMethodConfig(userId, methodCode, configJson);
        return "配置更新成功";
    }

    /**
     * 获取指定用户的公开支付方式（用于还款时显示）
     */
    @GetMapping("/user/{userId}")
    public List<UserPaymentMethodDto> getUserPublicMethods(@PathVariable Integer userId) {
        // 只返回已启用的支付方式
        return paymentMethodService.getEnabledMethods(userId);
    }
}
