package com.zhangben.backend.model;

import java.time.LocalDateTime;

public class ActivityEvent {
    private Integer id;
    private Integer activityId;
    private Integer userId;
    private String eventType;
    private String userCurrency;
    private String extraInfo;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getActivityId() { return activityId; }
    public void setActivityId(Integer activityId) { this.activityId = activityId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getUserCurrency() { return userCurrency; }
    public void setUserCurrency(String userCurrency) { this.userCurrency = userCurrency; }

    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
