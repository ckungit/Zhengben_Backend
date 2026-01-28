package com.zhangben.backend.dto;

public class GoogleSSOResponse {
    private String token;
    private Boolean isNewUser;
    private Boolean needsProfileCompletion;
    private String email;
    private String nickname;
    private String role;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getIsNewUser() {
        return isNewUser;
    }

    public void setIsNewUser(Boolean isNewUser) {
        this.isNewUser = isNewUser;
    }

    public Boolean getNeedsProfileCompletion() {
        return needsProfileCompletion;
    }

    public void setNeedsProfileCompletion(Boolean needsProfileCompletion) {
        this.needsProfileCompletion = needsProfileCompletion;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
