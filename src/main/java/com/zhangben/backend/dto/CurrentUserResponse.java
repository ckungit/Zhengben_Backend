package com.zhangben.backend.dto;

public class CurrentUserResponse {
    private Integer id;
    private String email;
    private String nickname;
    private String role;
    private String firstname;
    private String secondname;
    private Byte paypayFlag;
    private Byte bankFlag;
    private String primaryCurrency;
    private String avatarUrl;

    // 无参构造函数
    public CurrentUserResponse() {
    }

    // 全参构造函数
    public CurrentUserResponse(Integer id, String email, String nickname, String role) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    // Getters
    public Integer getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getRole() { return role; }
    public String getFirstname() { return firstname; }
    public String getSecondname() { return secondname; }
    public Byte getPaypayFlag() { return paypayFlag; }
    public Byte getBankFlag() { return bankFlag; }
    public String getPrimaryCurrency() { return primaryCurrency; }
    public String getAvatarUrl() { return avatarUrl; }

    // Setters
    public void setId(Integer id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setRole(String role) { this.role = role; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public void setSecondname(String secondname) { this.secondname = secondname; }
    public void setPaypayFlag(Byte paypayFlag) { this.paypayFlag = paypayFlag; }
    public void setBankFlag(Byte bankFlag) { this.bankFlag = bankFlag; }
    public void setPrimaryCurrency(String primaryCurrency) { this.primaryCurrency = primaryCurrency; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}