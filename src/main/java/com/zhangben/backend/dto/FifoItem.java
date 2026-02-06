package com.zhangben.backend.dto;

/**
 * FIFO 冲销项 — 描述单笔账单在本次还款中的冲销情况
 */
public class FifoItem {

    private Integer outcomeId;
    private Long originalAmount;   // 该笔账单应付 (perAmount * shares)
    private Long previouslyRepaid; // 此前已还
    private Long newlyRepaid;      // 本次冲销
    private String status;         // "COMPLETED" | "PARTIAL" | "UNCHANGED"
    private Double progress;       // 0.0 ~ 1.0
    private String comment;
    private String categoryName;
    private String payDatetime;

    public Integer getOutcomeId() {
        return outcomeId;
    }

    public void setOutcomeId(Integer outcomeId) {
        this.outcomeId = outcomeId;
    }

    public Long getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Long originalAmount) {
        this.originalAmount = originalAmount;
    }

    public Long getPreviouslyRepaid() {
        return previouslyRepaid;
    }

    public void setPreviouslyRepaid(Long previouslyRepaid) {
        this.previouslyRepaid = previouslyRepaid;
    }

    public Long getNewlyRepaid() {
        return newlyRepaid;
    }

    public void setNewlyRepaid(Long newlyRepaid) {
        this.newlyRepaid = newlyRepaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getPayDatetime() {
        return payDatetime;
    }

    public void setPayDatetime(String payDatetime) {
        this.payDatetime = payDatetime;
    }
}
