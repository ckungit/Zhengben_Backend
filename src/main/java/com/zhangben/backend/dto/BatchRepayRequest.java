package com.zhangben.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * V35: 批量还款请求（支持代人还款）
 */
public class BatchRepayRequest {

    /**
     * 债权人ID（收款人）
     */
    private Integer creditorId;

    /**
     * 还款明细列表
     */
    private List<RepaymentItem> items;

    /**
     * 备注（可选）
     */
    private String comment;

    /**
     * 还款时间（可选，不传则用当前时间）
     */
    private LocalDateTime payDatetime;

    /**
     * V49: 活动还款时指定活动ID
     */
    private Integer activityId;

    /**
     * 还款明细项
     */
    public static class RepaymentItem {
        /**
         * 被代还人ID（如果是自己还款，则为当前用户ID或null）
         */
        private Integer debtorId;

        /**
         * 还款金额（分）
         */
        private Long amount;

        public Integer getDebtorId() {
            return debtorId;
        }

        public void setDebtorId(Integer debtorId) {
            this.debtorId = debtorId;
        }

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }
    }

    // Getters and Setters

    public Integer getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(Integer creditorId) {
        this.creditorId = creditorId;
    }

    public List<RepaymentItem> getItems() {
        return items;
    }

    public void setItems(List<RepaymentItem> items) {
        this.items = items;
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

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    /**
     * 计算总金额
     */
    public Long getTotalAmount() {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        return items.stream()
                .mapToLong(item -> item.getAmount() != null ? item.getAmount() : 0L)
                .sum();
    }
}
