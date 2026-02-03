package com.zhangben.backend.dto;

import java.util.List;

/**
 * 我的欠款概览项（我欠某人的钱）
 */
public class MyDebtOverviewItem {

    private Integer creditorId;
    private String creditorName;
    private String creditorAvatarUrl;
    private Long totalAmount;
    private Long pendingAmount; // V32: 待确认的还款金额
    private List<CreditorDebtDetailItem> details;

    // 债权人支持的收款方式（旧字段）
    private Boolean paypaySupported;
    private Boolean bankSupported;

    // V39: 债权人支付方式列表和主要货币
    private List<String> paymentMethods;
    private String primaryCurrency;

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

    public String getCreditorAvatarUrl() {
        return creditorAvatarUrl;
    }

    public void setCreditorAvatarUrl(String creditorAvatarUrl) {
        this.creditorAvatarUrl = creditorAvatarUrl;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(Long pendingAmount) {
        this.pendingAmount = pendingAmount;
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

    public List<String> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<String> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public String getPrimaryCurrency() {
        return primaryCurrency;
    }

    public void setPrimaryCurrency(String primaryCurrency) {
        this.primaryCurrency = primaryCurrency;
    }
}
