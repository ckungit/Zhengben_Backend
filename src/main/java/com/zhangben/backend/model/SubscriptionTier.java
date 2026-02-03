package com.zhangben.backend.model;

/**
 * V42: 会员等级枚举
 */
public enum SubscriptionTier {
    FREE("免费用户"),
    NORMAL("普通会员"),
    PRO("专业会员");

    private final String displayName;

    SubscriptionTier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
