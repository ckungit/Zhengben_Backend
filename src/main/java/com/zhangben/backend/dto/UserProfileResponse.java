package com.zhangben.backend.dto;

/**
 * 用户详细信息响应
 */
public class UserProfileResponse {

    private Integer id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String role;
    private String firstname;
    private String secondname;
    private Byte paypayFlag;
    private String paypayAccount;
    private Byte bankFlag;
    private String bankName;
    private String bankBranch;
    private String bankAccount;
    private String preferredLanguage;
    private String primaryCurrency;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getSecondname() {
        return secondname;
    }

    public void setSecondname(String secondname) {
        this.secondname = secondname;
    }

    public Byte getPaypayFlag() {
        return paypayFlag;
    }

    public void setPaypayFlag(Byte paypayFlag) {
        this.paypayFlag = paypayFlag;
    }

    public String getPaypayAccount() {
        return paypayAccount;
    }

    public void setPaypayAccount(String paypayAccount) {
        this.paypayAccount = paypayAccount;
    }

    public Byte getBankFlag() {
        return bankFlag;
    }

    public void setBankFlag(Byte bankFlag) {
        this.bankFlag = bankFlag;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getPrimaryCurrency() {
        return primaryCurrency;
    }

    public void setPrimaryCurrency(String primaryCurrency) {
        this.primaryCurrency = primaryCurrency;
    }
}
