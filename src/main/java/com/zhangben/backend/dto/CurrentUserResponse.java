package com.zhangben.backend.dto;

public class CurrentUserResponse {
    private Integer id;
    private String email;
    private String nickname;
    private String role;

    public CurrentUserResponse(Integer id, String email, String nickname, String role) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public Integer getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getRole() { return role; }
}