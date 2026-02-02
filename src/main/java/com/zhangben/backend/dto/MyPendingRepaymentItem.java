package com.zhangben.backend.dto;

import java.time.LocalDateTime;

/**
 * V32: 我发起的待确认还款（债务人视角）
 */
public class MyPendingRepaymentItem {

    private Integer repaymentId;
    private Integer creditorId;
    private String creditorName;
    private String creditorAvatarUrl;
    private Long amount;
    private String comment;
    private LocalDateTime payDatetime;

    public Integer getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(Integer repaymentId) {
        this.repaymentId = repaymentId;
    }

    public Integer getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(Integer creditorId) {
        this.creditorId = creditorId;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

    public String getCreditorAvatarUrl() {
        return creditorAvatarUrl;
    }

    public void setCreditorAvatarUrl(String creditorAvatarUrl) {
        this.creditorAvatarUrl = creditorAvatarUrl;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getPayDatetime() {
        return payDatetime;
    }

    public void setPayDatetime(LocalDateTime payDatetime) {
        this.payDatetime = payDatetime;
    }
}
