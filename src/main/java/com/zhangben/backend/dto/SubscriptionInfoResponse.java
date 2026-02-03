package com.zhangben.backend.dto;

import java.time.LocalDateTime;

/**
 * V42: 订阅信息响应 DTO
 */
public class SubscriptionInfoResponse {

    private String tier;           // FREE, NORMAL, PRO
    private String type;           // MONTHLY, YEARLY, PERMANENT
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private boolean autoRenew;
    private boolean isPermanent;
    private boolean isInRenewalWindow;
    private Integer daysUntilExpiry;

    // 显示用
    private String tierDisplayName;
    private String typeDisplayName;

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }

    public boolean isInRenewalWindow() {
        return isInRenewalWindow;
    }

    public void setInRenewalWindow(boolean inRenewalWindow) {
        isInRenewalWindow = inRenewalWindow;
    }

    public Integer getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(Integer daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public String getTierDisplayName() {
        return tierDisplayName;
    }

    public void setTierDisplayName(String tierDisplayName) {
        this.tierDisplayName = tierDisplayName;
    }

    public String getTypeDisplayName() {
        return typeDisplayName;
    }

    public void setTypeDisplayName(String typeDisplayName) {
        this.typeDisplayName = typeDisplayName;
    }
}
