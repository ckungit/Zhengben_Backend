package com.zhangben.backend.dto;

import java.util.List;

public class MyCreditOverviewItem {

    private Integer debtorId;
    private String debtorName;
    private Long totalAmount;
    private List<DebtorDebtDetailItem> details;

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

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<DebtorDebtDetailItem> getDetails() {
        return details;
    }

    public void setDetails(List<DebtorDebtDetailItem> details) {
        this.details = details;
    }
}