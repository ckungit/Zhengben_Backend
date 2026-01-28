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
}
