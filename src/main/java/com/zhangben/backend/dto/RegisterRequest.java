package com.zhangben.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * V42: 添加 Bean Validation 注解
 */
public class RegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少8位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称长度应在2-20个字符之间")
    private String nickname;

    private String role;

    @Size(max = 50, message = "姓最多50个字符")
    private String secondname;

    @Size(max = 50, message = "名最多50个字符")
    private String firstname;

    private Byte paypayFlag;
    private String paypayAccount;

    private Byte bankFlag;
    private String bankName;
    private String bankBranch;
    private String bankAccount;

    // V24: 头像 Base64 数据 (可选)
    private String avatarBase64;

    // V39: 用户主要货币 (必填)
    @NotBlank(message = "请选择主要货币")
    private String primaryCurrency;

    public String getPrimaryCurrency() {
        return primaryCurrency;
    }

    public void setPrimaryCurrency(String primaryCurrency) {
        this.primaryCurrency = primaryCurrency;
    }

    public String getAvatarBase64() {
        return avatarBase64;
    }

    public void setAvatarBase64(String avatarBase64) {
        this.avatarBase64 = avatarBase64;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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
}
