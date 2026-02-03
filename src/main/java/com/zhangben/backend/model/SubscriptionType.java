package com.zhangben.backend.model;

/**
 * V42: 订阅周期枚举
 */
public enum SubscriptionType {
    MONTHLY("月付", 30),
    YEARLY("年付", 365),
    PERMANENT("永久", -1);

    private final String displayName;
    private final int durationDays;

    SubscriptionType(String displayName, int durationDays) {
        this.displayName = displayName;
        this.durationDays = durationDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isPermanent() {
        return this == PERMANENT;
    }
}
