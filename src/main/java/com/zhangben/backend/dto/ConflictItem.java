package com.zhangben.backend.dto;

import java.time.LocalDateTime;

public class ConflictItem {

    private Integer repaymentId;
    private Long amount;
    private LocalDateTime payDatetime;
    private Integer creatorId;
    private String creatorName;

    public Integer getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(Integer repaymentId) {
        this.repaymentId = repaymentId;
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

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
}
