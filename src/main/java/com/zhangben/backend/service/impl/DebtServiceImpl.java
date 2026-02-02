package com.zhangben.backend.service.impl;

import com.zhangben.backend.dto.*;
import com.zhangben.backend.mapper.NotificationMapper;
import com.zhangben.backend.mapper.OutcomeMapper;
import com.zhangben.backend.mapper.OutcomeParticipantMapper;
import com.zhangben.backend.mapper.PayStyleMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.*;
import com.zhangben.backend.service.DebtService;
import com.zhangben.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private EmailService emailService;

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
            // V30: 只有已确认的还款才计入
            if (o.getRepayFlag() == (byte) 2) {

                Integer debtor = o.getPayerUserid();
                Integer creditor = o.getTargetUserid();

                // 检查还款是否已确认
                OutcomeParticipant participant = outcomeParticipantMapper.selectByOutcomeAndUser(o.getId(), creditor);
                boolean isConfirmed = participant != null && participant.getConfirmStatus() != null && participant.getConfirmStatus() == 1;

                // 只有已确认的还款才计入
                if (isConfirmed) {
                    String key = debtor + "-" + creditor;
                    map.put(key, map.getOrDefault(key, 0L) - o.getAmount());
                }
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
    @Transactional
    public void repay(RepayRequest req, Integer currentUserId) {

        Outcome o = new Outcome();
        o.setAmount(req.getAmount());
        o.setPayerUserid(currentUserId);
        o.setCreatorId(currentUserId); // V29: 设置创建者
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

        // V30: 为还款创建参与者记录，状态为待确认
        OutcomeParticipant participant = new OutcomeParticipant();
        participant.setOutcomeId(o.getId());
        participant.setUserId(req.getCreditorId());
        participant.setShares(1);
        participant.setConfirmStatus((byte) 0); // 待确认
        outcomeParticipantMapper.insertSelective(participant);

        // V31: 发送通知给债权人（站内信）
        User debtor = userMapper.selectByPrimaryKey(currentUserId);
        User creditor = userMapper.selectByPrimaryKey(req.getCreditorId());

        try {
            Notification notification = new Notification();
            notification.setUserId(req.getCreditorId());
            notification.setType("repayment_received");
            notification.setTitle(debtor.getNickname() + " 向你还款");
            notification.setContent("金额: ¥" + String.format("%.2f", req.getAmount() / 100.0) + (req.getComment() != null ? " - " + req.getComment() : "") + "。请确认收款。");
            notification.setRelatedId(Long.valueOf(o.getId()));
            notification.setRelatedType("repayment");
            notification.setIsRead((byte) 0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insertSelective(notification);
        } catch (Exception e) {
            // 站内信发送失败不影响业务
        }

        // V32: 发送邮件通知给债权人（异步，失败不影响业务）- 提醒确认收款
        try {
            if (creditor != null && creditor.getEmail() != null) {
                String language = creditor.getPreferredLanguage() != null ? creditor.getPreferredLanguage() : "zh-CN";
                emailService.sendBillNotificationAsync(
                    creditor.getEmail(),
                    creditor.getNickname(),
                    language,
                    debtor.getNickname(),
                    req.getAmount(),
                    req.getAmount(), // perAmount 等于 amount
                    "收到还款，请确认: " + (req.getComment() != null ? req.getComment() : ""),
                    "待确认还款",
                    null,
                    false
                );
            }
        } catch (Exception e) {
            // 邮件发送失败不影响业务
        }
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
            long pendingAmount = 0L; // V32: 累计待确认金额

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
                        d.setIsRepayment(true);

                        // V32: 获取还款确认状态
                        OutcomeParticipant repayParticipant = outcomeParticipantMapper.selectByOutcomeAndUser(o.getId(), creditorId);
                        if (repayParticipant != null) {
                            int status = repayParticipant.getConfirmStatus() != null ? repayParticipant.getConfirmStatus().intValue() : 0;
                            d.setConfirmStatus(status);
                            // V32: 累计待确认金额
                            if (status == 0) {
                                pendingAmount += o.getAmount();
                            }
                        } else {
                            d.setConfirmStatus(0); // 默认待确认
                            pendingAmount += o.getAmount();
                        }

                        details.add(d);
                    }
                }
            }

            // V32: 设置待确认金额
            item.setPendingAmount(pendingAmount);

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

    @Override
    public List<PendingRepaymentItem> getPendingRepayments(Integer creditorId) {
        List<PendingRepaymentItem> result = new ArrayList<>();

        // 查询所有待确认的还款记录
        List<OutcomeParticipant> pendingParticipants = outcomeParticipantMapper.selectPendingConfirmations(creditorId);

        for (OutcomeParticipant p : pendingParticipants) {
            Outcome repayment = outcomeMapper.selectByPrimaryKey(p.getOutcomeId());
            if (repayment == null || repayment.getDeletedFlag() == 1) {
                continue;
            }

            User debtor = userMapper.selectByPrimaryKey(repayment.getPayerUserid());
            if (debtor == null) {
                continue;
            }

            PendingRepaymentItem item = new PendingRepaymentItem();
            item.setRepaymentId(repayment.getId());
            item.setDebtorId(debtor.getId());
            item.setDebtorName(debtor.getNickname());
            item.setDebtorAvatarUrl(debtor.getAvatarUrl());
            item.setAmount(repayment.getAmount());
            item.setComment(repayment.getComment());
            item.setPayDatetime(repayment.getPayDatetime());

            result.add(item);
        }

        return result;
    }

    @Override
    @Transactional
    public void confirmRepayment(Integer repaymentId, Integer creditorId) {
        // 查询还款记录
        Outcome repayment = outcomeMapper.selectByPrimaryKey(repaymentId);
        if (repayment == null || repayment.getDeletedFlag() == 1) {
            throw new IllegalArgumentException("还款记录不存在");
        }

        // 检查是否是该债权人的还款
        if (!repayment.getTargetUserid().equals(creditorId)) {
            throw new IllegalArgumentException("无权确认此还款");
        }

        // 查询并更新参与者记录
        OutcomeParticipant participant = outcomeParticipantMapper.selectByOutcomeAndUser(repaymentId, creditorId);
        if (participant == null) {
            throw new IllegalArgumentException("还款参与记录不存在");
        }

        if (participant.getConfirmStatus() != null && participant.getConfirmStatus() == 1) {
            throw new IllegalArgumentException("该还款已确认");
        }

        // 更新确认状态
        participant.setConfirmStatus((byte) 1);
        participant.setConfirmedAt(LocalDateTime.now());
        participant.setConfirmedBy(creditorId);
        outcomeParticipantMapper.updateByPrimaryKeySelective(participant);

        // V31: 发送确认通知给还款人（站内信）
        User creditor = userMapper.selectByPrimaryKey(creditorId);
        User debtor = userMapper.selectByPrimaryKey(repayment.getPayerUserid());

        try {
            Notification notification = new Notification();
            notification.setUserId(repayment.getPayerUserid());
            notification.setType("repayment_confirmed");
            notification.setTitle(creditor.getNickname() + " 确认收到还款");
            notification.setContent("金额: ¥" + String.format("%.2f", repayment.getAmount() / 100.0));
            notification.setRelatedId(Long.valueOf(repaymentId));
            notification.setRelatedType("repayment");
            notification.setIsRead((byte) 0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insertSelective(notification);
        } catch (Exception e) {
            // 站内信发送失败不影响业务
        }

        // V32: 发送确认邮件给还款人（异步，失败不影响业务）
        try {
            if (debtor != null && debtor.getEmail() != null) {
                String language = debtor.getPreferredLanguage() != null ? debtor.getPreferredLanguage() : "zh-CN";
                emailService.sendBillNotificationAsync(
                    debtor.getEmail(),
                    debtor.getNickname(),
                    language,
                    creditor.getNickname(),
                    repayment.getAmount(),
                    repayment.getAmount(), // perAmount 等于 amount
                    "还款确认通知: " + (repayment.getComment() != null ? repayment.getComment() : "还款已确认"),
                    "还款确认",
                    null,
                    false // 不是更新通知
                );
            }
        } catch (Exception e) {
            // 邮件发送失败不影响业务
        }
    }

    @Override
    @Transactional
    public void batchConfirmRepayments(List<Integer> repaymentIds, Integer creditorId) {
        for (Integer repaymentId : repaymentIds) {
            try {
                confirmRepayment(repaymentId, creditorId);
            } catch (IllegalArgumentException e) {
                // 跳过无效的还款ID，继续处理其他的
            }
        }
    }

    @Override
    public List<MyPendingRepaymentItem> getMyPendingRepayments(Integer debtorId) {
        List<MyPendingRepaymentItem> result = new ArrayList<>();

        // 查询我发起的所有还款记录
        OutcomeExample example = new OutcomeExample();
        example.createCriteria()
            .andPayerUseridEqualTo(debtorId)
            .andRepayFlagEqualTo((byte) 2)
            .andDeletedFlagEqualTo((byte) 0);
        example.setOrderByClause("pay_datetime DESC");

        List<Outcome> repayments = outcomeMapper.selectByExample(example);

        for (Outcome repayment : repayments) {
            // 检查确认状态
            OutcomeParticipant participant = outcomeParticipantMapper.selectByOutcomeAndUser(
                repayment.getId(), repayment.getTargetUserid());

            // 只返回待确认的
            if (participant != null && participant.getConfirmStatus() != null && participant.getConfirmStatus() == 1) {
                continue; // 已确认，跳过
            }

            User creditor = userMapper.selectByPrimaryKey(repayment.getTargetUserid());
            if (creditor == null) {
                continue;
            }

            MyPendingRepaymentItem item = new MyPendingRepaymentItem();
            item.setRepaymentId(repayment.getId());
            item.setCreditorId(creditor.getId());
            item.setCreditorName(creditor.getNickname());
            item.setCreditorAvatarUrl(creditor.getAvatarUrl());
            item.setAmount(repayment.getAmount());
            item.setComment(repayment.getComment());
            item.setPayDatetime(repayment.getPayDatetime());

            result.add(item);
        }

        return result;
    }
}
