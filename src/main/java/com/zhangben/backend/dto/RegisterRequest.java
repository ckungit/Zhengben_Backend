package com.zhangben.backend.dto;

public class RegisterRequest {
    private String email;
    private String password;
    private String nickname;
    private String role;

    private String secondname;
    private String firstname;

    private Byte paypayFlag;
    private String paypayAccount;

    private Byte bankFlag;
    private String bankName;
    private String bankBranch;
    private String bankAccount;


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
	public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}