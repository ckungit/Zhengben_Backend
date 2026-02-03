package com.zhangben.backend.controller;

import com.zhangben.backend.config.FeatureConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * V42: 功能开关 API
 * 返回当前启用的功能列表，供前端判断是否显示相关 UI
 */
@RestController
@RequestMapping("/api/features")
public class FeatureController {

    @Autowired
    private FeatureConfig featureConfig;

    /**
     * 获取功能开关状态
     * 无需登录即可访问
     */
    @GetMapping
    public Map<String, Boolean> getFeatures() {
        Map<String, Boolean> features = new HashMap<>();
        features.put("subscription", featureConfig.isSubscriptionEnabled());
        return features;
    }
}
