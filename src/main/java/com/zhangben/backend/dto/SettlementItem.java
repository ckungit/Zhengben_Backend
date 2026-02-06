package com.zhangben.backend.dto;

public class SettlementItem {

    private Integer fromId;
    private Integer toId;
    private String fromName;
    private String toName;
    private String fromAvatarUrl;
    private String toAvatarUrl;
    private Long amount; // cents

    public Integer getFromId() {
        return fromId;
    }

    public void setFromId(Integer fromId) {
        this.fromId = fromId;
    }

    public Integer getToId() {
        return toId;
    }

    public void setToId(Integer toId) {
        this.toId = toId;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getFromAvatarUrl() {
        return fromAvatarUrl;
    }

    public void setFromAvatarUrl(String fromAvatarUrl) {
        this.fromAvatarUrl = fromAvatarUrl;
    }

    public String getToAvatarUrl() {
        return toAvatarUrl;
    }

    public void setToAvatarUrl(String toAvatarUrl) {
        this.toAvatarUrl = toAvatarUrl;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
