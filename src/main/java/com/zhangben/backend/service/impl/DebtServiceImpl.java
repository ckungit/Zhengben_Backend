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
     * 构建 debtor-creditor 的原始欠款表（不含净额计算）
     */
    private Map<String, Long> buildRawDebtMap() {

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

                    // V18: 使用份额计算欠款金额
                    Integer shares = p.getShares() != null ? p.getShares() : 1;
                    long debtAmount = o.getPerAmount() * shares;

                    String key = uid + "-" + o.getPayerUserid();
                    map.put(key, map.getOrDefault(key, 0L) + debtAmount);
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

    /**
     * V19: 构建净额欠款表
     * 当A欠B 300元，B欠A 200元时，计算净额：A欠B 100元
     */
    private Map<String, Long> buildDebtMap() {

        Map<String, Long> rawMap = buildRawDebtMap();
        Map<String, Long> netMap = new HashMap<>();

        // 记录已处理过的用户对
        Set<String> processed = new HashSet<>();

        for (Map.Entry<String, Long> e : rawMap.entrySet()) {

            String[] parts = e.getKey().split("-");
            Integer userA = Integer.valueOf(parts[0]);
            Integer userB = Integer.valueOf(parts[1]);

            // 创建规范化的用户对标识（小ID在前）
            String pairKey = userA < userB ? userA + ":" + userB : userB + ":" + userA;

            if (processed.contains(pairKey)) {
                continue;
            }
            processed.add(pairKey);

            // A欠B的金额
            String keyAB = userA + "-" + userB;
            Long amountAB = rawMap.getOrDefault(keyAB, 0L);

            // B欠A的金额
            String keyBA = userB + "-" + userA;
            Long amountBA = rawMap.getOrDefault(keyBA, 0L);

            // 计算净额
            long net = amountAB - amountBA;

            if (net > 0) {
                // A欠B净额
                netMap.put(keyAB, net);
            } else if (net < 0) {
                // B欠A净额
                netMap.put(keyBA, -net);
            }
            // net == 0 表示两清，不记录
        }

        return netMap;
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

        Map<String, Long> netMap = buildDebtMap();
        Map<String, Long> rawMap = buildRawDebtMap();

        String key = userId + "-" + creditorId;
        Long total = netMap.getOrDefault(key, 0L);

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
                Optional<OutcomeParticipant> participantOpt = ps.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst();

                if (participantOpt.isPresent() && o.getPayerUserid().equals(creditorId)) {

                    // V18: 使用份额计算金额
                    OutcomeParticipant participant = participantOpt.get();
                    Integer shares = participant.getShares() != null ? participant.getShares() : 1;
                    long debtAmount = o.getPerAmount() * shares;

                    CreditorDebtDetailItem item = new CreditorDebtDetailItem();
                    item.setOutcomeId(o.getId());
                    item.setAmount(debtAmount);
                    item.setComment(o.getComment());
                    item.setPayDatetime(o.getPayDatetime());

                    if (o.getStyleId() != null) {
                        PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                        item.setCategoryName(style != null ? style.getStyleName() : "未分类");
                    } else {
                        item.setCategoryName("未分类");
                    }
                    item.setLocationText("位置信息");

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
                    item.setLocationText("无");

                    details.add(item);
                }
            }
        }

        // V19: 如果有互相欠款，添加抵消记录
        String reverseKey = creditorId + "-" + userId; // 对方欠我的金额
        Long theyOweMeRaw = rawMap.getOrDefault(reverseKey, 0L);
        if (theyOweMeRaw > 0) {
            CreditorDebtDetailItem offsetItem = new CreditorDebtDetailItem();
            offsetItem.setOutcomeId(-1);
            offsetItem.setAmount(-theyOweMeRaw);
            offsetItem.setComment("互相欠款抵消（Ta欠我的部分）");
            offsetItem.setPayDatetime(java.time.LocalDateTime.now());
            offsetItem.setCategoryName("抵消");
            offsetItem.setLocationText("无");
            details.add(offsetItem);
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
        // style_id 如果没有传，设置为0或者一个默认值
        o.setStyleId(req.getStyleId() != null ? req.getStyleId() : 0);
        o.setComment(req.getComment());
        o.setDeletedFlag((byte) 0);

        // V20: 支持自定义还款时间
        LocalDateTime payTime = req.getPayDatetime() != null ? req.getPayDatetime() : LocalDateTime.now();
        o.setPayDatetime(payTime);

        outcomeMapper.insertSelective(o);
    }

    @Override
    public List<MyCreditOverviewItem> getMyCreditOverview(Integer userId) {

        Map<String, Long> netMap = buildDebtMap();
        Map<String, Long> rawMap = buildRawDebtMap();

        Map<Integer, Long> debtorMap = new HashMap<>();

        for (Map.Entry<String, Long> e : netMap.entrySet()) {

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
            item.setDebtorAvatarUrl(debtor.getAvatarUrl());
            item.setTotalAmount(totalAmount);

            List<DebtorDebtDetailItem> details = new ArrayList<>();

            for (Outcome o : all) {

                // 消费明细 - 我付款，对方参与
                if (o.getRepayFlag() == (byte) 1) {

                    List<OutcomeParticipant> ps = loadParticipants(o.getId());
                    Optional<OutcomeParticipant> participantOpt = ps.stream()
                        .filter(p -> p.getUserId().equals(debtorId))
                        .findFirst();

                    if (participantOpt.isPresent() && o.getPayerUserid().equals(userId)) {

                        // V18: 使用份额计算金额
                        OutcomeParticipant participant = participantOpt.get();
                        Integer shares = participant.getShares() != null ? participant.getShares() : 1;
                        long debtAmount = o.getPerAmount() * shares;

                        DebtorDebtDetailItem d = new DebtorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(debtAmount);
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());

                        if (o.getStyleId() != null) {
                            PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                            d.setCategoryName(style != null ? style.getStyleName() : "未分类");
                        } else {
                            d.setCategoryName("未分类");
                        }
                        d.setLocationText("位置信息");

                        details.add(d);
                    }
                }

                // 还款明细 - 对方还给我
                if (o.getRepayFlag() == (byte) 2) {

                    if (o.getPayerUserid().equals(debtorId) && o.getTargetUserid().equals(userId)) {

                        DebtorDebtDetailItem d = new DebtorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(-o.getAmount());
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());
                        d.setCategoryName("还款");
                        d.setLocationText("无");

                        details.add(d);
                    }
                }
            }

            // V19: 如果有互相欠款，添加抵消记录
            String reverseKey = userId + "-" + debtorId; // 我欠对方的金额
            Long iOweThemRaw = rawMap.getOrDefault(reverseKey, 0L);
            if (iOweThemRaw > 0) {
                DebtorDebtDetailItem offsetItem = new DebtorDebtDetailItem();
                offsetItem.setOutcomeId(-1); // 特殊ID表示虚拟记录
                offsetItem.setAmount(-iOweThemRaw);
                offsetItem.setComment("互相欠款抵消（我欠Ta的部分）");
                offsetItem.setPayDatetime(java.time.LocalDateTime.now());
                offsetItem.setCategoryName("抵消");
                offsetItem.setLocationText("无");
                details.add(offsetItem);
            }

            item.setDetails(details);
            list.add(item);
        }

        return list;
    }

    @Override
    public List<MyDebtOverviewItem> getMyDebtOverview(Integer userId) {

        Map<String, Long> netMap = buildDebtMap();
        Map<String, Long> rawMap = buildRawDebtMap();

        // 找出我欠钱的所有债权人（使用净额）
        Map<Integer, Long> creditorMap = new HashMap<>();

        for (Map.Entry<String, Long> e : netMap.entrySet()) {

            String[] parts = e.getKey().split("-");
            Integer debtor = Integer.valueOf(parts[0]);
            Integer creditor = Integer.valueOf(parts[1]);
            Long amount = e.getValue();

            // 我是欠款人，且金额大于0
            if (amount > 0 && debtor.equals(userId)) {
                creditorMap.put(creditor, creditorMap.getOrDefault(creditor, 0L) + amount);
            }
        }

        List<MyDebtOverviewItem> list = new ArrayList<>();

        List<Outcome> all = loadAllOutcomes();

        for (Map.Entry<Integer, Long> e : creditorMap.entrySet()) {

            Integer creditorId = e.getKey();
            Long totalAmount = e.getValue();

            User creditor = userMapper.selectByPrimaryKey(creditorId);

            MyDebtOverviewItem item = new MyDebtOverviewItem();
            item.setCreditorId(creditorId);
            item.setCreditorName(creditor.getNickname());
            item.setCreditorAvatarUrl(creditor.getAvatarUrl());
            item.setTotalAmount(totalAmount);

            // 添加债权人支持的收款方式
            item.setPaypaySupported(creditor.getPaypayFlag() != null && creditor.getPaypayFlag() == 1);
            item.setBankSupported(creditor.getBankFlag() != null && creditor.getBankFlag() == 1);

            List<CreditorDebtDetailItem> details = new ArrayList<>();

            for (Outcome o : all) {

                // 消费明细 - 债权人付款，我是参与者
                if (o.getRepayFlag() == (byte) 1) {

                    List<OutcomeParticipant> ps = loadParticipants(o.getId());
                    Optional<OutcomeParticipant> participantOpt = ps.stream()
                        .filter(p -> p.getUserId().equals(userId))
                        .findFirst();

                    if (participantOpt.isPresent() && o.getPayerUserid().equals(creditorId)) {

                        // V18: 使用份额计算金额
                        OutcomeParticipant participant = participantOpt.get();
                        Integer shares = participant.getShares() != null ? participant.getShares() : 1;
                        long debtAmount = o.getPerAmount() * shares;

                        CreditorDebtDetailItem d = new CreditorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(debtAmount);
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());

                        if (o.getStyleId() != null) {
                            PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                            d.setCategoryName(style != null ? style.getStyleName() : "未分类");
                        } else {
                            d.setCategoryName("未分类");
                        }
                        d.setLocationText("位置信息");

                        details.add(d);
                    }
                }

                // 还款明细 - 我还给债权人
                if (o.getRepayFlag() == (byte) 2) {

                    if (o.getPayerUserid().equals(userId) && o.getTargetUserid().equals(creditorId)) {

                        CreditorDebtDetailItem d = new CreditorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(-o.getAmount());
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());
                        d.setCategoryName("还款");
                        d.setLocationText("无");

                        details.add(d);
                    }
                }
            }

            // V19: 如果有互相欠款，添加抵消记录
            String reverseKey = creditorId + "-" + userId; // 对方欠我的金额
            Long theyOweMeRaw = rawMap.getOrDefault(reverseKey, 0L);
            if (theyOweMeRaw > 0) {
                CreditorDebtDetailItem offsetItem = new CreditorDebtDetailItem();
                offsetItem.setOutcomeId(-1); // 特殊ID表示虚拟记录
                offsetItem.setAmount(-theyOweMeRaw);
                offsetItem.setComment("互相欠款抵消（Ta欠我的部分）");
                offsetItem.setPayDatetime(java.time.LocalDateTime.now());
                offsetItem.setCategoryName("抵消");
                offsetItem.setLocationText("无");
                details.add(offsetItem);
            }

            item.setDetails(details);
            list.add(item);
        }

        return list;
    }
}
