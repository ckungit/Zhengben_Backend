package com.zhangben.backend.service;

import com.zhangben.backend.dto.*;

import java.util.List;

public interface DebtService {

    /**
     * V35: 批量还款（支持代人还款）
     */
    void batchRepay(BatchRepayRequest req, Integer currentUserId);

    /**
     * V35: 获取某债权人下好友债务人的欠款信息（只返回好友）
     */
    List<DebtorDebtInfo> getDebtorsForCreditor(Integer creditorId, Integer currentUserId);

    /**
     * V35: 获取有好友欠款的债权人列表（用于帮他人还款）
     */
    List<CreditorForOnBehalfItem> getCreditorsWithFriendDebts(Integer currentUserId);

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

    /**
     * 获取最优结算方案（全局债务简化）
     */
    SettlementResponse getMinimizedSettlements(Integer userId);

    /**
     * FIFO 销账还款：按时间先后对冲旧账单，返回冲销明细
     */
    RepayFifoResponse repayWithFifo(RepayRequest req, Integer userId);
}
