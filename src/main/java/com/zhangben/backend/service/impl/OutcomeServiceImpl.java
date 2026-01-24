package com.zhangben.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.OutcomeCreateRequest;
import com.zhangben.backend.mapper.OutcomeMapper;
import com.zhangben.backend.mapper.OutcomeParticipantMapper;
import com.zhangben.backend.model.Outcome;
import com.zhangben.backend.model.OutcomeParticipant;
import com.zhangben.backend.service.OutcomeService;
import com.zhangben.backend.util.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OutcomeServiceImpl implements OutcomeService {

    @Autowired
    private OutcomeMapper outcomeMapper;

    @Autowired
    private OutcomeParticipantMapper outcomeParticipantMapper;

    @Override
    public void createOutcome(OutcomeCreateRequest req) {

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new IllegalArgumentException("amount 必须大于 0");
        }
        if (req.getTargetUserIds() == null || req.getTargetUserIds().isEmpty()) {
            throw new IllegalArgumentException("至少需要一个参与者");
        }
        if (req.getRepayFlag() == null) {
            throw new IllegalArgumentException("repayFlag 必须指定（1=支付，2=还钱）");
        }

        // 当前登录用户 = 支付者
        Integer payerId = StpUtil.getLoginIdAsInt();

        // 1. 计算参与人数
        int count = req.getTargetUserIds().size();
        if (Boolean.TRUE.equals(req.getIncludeSelf())) {
            count += 1;
        }

        // 2. 计算 per_amount（向上取整）
        long perAmount = (long) Math.ceil((double) req.getAmount() / count);

        // 3. 写入 outcome 表
        Outcome outcome = new Outcome();
        outcome.setAmount(req.getAmount());
        outcome.setPayerUserid(payerId);
        outcome.setTargetUserid(0); // 多人场景下不使用此字段
        outcome.setRepayFlag(req.getRepayFlag());
        outcome.setPerAmount(perAmount);
        outcome.setStyleId(req.getStyleId());
        outcome.setComment(req.getComment());
        outcome.setDeletedFlag((byte) 0);

        LocalDateTime payTime = req.getPayDatetime() != null ? req.getPayDatetime() : LocalDateTime.now();
        outcome.setPayDatetime(payTime);

        // GPS 转换为 MySQL POINT（二进制）
        if (req.getLatitude() != null && req.getLongitude() != null) {
            byte[] pointBytes = GeoUtils.toPoint(req.getLongitude(), req.getLatitude());
            outcome.setLocaton(pointBytes);
        }

        outcomeMapper.insertSelective(outcome);

        // 4. 写入参与者 outcome_participant
        for (Integer uid : req.getTargetUserIds()) {
            OutcomeParticipant ep = new OutcomeParticipant();
            ep.setOutcomeId(outcome.getId());
            ep.setUserId(uid);
            outcomeParticipantMapper.insertSelective(ep);
        }

        // 5. includeSelf = true → 把 payer 也加入参与者
        if (Boolean.TRUE.equals(req.getIncludeSelf())) {
            OutcomeParticipant ep = new OutcomeParticipant();
            ep.setOutcomeId(outcome.getId());
            ep.setUserId(payerId);
            outcomeParticipantMapper.insertSelective(ep);
        }
    }
}