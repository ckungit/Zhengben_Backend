package com.zhangben.backend.dto;

public class CompleteProfileRequest {
    private String password;
    private String nickname;
    private String secondname;
    private String firstname;
    private Integer paypayFlag;
    private Integer bankFlag;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSecondname() {
        return secondname;
    }

    public void setSecondname(String secondname) {
        this.secondname = secondname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public Integer getPaypayFlag() {
        return paypayFlag;
    }

    public void setPaypayFlag(Integer paypayFlag) {
        this.paypayFlag = paypayFlag;
    }

    public Integer getBankFlag() {
        return bankFlag;
    }

    public void setBankFlag(Integer bankFlag) {
        this.bankFlag = bankFlag;
    }
}
