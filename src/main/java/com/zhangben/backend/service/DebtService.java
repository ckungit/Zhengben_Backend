package com.zhangben.backend.service;

import com.zhangben.backend.dto.*;

import java.util.List;

public interface DebtService {

    MyDebtSummaryResponse getMyDebtSummary(Integer userId);

    CreditorDebtOverviewResponse getDebtByCreditor(Integer userId, Integer creditorId);

    void repay(RepayRequest req, Integer currentUserId);

    List<MyCreditOverviewItem> getMyCreditOverview(Integer userId);
}