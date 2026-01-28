package com.zhangben.backend.dto;

public class LoginResponse {
    private String token;
    private String nickname;
    private String email;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, String nickname, String email, String role) {
        this.token = token;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
