package com.zhangben.backend.dto;

import java.time.LocalDateTime;

public class CreditorDebtDetailItem {

    private Integer outcomeId;
    private String categoryName;
    private String comment;
    private Long amount;
    private LocalDateTime payDatetime;
    private String locationText;
    private Boolean isRepayment; // 是否是还款记录
    private Integer confirmStatus; // 确认状态: 0=待确认, 1=已确认, null=非还款记录
    private Boolean isOffset; // 是否是抵消记录
    private String currency; // V47: 金额所在币种
    private Long originalAmount; // V47: 原始币种金额 (cents)
    private String originalCurrency; // V47: 原始交易币种

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

    public Boolean getIsRepayment() {
        return isRepayment;
    }

    public void setIsRepayment(Boolean isRepayment) {
        this.isRepayment = isRepayment;
    }

    public Integer getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(Integer confirmStatus) {
        this.confirmStatus = confirmStatus;
    }

    public Boolean getIsOffset() {
        return isOffset;
    }

    public void setIsOffset(Boolean isOffset) {
        this.isOffset = isOffset;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Long originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }
}