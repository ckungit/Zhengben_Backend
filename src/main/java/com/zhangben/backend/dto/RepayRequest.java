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
}
