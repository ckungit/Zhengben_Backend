package com.zhangben.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V49: Locked exchange rate within an activity.
 * Once a member joins with a new currency, the rate is locked permanently.
 * locked_rate = cross rate from activity's base_currency to this currency_code.
 */
public class ActivityRateSnapshot {

    private Integer id;
    private Integer activityId;
    private String currencyCode;
    private BigDecimal lockedRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ActivityRateSnapshot() {}

    public ActivityRateSnapshot(Integer activityId, String currencyCode, BigDecimal lockedRate) {
        this.activityId = activityId;
        this.currencyCode = currencyCode;
        this.lockedRate = lockedRate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getLockedRate() {
        return lockedRate;
    }

    public void setLockedRate(BigDecimal lockedRate) {
        this.lockedRate = lockedRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
