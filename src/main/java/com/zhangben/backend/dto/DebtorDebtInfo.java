package com.zhangben.backend.dto;

/**
 * V35: 债务人欠款信息
 * 用于代人还款时显示每个债务人对某债权人的欠款情况
 */
public class DebtorDebtInfo {

    private Integer debtorId;
    private String debtorName;
    private String debtorAvatarUrl;
    private Long totalDebt;       // 总欠款
    private Long pendingAmount;   // 待确认金额
    private Long availableAmount; // 可还金额 = totalDebt - pendingAmount

    public Integer getDebtorId() {
        return debtorId;
    }

    public void setDebtorId(Integer debtorId) {
        this.debtorId = debtorId;
    }

    public String getDebtorName() {
        return debtorName;
    }

    public void setDebtorName(String debtorName) {
        this.debtorName = debtorName;
    }

    public String getDebtorAvatarUrl() {
        return debtorAvatarUrl;
    }

    public void setDebtorAvatarUrl(String debtorAvatarUrl) {
        this.debtorAvatarUrl = debtorAvatarUrl;
    }

    public Long getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(Long totalDebt) {
        this.totalDebt = totalDebt;
    }

    public Long getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(Long pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public Long getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(Long availableAmount) {
        this.availableAmount = availableAmount;
    }
}
