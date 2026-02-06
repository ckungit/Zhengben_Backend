package com.zhangben.backend.model;

import java.time.LocalDateTime;

public class UserMonthlySpending {

    private Integer id;
    private Integer userId;
    private String month;            // "YYYY-MM"
    private Long totalSpending;      // cents
    private Integer transactionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Long getTotalSpending() { return totalSpending; }
    public void setTotalSpending(Long totalSpending) { this.totalSpending = totalSpending; }

    public Integer getTransactionCount() { return transactionCount; }
    public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
