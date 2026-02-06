package com.zhangben.backend.dto;

import java.time.LocalDateTime;

public class DebtorDebtDetailItem {

    private Integer outcomeId;
    private String categoryName;
    private String comment;
    private Long amount;
    private LocalDateTime payDatetime;
    private String locationText;
    private Boolean isOffset; // 是否是抵消记录

    public Integer getOutcomeId() {
        return outcomeId;
    }

    public void setOutcomeId(Integer outcomeId) {
        this.outcomeId = outcomeId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public LocalDateTime getPayDatetime() {
        return payDatetime;
    }

    public void setPayDatetime(LocalDateTime payDatetime) {
        this.payDatetime = payDatetime;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public Boolean getIsOffset() {
        return isOffset;
    }

    public void setIsOffset(Boolean isOffset) {
        this.isOffset = isOffset;
    }
}