package com.zhangben.backend.dto;

import java.util.List;

/**
 * V35: 可代还债权人信息
 * 用于"帮他人还款"功能中选择要还给谁
 */
public class CreditorForOnBehalfItem {

    private Integer creditorId;
    private String creditorName;
    private String creditorAvatarUrl;
    private Integer friendCount;  // 有多少好友欠这个债权人钱
    private Long totalFriendDebt; // 好友总欠款

    // V39: 债权人支付方式和货币
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

    public Integer getFriendCount() {
        return friendCount;
    }

    public void setFriendCount(Integer friendCount) {
        this.friendCount = friendCount;
    }

    public Long getTotalFriendDebt() {
        return totalFriendDebt;
    }

    public void setTotalFriendDebt(Long totalFriendDebt) {
        this.totalFriendDebt = totalFriendDebt;
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
