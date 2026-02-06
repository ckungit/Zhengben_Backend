package com.zhangben.backend.dto;

import java.util.List;

/**
 * FIFO 还款响应 — 描述本次还款的 FIFO 冲销结果
 */
public class RepayFifoResponse {

    private String message;
    private List<FifoItem> settledBills;
    private boolean allSettled;       // 该债权人所有账清
    private Long remainingDebt;       // 剩余欠款 (cents)
    private Long totalRepaidThisTime; // 本次还款总额

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FifoItem> getSettledBills() {
        return settledBills;
    }

    public void setSettledBills(List<FifoItem> settledBills) {
        this.settledBills = settledBills;
    }

    public boolean isAllSettled() {
        return allSettled;
    }

    public void setAllSettled(boolean allSettled) {
        this.allSettled = allSettled;
    }

    public Long getRemainingDebt() {
        return remainingDebt;
    }

    public void setRemainingDebt(Long remainingDebt) {
        this.remainingDebt = remainingDebt;
    }

    public Long getTotalRepaidThisTime() {
        return totalRepaidThisTime;
    }

    public void setTotalRepaidThisTime(Long totalRepaidThisTime) {
        this.totalRepaidThisTime = totalRepaidThisTime;
    }
}
