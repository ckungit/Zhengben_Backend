package com.zhangben.backend.service.impl;

import com.zhangben.backend.dto.*;
import com.zhangben.backend.mapper.OutcomeMapper;
import com.zhangben.backend.mapper.OutcomeParticipantMapper;
import com.zhangben.backend.mapper.PayStyleMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.*;
import com.zhangben.backend.service.DebtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DebtServiceImpl implements DebtService {

    @Autowired
    private OutcomeMapper outcomeMapper;

    @Autowired
    private OutcomeParticipantMapper outcomeParticipantMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PayStyleMapper payStyleMapper;

    /**
     * 使用 Example 查询所有未删除的 outcome
     */
    private List<Outcome> loadAllOutcomes() {
        OutcomeExample example = new OutcomeExample();
        example.createCriteria().andDeletedFlagEqualTo((byte) 0);
        return outcomeMapper.selectByExample(example);
    }

    /**
     * 查询某个 outcome 的所有参与者
     */
    private List<OutcomeParticipant> loadParticipants(Integer outcomeId) {
        OutcomeParticipantExample example = new OutcomeParticipantExample();
        example.createCriteria().andOutcomeIdEqualTo(outcomeId);
        return outcomeParticipantMapper.selectByExample(example);
    }

    /**
     * 构建 debtor-creditor 的净欠款表
     */
    private Map<String, Long> buildDebtMap() {

        Map<String, Long> map = new HashMap<>();

        List<Outcome> list = loadAllOutcomes();

        for (Outcome o : list) {

            // repay_flag = 1 → 消费
            if (o.getRepayFlag() == (byte) 1) {

                List<OutcomeParticipant> ps = loadParticipants(o.getId());

                for (OutcomeParticipant p : ps) {
                    Integer uid = p.getUserId();

                    // 自己不欠自己
                    if (uid.equals(o.getPayerUserid())) {
                        continue;
                    }

                    String key = uid + "-" + o.getPayerUserid();
                    map.put(key, map.getOrDefault(key, 0L) + o.getPerAmount());
                }
            }

            // repay_flag = 2 → 还款
            if (o.getRepayFlag() == (byte) 2) {

                Integer debtor = o.getPayerUserid();
                Integer creditor = o.getTargetUserid();

                String key = debtor + "-" + creditor;
                map.put(key, map.getOrDefault(key, 0L) - o.getAmount());
            }
        }

        return map;
    }

    @Override
    public MyDebtSummaryResponse getMyDebtSummary(Integer userId) {

        Map<String, Long> map = buildDebtMap();

        int shouldReceive = 0;
        int shouldPay = 0;

        for (Map.Entry<String, Long> e : map.entrySet()) {

            String[] parts = e.getKey().split("-");
            Integer debtor = Integer.valueOf(parts[0]);
            Integer creditor = Integer.valueOf(parts[1]);
            Long amount = e.getValue();

            if (amount <= 0) continue;

            if (creditor.equals(userId)) {
                shouldReceive += amount;
            }
            if (debtor.equals(userId)) {
                shouldPay += amount;
            }
        }

        MyDebtSummaryResponse resp = new MyDebtSummaryResponse();
        resp.setTotalShouldReceive(shouldReceive);
        resp.setTotalShouldPay(shouldPay);
        return resp;
    }

    @Override
    public CreditorDebtOverviewResponse getDebtByCreditor(Integer userId, Integer creditorId) {

        Map<String, Long> map = buildDebtMap();

        String key = userId + "-" + creditorId;
        Long total = map.getOrDefault(key, 0L);

        CreditorDebtOverviewResponse resp = new CreditorDebtOverviewResponse();
        resp.setCreditorId(creditorId);

        User creditor = userMapper.selectByPrimaryKey(creditorId);
        resp.setCreditorName(creditor.getNickname());
        resp.setTotalAmount(total);

        List<Outcome> all = loadAllOutcomes();
        List<CreditorDebtDetailItem> details = new ArrayList<>();

        for (Outcome o : all) {

            // 消费明细
            if (o.getRepayFlag() == (byte) 1) {

                List<OutcomeParticipant> ps = loadParticipants(o.getId());
                boolean involved = ps.stream().anyMatch(p -> p.getUserId().equals(userId));

                if (involved && o.getPayerUserid().equals(creditorId)) {

                    CreditorDebtDetailItem item = new CreditorDebtDetailItem();
                    item.setOutcomeId(o.getId());
                    item.setAmount(o.getPerAmount());
                    item.setComment(o.getComment());
                    item.setPayDatetime(o.getPayDatetime());
                    item.setCategoryName(payStyleMapper.selectByPrimaryKey(o.getStyleId()).getStyleName());
                    item.setLocationText("POINT");

                    details.add(item);
                }
            }

            // 还款明细
            if (o.getRepayFlag() == (byte) 2) {

                if (o.getPayerUserid().equals(userId) && o.getTargetUserid().equals(creditorId)) {

                    CreditorDebtDetailItem item = new CreditorDebtDetailItem();
                    item.setOutcomeId(o.getId());
                    item.setAmount(-o.getAmount());
                    item.setComment(o.getComment());
                    item.setPayDatetime(o.getPayDatetime());
                    item.setCategoryName("还款");
                    item.setLocationText("N/A");

                    details.add(item);
                }
            }
        }

        resp.setDetails(details);
        return resp;
    }

    @Override
    public void repay(RepayRequest req, Integer currentUserId) {

        Outcome o = new Outcome();
        o.setAmount(req.getAmount());
        o.setPayerUserid(currentUserId);
        o.setTargetUserid(req.getCreditorId());
        o.setRepayFlag((byte) 2);
        o.setPerAmount(req.getAmount());
        o.setStyleId(req.getStyleId());
        o.setComment(req.getComment());
        o.setDeletedFlag((byte) 0);
        o.setPayDatetime(LocalDateTime.now());

        outcomeMapper.insertSelective(o);
    }

    @Override
    public List<MyCreditOverviewItem> getMyCreditOverview(Integer userId) {

        Map<String, Long> map = buildDebtMap();

        Map<Integer, Long> debtorMap = new HashMap<>();

        for (Map.Entry<String, Long> e : map.entrySet()) {

            String[] parts = e.getKey().split("-");
            Integer debtor = Integer.valueOf(parts[0]);
            Integer creditor = Integer.valueOf(parts[1]);
            Long amount = e.getValue();

            if (amount > 0 && creditor.equals(userId)) {
                debtorMap.put(debtor, debtorMap.getOrDefault(debtor, 0L) + amount);
            }
        }

        List<MyCreditOverviewItem> list = new ArrayList<>();

        List<Outcome> all = loadAllOutcomes();

        for (Map.Entry<Integer, Long> e : debtorMap.entrySet()) {

            Integer debtorId = e.getKey();
            Long totalAmount = e.getValue();

            User debtor = userMapper.selectByPrimaryKey(debtorId);

            MyCreditOverviewItem item = new MyCreditOverviewItem();
            item.setDebtorId(debtorId);
            item.setDebtorName(debtor.getNickname());
            item.setTotalAmount(totalAmount);

            List<DebtorDebtDetailItem> details = new ArrayList<>();

            for (Outcome o : all) {

                // 消费明细
                if (o.getRepayFlag() == (byte) 1) {

                    List<OutcomeParticipant> ps = loadParticipants(o.getId());
                    boolean involved = ps.stream().anyMatch(p -> p.getUserId().equals(debtorId));

                    if (involved && o.getPayerUserid().equals(userId)) {

                        DebtorDebtDetailItem d = new DebtorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(o.getPerAmount());
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());
                        d.setCategoryName(payStyleMapper.selectByPrimaryKey(o.getStyleId()).getStyleName());
                        d.setLocationText("POINT");

                        details.add(d);
                    }
                }

                // 还款明细
                if (o.getRepayFlag() == (byte) 2) {

                    if (o.getPayerUserid().equals(debtorId) && o.getTargetUserid().equals(userId)) {

                        DebtorDebtDetailItem d = new DebtorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(-o.getAmount());
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());
                        d.setCategoryName("还款");
                        d.setLocationText("N/A");

                        details.add(d);
                    }
                }
            }

            item.setDetails(details);
            list.add(item);
        }

        return list;
    }
}