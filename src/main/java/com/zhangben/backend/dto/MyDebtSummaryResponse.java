package com.zhangben.backend.dto;

public class MyDebtSummaryResponse {

    private Integer totalShouldReceive;
    private Integer totalShouldPay;

    public Integer getTotalShouldReceive() {
        return totalShouldReceive;
    }

    public void setTotalShouldReceive(Integer totalShouldReceive) {
        this.totalShouldReceive = totalShouldReceive;
    }

    public Integer getTotalShouldPay() {
        return totalShouldPay;
    }

    public void setTotalShouldPay(Integer totalShouldPay) {
        this.totalShouldPay = totalShouldPay;
    }
}