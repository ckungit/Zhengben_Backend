package com.zhangben.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * V42: 功能开关配置
 * 集中管理各个功能模块的启用/禁用状态
 */
@Configuration
public class FeatureConfig {

    /**
     * 会员订阅功能开关
     * 设置为 false 时，禁用所有会员相关功能（升级、徽章、续费提醒等）
     * 默认开启，生产环境通过配置关闭
     */
    @Value("${subscription.enabled:true}")
    private boolean subscriptionEnabled;

    public boolean isSubscriptionEnabled() {
        return subscriptionEnabled;
    }
}
