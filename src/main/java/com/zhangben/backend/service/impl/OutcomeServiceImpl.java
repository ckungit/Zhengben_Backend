package com.zhangben.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.OutcomeCreateRequest;
import com.zhangben.backend.dto.RecentOutcomeItem;
import com.zhangben.backend.mapper.OutcomeMapper;
import com.zhangben.backend.mapper.OutcomeParticipantMapper;
import com.zhangben.backend.mapper.PayStyleMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.*;
import com.zhangben.backend.service.OutcomeService;
import com.zhangben.backend.util.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OutcomeServiceImpl implements OutcomeService {

    @Autowired
    private OutcomeMapper outcomeMapper;

    @Autowired
    private OutcomeParticipantMapper outcomeParticipantMapper;

    @Autowired
    private PayStyleMapper payStyleMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void createOutcome(OutcomeCreateRequest req) {

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        if (req.getRepayFlag() == null) {
            throw new IllegalArgumentException("repayFlag 必须指定（1=支付，2=还钱）");
        }

        // 当前登录用户 = 支付者
        Integer payerId = StpUtil.getLoginIdAsInt();

        // V18: 自定义份额功能
        // 获取参与者份额映射（如果没有提供，每人默认1份）
        java.util.Map<Integer, Integer> sharesMap = req.getParticipantShares();

        // 计算各类份额
        int selfShares = Boolean.TRUE.equals(req.getIncludeSelf())
            ? (req.getSelfShares() != null ? req.getSelfShares() : 1)
            : 0;

        int extraCount = (req.getExtraParticipants() != null) ? req.getExtraParticipants() : 0;
        int extraShares = (req.getExtraShares() != null) ? req.getExtraShares() : extraCount;

        // 计算注册用户的总份额
        int registeredShares = 0;
        if (req.getTargetUserIds() != null) {
            for (Integer uid : req.getTargetUserIds()) {
                int shares = (sharesMap != null && sharesMap.containsKey(uid)) ? sharesMap.get(uid) : 1;
                registeredShares += shares;
            }
        }

        // 总份额数
        int totalShares = selfShares + registeredShares + extraShares;

        if (totalShares <= 0) {
            throw new IllegalArgumentException("总份额数必须大于0");
        }

        // 计算 per_amount（每份金额，向上取整）
        long perAmount = (long) Math.ceil((double) req.getAmount() / totalShares);

        // 写入 outcome 表
        Outcome outcome = new Outcome();
        outcome.setAmount(req.getAmount());
        outcome.setPayerUserid(payerId);
        outcome.setTargetUserid(0); // 多人场景下不使用此字段
        outcome.setRepayFlag(req.getRepayFlag());
        outcome.setPerAmount(perAmount);
        outcome.setTotalShares(totalShares); // V18: 保存总份额数
        outcome.setStyleId(req.getStyleId());
        outcome.setComment(req.getComment());
        outcome.setDeletedFlag((byte) 0);
        outcome.setExtraParticipants(extraCount);

        LocalDateTime payTime = req.getPayDatetime() != null ? req.getPayDatetime() : LocalDateTime.now();
        outcome.setPayDatetime(payTime);

        // GPS 转换为 MySQL POINT（二进制）
        if (req.getLatitude() != null && req.getLongitude() != null) {
            byte[] pointBytes = GeoUtils.toPoint(req.getLongitude(), req.getLatitude());
            outcome.setLocaton(pointBytes);
        }

        outcomeMapper.insertSelective(outcome);

        // 写入参与者 outcome_participant（仅注册用户，包含份额）
        if (req.getTargetUserIds() != null) {
            for (Integer uid : req.getTargetUserIds()) {
                int shares = (sharesMap != null && sharesMap.containsKey(uid)) ? sharesMap.get(uid) : 1;
                OutcomeParticipant ep = new OutcomeParticipant();
                ep.setOutcomeId(outcome.getId());
                ep.setUserId(uid);
                ep.setShares(shares); // V18: 保存份额
                outcomeParticipantMapper.insertSelective(ep);
            }
        }

        // includeSelf = true → 把 payer 也加入参与者
        if (Boolean.TRUE.equals(req.getIncludeSelf())) {
            OutcomeParticipant ep = new OutcomeParticipant();
            ep.setOutcomeId(outcome.getId());
            ep.setUserId(payerId);
            ep.setShares(selfShares); // V18: 保存自己的份额
            outcomeParticipantMapper.insertSelective(ep);
        }
    }

    @Override
    public List<RecentOutcomeItem> getRecentOutcomes(Integer userId, Integer limit) {

        // 查询用户作为支付者的所有记录
        OutcomeExample example = new OutcomeExample();
        example.createCriteria()
                .andPayerUseridEqualTo(userId)
                .andDeletedFlagEqualTo((byte) 0);
        example.setOrderByClause("pay_datetime DESC");

        List<Outcome> outcomes = outcomeMapper.selectByExample(example);

        // 限制数量
        List<Outcome> limited = outcomes.stream()
                .limit(limit)
                .collect(Collectors.toList());

        List<RecentOutcomeItem> result = new ArrayList<>();

        for (Outcome o : limited) {
            RecentOutcomeItem item = new RecentOutcomeItem();
            item.setId(o.getId());
            item.setAmount(o.getAmount());
            item.setPerAmount(o.getPerAmount());
            item.setComment(o.getComment());
            item.setRepayFlag(o.getRepayFlag());
            item.setPayDatetime(o.getPayDatetime());

            // 获取分类名称
            if (o.getRepayFlag() == (byte) 2) {
                // 还款记录
                item.setStyleName("还款");
                
                // 设置还款对象信息
                if (o.getTargetUserid() != null && o.getTargetUserid() > 0) {
                    item.setTargetUserId(o.getTargetUserid());
                    User targetUser = userMapper.selectByPrimaryKey(o.getTargetUserid());
                    if (targetUser != null) {
                        item.setTargetUserName(targetUser.getNickname());
                    }
                }
            } else {
                // 普通支付记录
                if (o.getStyleId() != null && o.getStyleId() > 0) {
                    PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                    item.setStyleName(style != null ? style.getStyleName() : "未分类");
                } else {
                    item.setStyleName("未分类");
                }
            }

            // 获取参与者名称
            OutcomeParticipantExample pExample = new OutcomeParticipantExample();
            pExample.createCriteria().andOutcomeIdEqualTo(o.getId());
            List<OutcomeParticipant> participants = outcomeParticipantMapper.selectByExample(pExample);

            List<String> names = new ArrayList<>();
            for (OutcomeParticipant p : participants) {
                User u = userMapper.selectByPrimaryKey(p.getUserId());
                if (u != null) {
                    names.add(u.getNickname());
                }
            }
            item.setParticipantNames(names);

            result.add(item);
        }

        return result;
    }

    @Override
    public void deleteOutcome(Integer outcomeId, Integer userId) {
        // 查询记录
        Outcome outcome = outcomeMapper.selectByPrimaryKey(outcomeId);
        
        if (outcome == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        
        // 检查是否是本人创建的记录
        if (!outcome.getPayerUserid().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己创建的记录");
        }
        
        // 检查是否已删除
        if (outcome.getDeletedFlag() != null && outcome.getDeletedFlag() == 1) {
            throw new IllegalArgumentException("记录已删除");
        }
        
        // 软删除
        outcome.setDeletedFlag((byte) 1);
        outcomeMapper.updateByPrimaryKeySelective(outcome);
    }
}
