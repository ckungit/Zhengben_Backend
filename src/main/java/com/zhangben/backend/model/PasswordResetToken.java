package com.zhangben.backend.model;

import java.util.Date;

public class PasswordResetToken {
    private Integer id;
    private Integer userId;
    private String token;
    private Date expiresAt;
    private Byte used;
    private Date createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public Byte getUsed() { return used; }
    public void setUsed(Byte used) { this.used = used; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Date());
    }

    public boolean isUsed() {
        return used != null && used == 1;
    }
}
