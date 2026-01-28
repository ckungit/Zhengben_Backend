package com.zhangben.backend.dto;

import java.util.List;

public class CreditorDebtOverviewResponse {

    private Integer creditorId;
    private String creditorName;
    private Long totalAmount;
    private List<CreditorDebtDetailItem> details;

    public Integer getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(Integer creditorId) {
        this.creditorId = creditorId;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<CreditorDebtDetailItem> getDetails() {
        return details;
    }

    public void setDetails(List<CreditorDebtDetailItem> details) {
        this.details = details;
    }
}