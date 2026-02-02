package com.zhangben.backend.dto;

/**
 * V35: 可代还债权人信息
 * 用于"帮他人还款"功能中选择要还给谁
 */
public class CreditorForOnBehalfItem {

    private Integer creditorId;
    private String creditorName;
    private String creditorAvatarUrl;
    private Integer friendCount;  // 有多少好友欠这个债权人钱
    private Long totalFriendDebt; // 好友总欠款

    public Integer getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(Integer creditorId) {
        this.creditorId = creditorId;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

    public String getCreditorAvatarUrl() {
        return creditorAvatarUrl;
    }

    public void setCreditorAvatarUrl(String creditorAvatarUrl) {
        this.creditorAvatarUrl = creditorAvatarUrl;
    }

    public Integer getFriendCount() {
        return friendCount;
    }

    public void setFriendCount(Integer friendCount) {
        this.friendCount = friendCount;
    }

    public Long getTotalFriendDebt() {
        return totalFriendDebt;
    }

    public void setTotalFriendDebt(Long totalFriendDebt) {
        this.totalFriendDebt = totalFriendDebt;
    }
}
