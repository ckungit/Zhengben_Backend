package com.zhangben.backend.dto;

import java.time.LocalDateTime;

/**
 * V30: 待确认的还款项
 * V35: 增加代还信息字段
 */
public class PendingRepaymentItem {

    private Integer repaymentId;
    private Integer debtorId;
    private String debtorName;
    private String debtorAvatarUrl;
    private Long amount;
    private String comment;
    private LocalDateTime payDatetime;

    // V35: 代还信息
    private Integer repaidById;       // 实际付款人ID
    private String repaidByName;      // 实际付款人名称
    private Integer onBehalfOfId;     // 被代还人ID
    private String onBehalfOfName;    // 被代还人名称
    private Boolean isOnBehalf;       // 是否为代还

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

    public Integer getRepaidById() {
        return repaidById;
    }

    public void setRepaidById(Integer repaidById) {
        this.repaidById = repaidById;
    }

    public String getRepaidByName() {
        return repaidByName;
    }

    public void setRepaidByName(String repaidByName) {
        this.repaidByName = repaidByName;
    }

    public Integer getOnBehalfOfId() {
        return onBehalfOfId;
    }

    public void setOnBehalfOfId(Integer onBehalfOfId) {
        this.onBehalfOfId = onBehalfOfId;
    }

    public String getOnBehalfOfName() {
        return onBehalfOfName;
    }

    public void setOnBehalfOfName(String onBehalfOfName) {
        this.onBehalfOfName = onBehalfOfName;
    }

    public Boolean getIsOnBehalf() {
        return isOnBehalf;
    }

    public void setIsOnBehalf(Boolean isOnBehalf) {
        this.isOnBehalf = isOnBehalf;
    }
}
