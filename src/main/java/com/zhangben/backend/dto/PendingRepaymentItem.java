package com.zhangben.backend.dto;

import java.time.LocalDateTime;

/**
 * V30: 待确认的还款项
 */
public class PendingRepaymentItem {

    private Integer repaymentId;
    private Integer debtorId;
    private String debtorName;
    private String debtorAvatarUrl;
    private Long amount;
    private String comment;
    private LocalDateTime payDatetime;

    public Integer getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(Integer repaymentId) {
        this.repaymentId = repaymentId;
    }

    public Integer getDebtorId() {
        return debtorId;
    }

    public void setDebtorId(Integer debtorId) {
        this.debtorId = debtorId;
    }

    public String getDebtorName() {
        return debtorName;
    }

    public void setDebtorName(String debtorName) {
        this.debtorName = debtorName;
    }

    public String getDebtorAvatarUrl() {
        return debtorAvatarUrl;
    }

    public void setDebtorAvatarUrl(String debtorAvatarUrl) {
        this.debtorAvatarUrl = debtorAvatarUrl;
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
