package com.zhangben.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * V42: 添加 Bean Validation 注解
 */
public class RepayRequest {

    @NotNull(message = "债权人ID不能为空")
    private Integer creditorId;

    @NotNull(message = "还款金额不能为空")
    @Min(value = 1, message = "还款金额必须大于0")
    private Long amount;

    private Integer styleId;

    private String comment;

    // V20: 还款时间（可选，不传则用当前时间）
    private LocalDateTime payDatetime;

    // 债权人录入时指定债务人
    private Integer debtorId;

    // 跳过冲突检测（用户选择"仍然提交"）
    private Boolean skipConflictCheck;

    // V49: 活动还款时指定活动ID
    private Integer activityId;

    public Integer getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(Integer creditorId) {
        this.creditorId = creditorId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getStyleId() {
        return styleId;
    }

    public void setStyleId(Integer styleId) {
        this.styleId = styleId;
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

    public Integer getDebtorId() {
        return debtorId;
    }

    public void setDebtorId(Integer debtorId) {
        this.debtorId = debtorId;
    }

    public Boolean getSkipConflictCheck() {
        return skipConflictCheck;
    }

    public void setSkipConflictCheck(Boolean skipConflictCheck) {
        this.skipConflictCheck = skipConflictCheck;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }
}
