package com.zhangben.backend.service;

import com.zhangben.backend.dto.OutcomeCreateRequest;

public interface OutcomeService {

    /**
     * 创建一条支出/还款记录（支持多人 AA）
     */
    void createOutcome(OutcomeCreateRequest req);
}