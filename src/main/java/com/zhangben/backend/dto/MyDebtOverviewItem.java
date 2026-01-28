package com.zhangben.backend.dto;

import java.util.List;

/**
 * 我的欠款概览项（我欠某人的钱）
 */
public class MyDebtOverviewItem {

    private Integer creditorId;
    private String creditorName;
    private Long totalAmount;
    private List<CreditorDebtDetailItem> details;
    
    // 债权人支持的收款方式
    private Boolean paypaySupported;
    private Boolean bankSupported;

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

    public Boolean getPaypaySupported() {
        return paypaySupported;
    }

    public void setPaypaySupported(Boolean paypaySupported) {
        this.paypaySupported = paypaySupported;
    }

    public Boolean getBankSupported() {
        return bankSupported;
    }

    public void setBankSupported(Boolean bankSupported) {
        this.bankSupported = bankSupported;
    }
}
