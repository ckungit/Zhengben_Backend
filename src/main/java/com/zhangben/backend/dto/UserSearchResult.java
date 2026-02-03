package com.zhangben.backend.dto;

import java.util.List;

/**
 * 用户搜索结果
 */
public class UserSearchResult {

    private Integer id;
    private String nickname;
    private String email;
    private String fullName;
    private String avatarUrl;

    // 支持的收款方式（旧字段，保持兼容）
    private Boolean paypaySupported;
    private Boolean bankSupported;

    // V39: 支付方式列表（新字段）
    private List<String> paymentMethods;

    // V39: 主要货币
    private String primaryCurrency;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
