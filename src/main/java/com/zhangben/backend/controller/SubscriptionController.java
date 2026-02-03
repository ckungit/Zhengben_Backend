package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.config.FeatureConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V42: 订阅控制器
 * 处理会员订阅相关的 API 请求
 * 注意：支付功能为占位接口，实际支付逻辑待实现
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private FeatureConfig featureConfig;

    /**
     * 获取订阅计划列表和价格
     */
    @GetMapping("/plans")
    public ResponseEntity<?> getSubscriptionPlans() {
        if (!featureConfig.isSubscriptionEnabled()) {
            return ResponseEntity.ok(Map.of("enabled", false, "plans", List.of()));
        }

        // 价格配置（单位：分）
        var normalPlan = Map.of(
            "tier", "NORMAL",
            "name", "普通会员",
            "pricing", Map.of(
                "MONTHLY", 980,
                "YEARLY", 9800
            ),
            "features", List.of("maxFriends:50", "maxActivities:20", "exportData", "advancedStats", "noAds")
        );

        // 注意：永久会员(PERMANENT)只能由管理员设置，不对外销售
        var proPlan = Map.of(
            "tier", "PRO",
            "name", "专业会员",
            "pricing", Map.of(
                "MONTHLY", 1980,
                "YEARLY", 19800
            ),
            "features", List.of("unlimited", "exportData", "advancedStats", "prioritySupport", "customCategories", "noAds")
        );

        return ResponseEntity.ok(Map.of(
            "enabled", true,
            "plans", List.of(normalPlan, proPlan),
            "currency", "CNY"
        ));
    }

    /**
     * 创建订阅订单（占位接口）
     * 实际支付功能待实现
     */
    @PostMapping("/orders")
    public ResponseEntity<?> createSubscriptionOrder(@RequestBody Map<String, Object> request) {
        if (!featureConfig.isSubscriptionEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "订阅功能未启用"));
        }

        Integer userId = StpUtil.getLoginIdAsInt();
        String tier = (String) request.get("tier");
        String period = (String) request.get("period");
        String paymentMethod = (String) request.get("paymentMethod");

        // 验证参数
        if (tier == null || period == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数"));
        }

        if (!List.of("NORMAL", "PRO").contains(tier)) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的会员等级"));
        }

        // 注意：永久会员(PERMANENT)只能由管理员设置，用户无法自行购买
        if (!List.of("MONTHLY", "YEARLY").contains(period)) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的订阅周期"));
        }

        // 占位响应：实际支付逻辑待实现
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "支付功能即将上线，敬请期待",
            "orderId", null,
            "paymentUrl", null
        ));
    }

    /**
     * 查询订单状态（占位接口）
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderStatus(@PathVariable String orderId) {
        if (!featureConfig.isSubscriptionEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "订阅功能未启用"));
        }

        // 占位响应
        return ResponseEntity.ok(Map.of(
            "orderId", orderId,
            "status", "PENDING",
            "message", "支付功能即将上线"
        ));
    }

    /**
     * 取消自动续费（占位接口）
     */
    @PostMapping("/cancel-auto-renew")
    public ResponseEntity<?> cancelAutoRenew() {
        if (!featureConfig.isSubscriptionEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "订阅功能未启用"));
        }

        Integer userId = StpUtil.getLoginIdAsInt();

        // 占位响应
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "已取消自动续费"
        ));
    }

    /**
     * 获取订阅历史（占位接口）
     */
    @GetMapping("/history")
    public ResponseEntity<?> getSubscriptionHistory() {
        if (!featureConfig.isSubscriptionEnabled()) {
            return ResponseEntity.ok(Map.of("history", List.of()));
        }

        Integer userId = StpUtil.getLoginIdAsInt();

        // 占位响应：从 subscription_history 表查询
        // 实际实现待完成
        return ResponseEntity.ok(Map.of(
            "history", List.of()
        ));
    }

    /**
     * 获取可用的支付方式（占位接口）
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<?> getPaymentMethods() {
        if (!featureConfig.isSubscriptionEnabled()) {
            return ResponseEntity.ok(Map.of("methods", List.of()));
        }

        // 占位响应：支付方式待实现
        return ResponseEntity.ok(Map.of(
            "methods", List.of(
                Map.of("code", "ALIPAY", "name", "支付宝", "enabled", false, "comingSoon", true),
                Map.of("code", "WECHAT", "name", "微信支付", "enabled", false, "comingSoon", true),
                Map.of("code", "CARD", "name", "银行卡", "enabled", false, "comingSoon", true)
            )
        ));
    }
}
