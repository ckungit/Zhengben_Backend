package com.zhangben.backend.dto;

/**
 * 用户搜索结果
 */
public class UserSearchResult {

    private Integer id;
    private String nickname;
    private String email;
    private String fullName;
    private String avatarUrl;

    // 支持的收款方式
    private Boolean paypaySupported;
    private Boolean bankSupported;

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
}
