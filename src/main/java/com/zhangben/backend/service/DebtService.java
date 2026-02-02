package com.zhangben.backend.service;

import com.zhangben.backend.dto.*;

import java.util.List;

public interface DebtService {

    MyDebtSummaryResponse getMyDebtSummary(Integer userId);

    CreditorDebtOverviewResponse getDebtByCreditor(Integer userId, Integer creditorId);

    void repay(RepayRequest req, Integer currentUserId);

    /**
     * 获取我的债权（别人欠我的钱）
     */
    List<MyCreditOverviewItem> getMyCreditOverview(Integer userId);

    /**
     * 获取我的欠款（我欠别人的钱）
     */
    List<MyDebtOverviewItem> getMyDebtOverview(Integer userId);

    /**
     * V30: 获取待确认的还款（我需要确认的）
     */
    List<PendingRepaymentItem> getPendingRepayments(Integer creditorId);

    /**
     * V30: 确认还款
     */
    void confirmRepayment(Integer repaymentId, Integer creditorId);

    /**
     * V30: 批量确认还款
     */
    void batchConfirmRepayments(List<Integer> repaymentIds, Integer creditorId);

    /**
     * V32: 获取我发起的待确认还款（作为债务人）
     */
    List<MyPendingRepaymentItem> getMyPendingRepayments(Integer debtorId);
}
