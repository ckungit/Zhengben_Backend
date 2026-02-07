package com.zhangben.backend.service.impl;

import com.zhangben.backend.dto.*;
import com.zhangben.backend.mapper.ActivityMapper;
import com.zhangben.backend.mapper.ActivityMemberMapper;
import com.zhangben.backend.mapper.FavoredUserMapper;
import com.zhangben.backend.mapper.NotificationMapper;
import com.zhangben.backend.mapper.OutcomeMapper;
import com.zhangben.backend.mapper.OutcomeParticipantMapper;
import com.zhangben.backend.mapper.PayStyleMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.*;
import com.zhangben.backend.service.DebtService;
import com.zhangben.backend.service.EmailService;
import com.zhangben.backend.service.UserPaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private FavoredUserMapper favoredUserMapper;

    @Autowired
    private UserPaymentMethodService userPaymentMethodService;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityMemberMapper activityMemberMapper;

    /**
     * 使用 Example 查询所有未删除的 outcome
     */
    private List<Outcome> loadAllOutcomes() {
        OutcomeExample example = new OutcomeExample();
        example.createCriteria().andDeletedFlagEqualTo((byte) 0);
        return outcomeMapper.selectByExample(example);
    }

    /**
     * V49: 加载一般模式的 outcome（排除活动账单）
     */
    private List<Outcome> loadGeneralOutcomes() {
        return loadAllOutcomes().stream()
            .filter(o -> o.getActivityId() == null || o.getActivityId() == 0)
            .collect(Collectors.toList());
    }

    /**
     * V49: 加载指定活动的 outcome
     */
    private List<Outcome> loadActivityOutcomes(Integer activityId) {
        return loadAllOutcomes().stream()
            .filter(o -> activityId.equals(o.getActivityId()))
            .collect(Collectors.toList());
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
     * V49: 使用一般模式 outcomes（排除活动账单）
     */
    private Map<String, Long> buildRawDebtMap() {
        return buildRawDebtMapFrom(loadGeneralOutcomes());
    }

    /**
     * V49: 从指定的 outcome 列表构建原始欠款表
     */
    private Map<String, Long> buildRawDebtMapFrom(List<Outcome> list) {

        Map<String, Long> map = new HashMap<>();

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

                // V35: 使用 onBehalfOf 确定真正的债务人（债务归属人）
                // 如果是代还，onBehalfOf 是被代还的好友；否则是付款人自己
                Integer debtor = o.getOnBehalfOf() != null ? o.getOnBehalfOf() : o.getPayerUserid();
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
     * V49: 从指定 outcome 列表构建净额欠款表
     */
    private Map<String, Long> buildDebtMapFrom(List<Outcome> outcomes) {
        Map<String, Long> rawMap = buildRawDebtMapFrom(outcomes);
        return netFromRaw(rawMap);
    }

    /**
     * V49: 从原始欠款表计算净额
     */
    private Map<String, Long> netFromRaw(Map<String, Long> rawMap) {
        Map<String, Long> netMap = new HashMap<>();
        Set<String> processed = new HashSet<>();

        for (Map.Entry<String, Long> e : rawMap.entrySet()) {
            String[] parts = e.getKey().split("-");
            Integer userA = Integer.valueOf(parts[0]);
            Integer userB = Integer.valueOf(parts[1]);

            String pairKey = userA < userB ? userA + ":" + userB : userB + ":" + userA;
            if (processed.contains(pairKey)) continue;
            processed.add(pairKey);

            String keyAB = userA + "-" + userB;
            Long amountAB = rawMap.getOrDefault(keyAB, 0L);
            String keyBA = userB + "-" + userA;
            Long amountBA = rawMap.getOrDefault(keyBA, 0L);

            long net = amountAB - amountBA;
            if (net > 0) {
                netMap.put(keyAB, net);
            } else if (net < 0) {
                netMap.put(keyBA, -net);
            }
        }
        return netMap;
    }

    /**
     * V19: 构建净额欠款表（一般模式，排除活动账单）
     * 当A欠B 300元，B欠A 200元时，计算净额：A欠B 100元
     */
    private Map<String, Long> buildDebtMap() {

        Map<String, Long> rawMap = buildRawDebtMap();
        return netFromRaw(rawMap);
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

        List<Outcome> all = loadGeneralOutcomes();
        List<CreditorDebtDetailItem> details = new ArrayList<>();
        List<CreditorDebtDetailItem> offsetDetails = new ArrayList<>();

        for (Outcome o : all) {

            // 消费明细
            if (o.getRepayFlag() == (byte) 1) {

                List<OutcomeParticipant> ps = loadParticipants(o.getId());

                // 我参与、对方付款 → 我欠对方
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
                    item.setCurrency(o.getTargetCurrencySnapshot());
                    item.setOriginalAmount(o.getOriginalAmount());
                    item.setOriginalCurrency(o.getOriginalCurrency());

                    details.add(item);
                }

                // 对方参与、我付款 → 对方欠我（抵消明细）
                if (o.getPayerUserid().equals(userId)) {
                    Optional<OutcomeParticipant> offsetOpt = ps.stream()
                        .filter(p -> p.getUserId().equals(creditorId))
                        .findFirst();
                    if (offsetOpt.isPresent()) {
                        OutcomeParticipant participant = offsetOpt.get();
                        Integer shares = participant.getShares() != null ? participant.getShares() : 1;
                        long debtAmount = o.getPerAmount() * shares;

                        CreditorDebtDetailItem item = new CreditorDebtDetailItem();
                        item.setOutcomeId(o.getId());
                        item.setAmount(-debtAmount);
                        item.setComment(o.getComment());
                        item.setPayDatetime(o.getPayDatetime());
                        item.setIsOffset(true);

                        if (o.getStyleId() != null) {
                            PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                            item.setCategoryName(style != null ? style.getStyleName() : "未分类");
                        } else {
                            item.setCategoryName("未分类");
                        }
                        item.setLocationText("无");
                        item.setCurrency(o.getTargetCurrencySnapshot());
                        item.setOriginalAmount(o.getOriginalAmount());
                        item.setOriginalCurrency(o.getOriginalCurrency());

                        offsetDetails.add(item);
                    }
                }
            }

            // 还款明细
            if (o.getRepayFlag() == (byte) 2) {

                // V35: 使用 onBehalfOf 确定真正的债务人
                Integer actualDebtor = o.getOnBehalfOf() != null ? o.getOnBehalfOf() : o.getPayerUserid();

                // 我还给对方
                if (actualDebtor.equals(userId) && o.getTargetUserid().equals(creditorId)) {

                    CreditorDebtDetailItem item = new CreditorDebtDetailItem();
                    item.setOutcomeId(o.getId());
                    item.setAmount(-o.getAmount());
                    item.setComment(o.getComment());
                    item.setPayDatetime(o.getPayDatetime());
                    item.setCategoryName("还款");
                    item.setLocationText("无");
                    item.setCurrency(o.getTargetCurrencySnapshot());
                    item.setOriginalAmount(o.getOriginalAmount());
                    item.setOriginalCurrency(o.getOriginalCurrency());

                    details.add(item);
                }

                // 对方还给我（减少抵消）
                if (actualDebtor.equals(creditorId) && o.getTargetUserid().equals(userId)) {
                    OutcomeParticipant rp = outcomeParticipantMapper.selectByOutcomeAndUser(o.getId(), userId);
                    boolean isConfirmed = rp != null && rp.getConfirmStatus() != null && rp.getConfirmStatus() == 1;
                    if (isConfirmed) {
                        CreditorDebtDetailItem item = new CreditorDebtDetailItem();
                        item.setOutcomeId(o.getId());
                        item.setAmount(o.getAmount());
                        item.setComment(o.getComment());
                        item.setPayDatetime(o.getPayDatetime());
                        item.setCategoryName("还款");
                        item.setIsOffset(true);
                        item.setLocationText("无");
                        item.setCurrency(o.getTargetCurrencySnapshot());
                        item.setOriginalAmount(o.getOriginalAmount());
                        item.setOriginalCurrency(o.getOriginalCurrency());

                        offsetDetails.add(item);
                    }
                }
            }
        }

        // V19: 如果有互相欠款，添加抵消明细记录
        String reverseKey = creditorId + "-" + userId;
        Long theyOweMeRaw = rawMap.getOrDefault(reverseKey, 0L);
        if (theyOweMeRaw > 0) {
            details.addAll(offsetDetails);
        }

        resp.setDetails(details);
        return resp;
    }

    @Override
    @Transactional
    public void repay(RepayRequest req, Integer currentUserId) {

        // 判断是否债权人录入
        boolean isCreditorEntry = req.getDebtorId() != null && currentUserId.equals(req.getCreditorId());

        Integer actualDebtor = isCreditorEntry ? req.getDebtorId() : currentUserId;
        Integer actualCreditor = req.getCreditorId();

        Outcome o = new Outcome();
        o.setAmount(req.getAmount());
        o.setPayerUserid(actualDebtor);        // 真正的债务人
        o.setCreatorId(currentUserId);         // 谁创建的记录
        o.setTargetUserid(actualCreditor);     // 债权人
        o.setRepayFlag((byte) 2);
        o.setPerAmount(req.getAmount());
        // style_id 如果没有传，设置为0或者一个默认值
        o.setStyleId(req.getStyleId() != null ? req.getStyleId() : 0);
        o.setComment(req.getComment());
        o.setDeletedFlag((byte) 0);

        // V49: 活动还款时设置 activityId
        if (req.getActivityId() != null) {
            o.setActivityId(req.getActivityId());
        }

        // V20: 支持自定义还款时间
        LocalDateTime payTime = req.getPayDatetime() != null ? req.getPayDatetime() : LocalDateTime.now();
        o.setPayDatetime(payTime);

        outcomeMapper.insertSelective(o);

        // V30: 为还款创建参与者记录 — 债权人录入自动确认
        OutcomeParticipant participant = new OutcomeParticipant();
        participant.setOutcomeId(o.getId());
        participant.setUserId(actualCreditor);
        participant.setShares(1);
        participant.setConfirmStatus(isCreditorEntry ? (byte) 1 : (byte) 0);
        if (isCreditorEntry) {
            participant.setConfirmedAt(LocalDateTime.now());
            participant.setConfirmedBy(currentUserId);
        }
        outcomeParticipantMapper.insertSelective(participant);

        User creator = userMapper.selectByPrimaryKey(currentUserId);
        User debtor = isCreditorEntry ? userMapper.selectByPrimaryKey(actualDebtor) : creator;
        User creditor = isCreditorEntry ? creator : userMapper.selectByPrimaryKey(actualCreditor);

        if (isCreditorEntry) {
            // 债权人录入 → 通知债务人
            try {
                Notification notification = new Notification();
                notification.setUserId(actualDebtor);
                notification.setType("repayment_confirmed");
                notification.setTitle(creditor.getNickname() + " 已记录收款");
                notification.setContent("金额: ¥" + String.format("%.2f", req.getAmount() / 100.0) + (req.getComment() != null ? " - " + req.getComment() : "") + "。已自动确认。");
                notification.setRelatedId(Long.valueOf(o.getId()));
                notification.setRelatedType("repayment");
                notification.setIsRead((byte) 0);
                notification.setCreatedAt(LocalDateTime.now());
                notificationMapper.insertSelective(notification);
            } catch (Exception e) {
                // 站内信发送失败不影响业务
            }
        } else {
            // 债务人录入 → 通知债权人（现有逻辑）
            try {
                Notification notification = new Notification();
                notification.setUserId(actualCreditor);
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

        List<Outcome> all = loadGeneralOutcomes();

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
            List<DebtorDebtDetailItem> offsetDetails = new ArrayList<>();

            for (Outcome o : all) {

                // 消费明细
                if (o.getRepayFlag() == (byte) 1) {

                    List<OutcomeParticipant> ps = loadParticipants(o.getId());

                    // 我付款，对方参与 → 对方欠我
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
                        d.setCurrency(o.getTargetCurrencySnapshot());
                        d.setOriginalAmount(o.getOriginalAmount());
                        d.setOriginalCurrency(o.getOriginalCurrency());

                        details.add(d);
                    }

                    // 对方付款，我参与 → 我欠对方（抵消明细）
                    if (o.getPayerUserid().equals(debtorId)) {
                        Optional<OutcomeParticipant> offsetOpt = ps.stream()
                            .filter(p -> p.getUserId().equals(userId))
                            .findFirst();
                        if (offsetOpt.isPresent()) {
                            OutcomeParticipant participant = offsetOpt.get();
                            Integer shares = participant.getShares() != null ? participant.getShares() : 1;
                            long debtAmount = o.getPerAmount() * shares;

                            DebtorDebtDetailItem d = new DebtorDebtDetailItem();
                            d.setOutcomeId(o.getId());
                            d.setAmount(-debtAmount);
                            d.setComment(o.getComment());
                            d.setPayDatetime(o.getPayDatetime());
                            d.setIsOffset(true);

                            if (o.getStyleId() != null) {
                                PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                                d.setCategoryName(style != null ? style.getStyleName() : "未分类");
                            } else {
                                d.setCategoryName("未分类");
                            }
                            d.setLocationText("无");
                            d.setCurrency(o.getTargetCurrencySnapshot());
                            d.setOriginalAmount(o.getOriginalAmount());
                            d.setOriginalCurrency(o.getOriginalCurrency());

                            offsetDetails.add(d);
                        }
                    }
                }

                // 还款明细 - 对方还给我
                if (o.getRepayFlag() == (byte) 2) {

                    // V35: 使用 onBehalfOf 确定真正的债务人
                    Integer actualDebtor = o.getOnBehalfOf() != null ? o.getOnBehalfOf() : o.getPayerUserid();

                    // 对方还给我
                    if (actualDebtor.equals(debtorId) && o.getTargetUserid().equals(userId)) {

                        DebtorDebtDetailItem d = new DebtorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(-o.getAmount());
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());
                        d.setCategoryName("还款");
                        d.setLocationText("无");
                        d.setCurrency(o.getTargetCurrencySnapshot());
                        d.setOriginalAmount(o.getOriginalAmount());
                        d.setOriginalCurrency(o.getOriginalCurrency());

                        details.add(d);
                    }

                    // 我还给对方（减少抵消）
                    if (actualDebtor.equals(userId) && o.getTargetUserid().equals(debtorId)) {
                        OutcomeParticipant rp = outcomeParticipantMapper.selectByOutcomeAndUser(o.getId(), debtorId);
                        boolean isConfirmed = rp != null && rp.getConfirmStatus() != null && rp.getConfirmStatus() == 1;
                        if (isConfirmed) {
                            DebtorDebtDetailItem d = new DebtorDebtDetailItem();
                            d.setOutcomeId(o.getId());
                            d.setAmount(o.getAmount());
                            d.setComment(o.getComment());
                            d.setPayDatetime(o.getPayDatetime());
                            d.setCategoryName("还款");
                            d.setIsOffset(true);
                            d.setLocationText("无");
                            d.setCurrency(o.getTargetCurrencySnapshot());
                            d.setOriginalAmount(o.getOriginalAmount());
                            d.setOriginalCurrency(o.getOriginalCurrency());

                            offsetDetails.add(d);
                        }
                    }
                }
            }

            // V19: 如果有互相欠款，添加抵消明细记录
            String reverseKey = userId + "-" + debtorId;
            Long iOweThemRaw = rawMap.getOrDefault(reverseKey, 0L);
            if (iOweThemRaw > 0) {
                details.addAll(offsetDetails);
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

        List<Outcome> all = loadGeneralOutcomes();

        for (Map.Entry<Integer, Long> e : creditorMap.entrySet()) {

            Integer creditorId = e.getKey();
            Long totalAmount = e.getValue();

            User creditor = userMapper.selectByPrimaryKey(creditorId);

            MyDebtOverviewItem item = new MyDebtOverviewItem();
            item.setCreditorId(creditorId);
            item.setCreditorName(creditor.getNickname());
            item.setCreditorAvatarUrl(creditor.getAvatarUrl());
            item.setTotalAmount(totalAmount);

            // 添加债权人支持的收款方式（旧字段）
            item.setPaypaySupported(creditor.getPaypayFlag() != null && creditor.getPaypayFlag() == 1);
            item.setBankSupported(creditor.getBankFlag() != null && creditor.getBankFlag() == 1);

            // V39: 添加债权人的支付方式列表和主要货币
            item.setPrimaryCurrency(creditor.getPrimaryCurrency());
            List<UserPaymentMethodDto> enabledMethods = userPaymentMethodService.getEnabledMethods(creditorId);
            List<String> methodCodes = enabledMethods.stream()
                    .map(UserPaymentMethodDto::getMethodCode)
                    .collect(Collectors.toList());
            item.setPaymentMethods(methodCodes);

            List<CreditorDebtDetailItem> details = new ArrayList<>();
            List<CreditorDebtDetailItem> offsetDetails = new ArrayList<>();
            long pendingAmount = 0L; // V32: 累计待确认金额

            for (Outcome o : all) {

                // 消费明细
                if (o.getRepayFlag() == (byte) 1) {

                    List<OutcomeParticipant> ps = loadParticipants(o.getId());

                    // 债权人付款，我是参与者 → 我欠债权人
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
                        d.setCurrency(o.getTargetCurrencySnapshot());
                        d.setOriginalAmount(o.getOriginalAmount());
                        d.setOriginalCurrency(o.getOriginalCurrency());

                        details.add(d);
                    }

                    // 我付款，债权人参与 → 债权人欠我（抵消明细）
                    if (o.getPayerUserid().equals(userId)) {
                        Optional<OutcomeParticipant> offsetOpt = ps.stream()
                            .filter(p -> p.getUserId().equals(creditorId))
                            .findFirst();
                        if (offsetOpt.isPresent()) {
                            OutcomeParticipant participant = offsetOpt.get();
                            Integer shares = participant.getShares() != null ? participant.getShares() : 1;
                            long debtAmount = o.getPerAmount() * shares;

                            CreditorDebtDetailItem d = new CreditorDebtDetailItem();
                            d.setOutcomeId(o.getId());
                            d.setAmount(-debtAmount);
                            d.setComment(o.getComment());
                            d.setPayDatetime(o.getPayDatetime());
                            d.setIsOffset(true);

                            if (o.getStyleId() != null) {
                                PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                                d.setCategoryName(style != null ? style.getStyleName() : "未分类");
                            } else {
                                d.setCategoryName("未分类");
                            }
                            d.setLocationText("无");
                            d.setCurrency(o.getTargetCurrencySnapshot());
                            d.setOriginalAmount(o.getOriginalAmount());
                            d.setOriginalCurrency(o.getOriginalCurrency());

                            offsetDetails.add(d);
                        }
                    }
                }

                // 还款明细 - 我的债务被还款（可能是我自己还，也可能是别人代我还）
                if (o.getRepayFlag() == (byte) 2) {

                    // V35: 使用 onBehalfOf 确定真正的债务人
                    Integer actualDebtor = o.getOnBehalfOf() != null ? o.getOnBehalfOf() : o.getPayerUserid();

                    // 我还给债权人
                    if (actualDebtor.equals(userId) && o.getTargetUserid().equals(creditorId)) {

                        CreditorDebtDetailItem d = new CreditorDebtDetailItem();
                        d.setOutcomeId(o.getId());
                        d.setAmount(-o.getAmount());
                        d.setComment(o.getComment());
                        d.setPayDatetime(o.getPayDatetime());

                        // V35: 显示是否为代还
                        boolean isOnBehalf = o.getOnBehalfOf() != null && !o.getOnBehalfOf().equals(o.getPayerUserid());
                        if (isOnBehalf) {
                            User payer = userMapper.selectByPrimaryKey(o.getPayerUserid());
                            d.setCategoryName("还款 (由" + (payer != null ? payer.getNickname() : "他人") + "代付)");
                        } else {
                            d.setCategoryName("还款");
                        }
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

                        d.setCurrency(o.getTargetCurrencySnapshot());
                        d.setOriginalAmount(o.getOriginalAmount());
                        d.setOriginalCurrency(o.getOriginalCurrency());

                        details.add(d);
                    }

                    // 债权人还给我（减少抵消）
                    if (actualDebtor.equals(creditorId) && o.getTargetUserid().equals(userId)) {
                        OutcomeParticipant rp = outcomeParticipantMapper.selectByOutcomeAndUser(o.getId(), userId);
                        boolean isConfirmed = rp != null && rp.getConfirmStatus() != null && rp.getConfirmStatus() == 1;
                        if (isConfirmed) {
                            CreditorDebtDetailItem d = new CreditorDebtDetailItem();
                            d.setOutcomeId(o.getId());
                            d.setAmount(o.getAmount());
                            d.setComment(o.getComment());
                            d.setPayDatetime(o.getPayDatetime());
                            d.setCategoryName("还款");
                            d.setIsOffset(true);
                            d.setLocationText("无");
                            d.setCurrency(o.getTargetCurrencySnapshot());
                            d.setOriginalAmount(o.getOriginalAmount());
                            d.setOriginalCurrency(o.getOriginalCurrency());

                            offsetDetails.add(d);
                        }
                    }
                }
            }

            // V32: 设置待确认金额
            item.setPendingAmount(pendingAmount);

            // V19: 如果有互相欠款，添加抵消明细记录
            String reverseKey = creditorId + "-" + userId;
            Long theyOweMeRaw = rawMap.getOrDefault(reverseKey, 0L);
            if (theyOweMeRaw > 0) {
                details.addAll(offsetDetails);
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

            // V35: 使用 repaidBy 获取实际付款人
            Integer repaidById = repayment.getRepaidBy() != null ?
                repayment.getRepaidBy() : repayment.getPayerUserid();
            User payer = userMapper.selectByPrimaryKey(repaidById);
            if (payer == null) {
                continue;
            }

            PendingRepaymentItem item = new PendingRepaymentItem();
            item.setRepaymentId(repayment.getId());
            item.setDebtorId(payer.getId());
            item.setDebtorName(payer.getNickname());
            item.setDebtorAvatarUrl(payer.getAvatarUrl());
            item.setAmount(repayment.getAmount());
            item.setComment(repayment.getComment());
            item.setPayDatetime(repayment.getPayDatetime());

            // V35: 设置代还信息
            item.setRepaidById(repaidById);
            item.setRepaidByName(payer.getNickname());

            Integer onBehalfOfId = repayment.getOnBehalfOf() != null ?
                repayment.getOnBehalfOf() : repayment.getPayerUserid();
            item.setOnBehalfOfId(onBehalfOfId);

            // 判断是否为代还
            boolean isOnBehalf = !repaidById.equals(onBehalfOfId);
            item.setIsOnBehalf(isOnBehalf);

            if (isOnBehalf) {
                User beneficiary = userMapper.selectByPrimaryKey(onBehalfOfId);
                if (beneficiary != null) {
                    item.setOnBehalfOfName(beneficiary.getNickname());
                }
            } else {
                item.setOnBehalfOfName(payer.getNickname());
            }

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

        // V35: 获取付款人和被代还人信息
        User creditor = userMapper.selectByPrimaryKey(creditorId);
        Integer repaidById = repayment.getRepaidBy() != null ?
            repayment.getRepaidBy() : repayment.getPayerUserid();
        Integer onBehalfOfId = repayment.getOnBehalfOf() != null ?
            repayment.getOnBehalfOf() : repayment.getPayerUserid();

        User payer = userMapper.selectByPrimaryKey(repaidById);
        boolean isOnBehalf = !repaidById.equals(onBehalfOfId);
        User beneficiary = isOnBehalf ? userMapper.selectByPrimaryKey(onBehalfOfId) : null;

        // V31/V35: 发送确认通知给付款人（站内信）
        try {
            Notification notification = new Notification();
            notification.setUserId(repaidById);
            notification.setType("repayment_confirmed");
            notification.setTitle(creditor.getNickname() + " 确认收到还款");
            String content = "金额: ¥" + String.format("%.2f", repayment.getAmount() / 100.0);
            if (isOnBehalf && beneficiary != null) {
                content += " (代 " + beneficiary.getNickname() + " 还款)";
            }
            notification.setContent(content);
            notification.setRelatedId(Long.valueOf(repaymentId));
            notification.setRelatedType("repayment");
            notification.setIsRead((byte) 0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insertSelective(notification);
        } catch (Exception e) {
            // 站内信发送失败不影响业务
        }

        // V35: 如果是代还，还要通知被代还人
        if (isOnBehalf && beneficiary != null) {
            try {
                Notification notification = new Notification();
                notification.setUserId(onBehalfOfId);
                notification.setType("repayment_confirmed");
                notification.setTitle(payer.getNickname() + " 代您向 " + creditor.getNickname() + " 还款已确认");
                notification.setContent("金额: ¥" + String.format("%.2f", repayment.getAmount() / 100.0));
                notification.setRelatedId(Long.valueOf(repaymentId));
                notification.setRelatedType("repayment");
                notification.setIsRead((byte) 0);
                notification.setCreatedAt(LocalDateTime.now());
                notificationMapper.insertSelective(notification);
            } catch (Exception e) {
                // 站内信发送失败不影响业务
            }
        }

        // V32/V35: 发送确认邮件给付款人（异步，失败不影响业务）
        try {
            if (payer != null && payer.getEmail() != null) {
                String language = payer.getPreferredLanguage() != null ? payer.getPreferredLanguage() : "zh-CN";
                String emailContent = "还款确认通知: " + (repayment.getComment() != null ? repayment.getComment() : "还款已确认");
                if (isOnBehalf && beneficiary != null) {
                    emailContent += " (代 " + beneficiary.getNickname() + " 还款)";
                }
                emailService.sendBillNotificationAsync(
                    payer.getEmail(),
                    payer.getNickname(),
                    language,
                    creditor.getNickname(),
                    repayment.getAmount(),
                    repayment.getAmount(),
                    emailContent,
                    "还款确认",
                    null,
                    false
                );
            }
        } catch (Exception e) {
            // 邮件发送失败不影响业务
        }

        // V35: 发送确认邮件给被代还人（异步）
        if (isOnBehalf && beneficiary != null && beneficiary.getEmail() != null) {
            try {
                String language = beneficiary.getPreferredLanguage() != null ? beneficiary.getPreferredLanguage() : "zh-CN";
                emailService.sendBillNotificationAsync(
                    beneficiary.getEmail(),
                    beneficiary.getNickname(),
                    language,
                    payer.getNickname() + " 代您",
                    repayment.getAmount(),
                    repayment.getAmount(),
                    "代还款已确认: " + creditor.getNickname() + " 已确认收款",
                    "代还款确认",
                    null,
                    false
                );
            } catch (Exception e) {
                // 邮件发送失败不影响业务
            }
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

    @Override
    @Transactional
    public void batchRepay(BatchRepayRequest req, Integer currentUserId) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("还款明细不能为空");
        }

        User payer = userMapper.selectByPrimaryKey(currentUserId);
        User creditor = userMapper.selectByPrimaryKey(req.getCreditorId());

        if (creditor == null) {
            throw new IllegalArgumentException("债权人不存在");
        }

        LocalDateTime payTime = req.getPayDatetime() != null ? req.getPayDatetime() : LocalDateTime.now();
        List<Integer> repaymentIds = new ArrayList<>();
        StringBuilder detailsBuilder = new StringBuilder();
        long totalAmount = 0L;

        // 为每个代还对象创建独立的还款记录
        for (BatchRepayRequest.RepaymentItem item : req.getItems()) {
            Integer debtorId = item.getDebtorId();
            Long amount = item.getAmount();

            if (amount == null || amount <= 0) {
                continue;
            }

            User debtor = userMapper.selectByPrimaryKey(debtorId);
            if (debtor == null) {
                continue;
            }

            // 判断是自己还款还是代人还款
            boolean isOnBehalf = !debtorId.equals(currentUserId);

            Outcome o = new Outcome();
            o.setAmount(amount);
            o.setPayerUserid(currentUserId);  // 实际付款人
            o.setCreatorId(currentUserId);
            o.setTargetUserid(req.getCreditorId());  // 债权人
            o.setRepayFlag((byte) 2);
            o.setPerAmount(amount);
            o.setStyleId(0);
            o.setComment(req.getComment());
            o.setDeletedFlag((byte) 0);
            o.setPayDatetime(payTime);

            // V49: 活动还款时设置 activityId
            if (req.getActivityId() != null) {
                o.setActivityId(req.getActivityId());
            }

            // V35: 设置代还字段
            o.setRepaidBy(currentUserId);     // 实际付款人
            o.setOnBehalfOf(debtorId);        // 被代还人（债务归属人）

            outcomeMapper.insertSelective(o);
            repaymentIds.add(o.getId());

            // 创建参与者记录（待确认）
            OutcomeParticipant participant = new OutcomeParticipant();
            participant.setOutcomeId(o.getId());
            participant.setUserId(req.getCreditorId());
            participant.setShares(1);
            participant.setConfirmStatus((byte) 0);
            outcomeParticipantMapper.insertSelective(participant);

            totalAmount += amount;

            // 构建明细字符串
            if (detailsBuilder.length() > 0) {
                detailsBuilder.append(", ");
            }
            if (isOnBehalf) {
                detailsBuilder.append(debtor.getNickname()).append("(代)");
            } else {
                detailsBuilder.append(debtor.getNickname());
            }
            detailsBuilder.append(" ¥").append(String.format("%.2f", amount / 100.0));
        }

        if (repaymentIds.isEmpty()) {
            throw new IllegalArgumentException("没有有效的还款记录");
        }

        // V35: 发送合并通知给债权人（站内信）
        try {
            String notificationTitle;
            String notificationContent;

            if (req.getItems().size() == 1 && req.getItems().get(0).getDebtorId().equals(currentUserId)) {
                // 单人自己还款
                notificationTitle = payer.getNickname() + " 向你还款";
                notificationContent = "金额: ¥" + String.format("%.2f", totalAmount / 100.0);
            } else {
                // 批量或代还
                notificationTitle = payer.getNickname() + " 替多位成员还款";
                notificationContent = "总金额: ¥" + String.format("%.2f", totalAmount / 100.0) +
                    " (明细: " + detailsBuilder.toString() + ")";
            }

            if (req.getComment() != null && !req.getComment().isEmpty()) {
                notificationContent += " - " + req.getComment();
            }
            notificationContent += "。请确认收款。";

            Notification notification = new Notification();
            notification.setUserId(req.getCreditorId());
            notification.setType("repayment_received");
            notification.setTitle(notificationTitle);
            notification.setContent(notificationContent);
            notification.setRelatedId(Long.valueOf(repaymentIds.get(0))); // 关联第一条记录
            notification.setRelatedType("repayment");
            notification.setIsRead((byte) 0);
            notification.setCreatedAt(LocalDateTime.now());
            notificationMapper.insertSelective(notification);
        } catch (Exception e) {
            // 站内信发送失败不影响业务
        }

        // V35: 发送邮件通知给债权人（异步）
        try {
            if (creditor.getEmail() != null) {
                String language = creditor.getPreferredLanguage() != null ? creditor.getPreferredLanguage() : "zh-CN";
                String emailContent = "收到还款，请确认。明细: " + detailsBuilder.toString();
                if (req.getComment() != null) {
                    emailContent += " - " + req.getComment();
                }
                emailService.sendBillNotificationAsync(
                    creditor.getEmail(),
                    creditor.getNickname(),
                    language,
                    payer.getNickname(),
                    totalAmount,
                    totalAmount,
                    emailContent,
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
    public SettlementResponse getMinimizedSettlements(Integer userId) {
        Map<String, Long> netMap = buildDebtMap();

        // 原始转账笔数
        int originalCount = 0;
        for (Long v : netMap.values()) {
            if (v > 0) originalCount++;
        }

        // 计算每个用户的净余额: 正值=应收, 负值=应付
        Map<Integer, Long> balanceMap = new HashMap<>();
        for (Map.Entry<String, Long> e : netMap.entrySet()) {
            if (e.getValue() <= 0) continue;
            String[] parts = e.getKey().split("-");
            Integer debtor = Integer.valueOf(parts[0]);
            Integer creditor = Integer.valueOf(parts[1]);
            balanceMap.put(creditor, balanceMap.getOrDefault(creditor, 0L) + e.getValue());
            balanceMap.put(debtor, balanceMap.getOrDefault(debtor, 0L) - e.getValue());
        }

        // 分离债权人和债务人
        List<int[]> creditors = new ArrayList<>(); // [userId, balance]
        List<int[]> debtors = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : balanceMap.entrySet()) {
            long bal = e.getValue();
            if (bal > 0) {
                creditors.add(new int[]{e.getKey(), (int) bal});
            } else if (bal < 0) {
                debtors.add(new int[]{e.getKey(), (int) (-bal)});
            }
        }

        // 按金额降序
        creditors.sort((a, b) -> Integer.compare(b[1], a[1]));
        debtors.sort((a, b) -> Integer.compare(b[1], a[1]));

        // 贪心匹配
        List<SettlementItem> settlements = new ArrayList<>();
        int ci = 0, di = 0;
        while (ci < creditors.size() && di < debtors.size()) {
            int[] cr = creditors.get(ci);
            int[] dr = debtors.get(di);
            int transfer = Math.min(cr[1], dr[1]);

            SettlementItem item = new SettlementItem();
            item.setFromId(dr[0]);
            item.setToId(cr[0]);
            item.setAmount((long) transfer);
            settlements.add(item);

            cr[1] -= transfer;
            dr[1] -= transfer;
            if (cr[1] == 0) ci++;
            if (dr[1] == 0) di++;
        }

        // 填充用户信息
        Map<Integer, User> userCache = new HashMap<>();
        for (SettlementItem s : settlements) {
            User from = userCache.computeIfAbsent(s.getFromId(), userMapper::selectByPrimaryKey);
            User to = userCache.computeIfAbsent(s.getToId(), userMapper::selectByPrimaryKey);
            if (from != null) {
                s.setFromName(from.getNickname());
                s.setFromAvatarUrl(from.getAvatarUrl());
            }
            if (to != null) {
                s.setToName(to.getNickname());
                s.setToAvatarUrl(to.getAvatarUrl());
            }
        }

        // 构建与当前用户有直接债务关系的用户集合
        Set<Integer> relatedUsers = new HashSet<>();
        relatedUsers.add(userId);
        for (Map.Entry<String, Long> e : netMap.entrySet()) {
            if (e.getValue() <= 0) continue;
            String[] parts = e.getKey().split("-");
            Integer debtor = Integer.valueOf(parts[0]);
            Integer creditor = Integer.valueOf(parts[1]);
            if (debtor.equals(userId) || creditor.equals(userId)) {
                relatedUsers.add(debtor);
                relatedUsers.add(creditor);
            }
        }

        // 筛选双方都在用户债务圈内的结算路径
        List<SettlementItem> mySettlements = settlements.stream()
                .filter(s -> relatedUsers.contains(s.getFromId()) && relatedUsers.contains(s.getToId()))
                .collect(Collectors.toList());

        SettlementResponse resp = new SettlementResponse();
        resp.setSettlements(settlements);
        resp.setMySettlements(mySettlements);
        resp.setOriginalTransferCount(originalCount);
        resp.setMinimizedTransferCount(settlements.size());
        return resp;
    }

    /**
     * 获取当前用户的好友ID列表
     */
    private Set<Integer> getFriendIds(Integer currentUserId) {
        FavoredUserExample example = new FavoredUserExample();
        example.createCriteria().andUserIdEqualTo(currentUserId);
        List<FavoredUser> favored = favoredUserMapper.selectByExample(example);
        return favored.stream()
            .map(FavoredUser::getFavoredUserId)
            .collect(Collectors.toSet());
    }

    @Override
    public List<DebtorDebtInfo> getDebtorsForCreditor(Integer creditorId, Integer currentUserId) {
        // V35: 获取当前用户的好友列表
        Set<Integer> friendIds = getFriendIds(currentUserId);

        // 获取所有欠这个债权人钱的人
        Map<String, Long> netMap = buildDebtMap();
        Map<Integer, Long> debtorMap = new HashMap<>();
        Map<Integer, Long> pendingMap = new HashMap<>();

        // 计算每个人对债权人的净欠款
        for (Map.Entry<String, Long> e : netMap.entrySet()) {
            String[] parts = e.getKey().split("-");
            Integer debtorId = Integer.valueOf(parts[0]);
            Integer cId = Integer.valueOf(parts[1]);
            Long amount = e.getValue();

            if (amount > 0 && cId.equals(creditorId)) {
                debtorMap.put(debtorId, debtorMap.getOrDefault(debtorId, 0L) + amount);
            }
        }

        // 计算每个人的待确认金额
        OutcomeExample example = new OutcomeExample();
        example.createCriteria()
            .andTargetUseridEqualTo(creditorId)
            .andRepayFlagEqualTo((byte) 2)
            .andDeletedFlagEqualTo((byte) 0);

        List<Outcome> repayments = outcomeMapper.selectByExample(example);

        for (Outcome repayment : repayments) {
            OutcomeParticipant participant = outcomeParticipantMapper.selectByOutcomeAndUser(
                repayment.getId(), creditorId);

            // 只统计待确认的
            if (participant == null || participant.getConfirmStatus() == null || participant.getConfirmStatus() == 0) {
                // V35: 使用 onBehalfOf 字段确定真正的债务人
                Integer actualDebtor = repayment.getOnBehalfOf() != null ?
                    repayment.getOnBehalfOf() : repayment.getPayerUserid();
                pendingMap.put(actualDebtor, pendingMap.getOrDefault(actualDebtor, 0L) + repayment.getAmount());
            }
        }

        // 构建结果列表 - V35: 只返回好友
        List<DebtorDebtInfo> result = new ArrayList<>();

        for (Map.Entry<Integer, Long> e : debtorMap.entrySet()) {
            Integer debtorId = e.getKey();

            // V35: 只返回好友（排除自己）
            if (!friendIds.contains(debtorId) || debtorId.equals(currentUserId)) {
                continue;
            }

            Long totalDebt = e.getValue();
            Long pendingAmount = pendingMap.getOrDefault(debtorId, 0L);
            Long availableAmount = totalDebt - pendingAmount;

            // 只返回有可还金额的债务人
            if (availableAmount <= 0) {
                continue;
            }

            User debtor = userMapper.selectByPrimaryKey(debtorId);
            if (debtor == null) {
                continue;
            }

            DebtorDebtInfo info = new DebtorDebtInfo();
            info.setDebtorId(debtorId);
            info.setDebtorName(debtor.getNickname());
            info.setDebtorAvatarUrl(debtor.getAvatarUrl());
            info.setTotalDebt(totalDebt);
            info.setPendingAmount(pendingAmount);
            info.setAvailableAmount(availableAmount);

            result.add(info);
        }

        // 按可还金额降序排列
        result.sort((a, b) -> Long.compare(b.getAvailableAmount(), a.getAvailableAmount()));

        return result;
    }

    @Override
    public List<CreditorForOnBehalfItem> getCreditorsWithFriendDebts(Integer currentUserId) {
        // 获取当前用户的好友列表
        Set<Integer> friendIds = getFriendIds(currentUserId);

        if (friendIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有净欠款
        Map<String, Long> netMap = buildDebtMap();

        // 统计每个债权人有多少好友欠钱，以及总欠款
        Map<Integer, Set<Integer>> creditorFriends = new HashMap<>();
        Map<Integer, Long> creditorTotalDebt = new HashMap<>();

        for (Map.Entry<String, Long> e : netMap.entrySet()) {
            String[] parts = e.getKey().split("-");
            Integer debtorId = Integer.valueOf(parts[0]);
            Integer creditorId = Integer.valueOf(parts[1]);
            Long amount = e.getValue();

            // 只统计好友的正向欠款（好友欠别人钱）
            if (amount > 0 && friendIds.contains(debtorId) && !debtorId.equals(currentUserId)) {
                creditorFriends.computeIfAbsent(creditorId, k -> new HashSet<>()).add(debtorId);
                creditorTotalDebt.put(creditorId, creditorTotalDebt.getOrDefault(creditorId, 0L) + amount);
            }
        }

        // 构建结果列表
        List<CreditorForOnBehalfItem> result = new ArrayList<>();

        for (Map.Entry<Integer, Set<Integer>> e : creditorFriends.entrySet()) {
            Integer creditorId = e.getKey();
            Set<Integer> friends = e.getValue();

            // V35: 排除自己作为债权人（不能帮朋友把钱还给自己）
            if (creditorId.equals(currentUserId)) {
                continue;
            }

            User creditor = userMapper.selectByPrimaryKey(creditorId);
            if (creditor == null) {
                continue;
            }

            CreditorForOnBehalfItem item = new CreditorForOnBehalfItem();
            item.setCreditorId(creditorId);
            item.setCreditorName(creditor.getNickname());
            item.setCreditorAvatarUrl(creditor.getAvatarUrl());
            item.setFriendCount(friends.size());
            item.setTotalFriendDebt(creditorTotalDebt.getOrDefault(creditorId, 0L));

            // V39: 添加债权人支付方式和货币
            item.setPrimaryCurrency(creditor.getPrimaryCurrency());
            List<UserPaymentMethodDto> enabledMethods = userPaymentMethodService.getEnabledMethods(creditorId);
            List<String> methodCodes = enabledMethods.stream()
                    .map(UserPaymentMethodDto::getMethodCode)
                    .collect(Collectors.toList());
            item.setPaymentMethods(methodCodes);

            result.add(item);
        }

        // 按好友总欠款降序排列
        result.sort((a, b) -> Long.compare(b.getTotalFriendDebt(), a.getTotalFriendDebt()));

        return result;
    }

    /**
     * 冲突检测：查找24h内同对、金额±1%的待确认还款
     */
    private List<ConflictItem> findConflicts(Integer debtorId, Integer creditorId, Long amount) {
        OutcomeExample example = new OutcomeExample();
        example.createCriteria()
            .andRepayFlagEqualTo((byte) 2)
            .andDeletedFlagEqualTo((byte) 0)
            .andTargetUseridEqualTo(creditorId)
            .andPayDatetimeGreaterThan(LocalDateTime.now().minusHours(24));

        List<Outcome> recent = outcomeMapper.selectByExample(example);
        List<ConflictItem> conflicts = new ArrayList<>();

        for (Outcome o : recent) {
            // 确定真正的债务人
            Integer actualDebtor = o.getOnBehalfOf() != null ? o.getOnBehalfOf() : o.getPayerUserid();
            if (!actualDebtor.equals(debtorId)) continue;

            // 检查confirm_status是否为PENDING
            OutcomeParticipant p = outcomeParticipantMapper.selectByOutcomeAndUser(o.getId(), creditorId);
            if (p != null && p.getConfirmStatus() != null && p.getConfirmStatus() == 1) continue;

            // 金额±1%检查
            double ratio = Math.abs((double)(o.getAmount() - amount)) / Math.max(amount, 1);
            if (ratio <= 0.01) {
                ConflictItem item = new ConflictItem();
                item.setRepaymentId(o.getId());
                item.setAmount(o.getAmount());
                item.setPayDatetime(o.getPayDatetime());
                item.setCreatorId(o.getCreatorId());
                User creator = userMapper.selectByPrimaryKey(o.getCreatorId());
                item.setCreatorName(creator != null ? creator.getNickname() : "");
                conflicts.add(item);
            }
        }
        return conflicts;
    }

    @Override
    @Transactional
    public RepayFifoResponse repayWithFifo(RepayRequest req, Integer userId) {
        Integer creditorId = req.getCreditorId();
        Integer debtorId = req.getDebtorId() != null ? req.getDebtorId() : userId;

        // 冲突检测
        if (!Boolean.TRUE.equals(req.getSkipConflictCheck())) {
            List<ConflictItem> conflicts = findConflicts(debtorId, creditorId, req.getAmount());
            if (!conflicts.isEmpty()) {
                RepayFifoResponse resp = new RepayFifoResponse();
                resp.setHasConflict(true);
                resp.setConflictItems(conflicts);
                resp.setMessage("检测到疑似重复还款");
                return resp;
            }
        }

        // 1. 加载所有未删除的 outcomes
        List<Outcome> allOutcomes = loadGeneralOutcomes();

        // 2. 筛选：creditor 付款、debtor 参与的消费 (repayFlag=1)，按 pay_datetime ASC
        List<long[]> expenses = new ArrayList<>(); // [outcomeId, shareAmount]
        List<Outcome> expenseOutcomes = new ArrayList<>();
        for (Outcome o : allOutcomes) {
            if (o.getRepayFlag() != (byte) 1) continue;
            if (!o.getPayerUserid().equals(creditorId)) continue;
            List<OutcomeParticipant> ps = loadParticipants(o.getId());
            for (OutcomeParticipant p : ps) {
                if (p.getUserId().equals(debtorId)) {
                    Integer shares = p.getShares() != null ? p.getShares() : 1;
                    long shareAmount = o.getPerAmount() * shares;
                    expenses.add(new long[]{o.getId(), shareAmount});
                    expenseOutcomes.add(o);
                    break;
                }
            }
        }
        // 按 pay_datetime ASC 排序
        List<Integer> sortedIndices = new ArrayList<>();
        for (int i = 0; i < expenseOutcomes.size(); i++) sortedIndices.add(i);
        sortedIndices.sort((a, b) -> {
            LocalDateTime dtA = expenseOutcomes.get(a).getPayDatetime();
            LocalDateTime dtB = expenseOutcomes.get(b).getPayDatetime();
            if (dtA == null && dtB == null) return 0;
            if (dtA == null) return -1;
            if (dtB == null) return 1;
            return dtA.compareTo(dtB);
        });
        List<long[]> sortedExpenses = new ArrayList<>();
        List<Outcome> sortedExpenseOutcomes = new ArrayList<>();
        for (int idx : sortedIndices) {
            sortedExpenses.add(expenses.get(idx));
            sortedExpenseOutcomes.add(expenseOutcomes.get(idx));
        }

        // 3. 筛选：debtor→creditor 的已确认还款 (repayFlag=2)，按 pay_datetime ASC
        List<long[]> repayments = new ArrayList<>(); // [outcomeId, amount]
        for (Outcome o : allOutcomes) {
            if (o.getRepayFlag() != (byte) 2) continue;
            Integer actualDebtor = o.getOnBehalfOf() != null ? o.getOnBehalfOf() : o.getPayerUserid();
            if (!actualDebtor.equals(debtorId) || !o.getTargetUserid().equals(creditorId)) continue;
            // 只计已确认
            OutcomeParticipant participant = outcomeParticipantMapper.selectByOutcomeAndUser(o.getId(), creditorId);
            boolean confirmed = participant != null && participant.getConfirmStatus() != null && participant.getConfirmStatus() == 1;
            if (confirmed) {
                repayments.add(new long[]{o.getId(), o.getAmount()});
            }
        }
        // 按 pay_datetime ASC 排序
        repayments.sort((a, b) -> {
            Outcome oA = outcomeMapper.selectByPrimaryKey((int) a[0]);
            Outcome oB = outcomeMapper.selectByPrimaryKey((int) b[0]);
            LocalDateTime dtA = oA != null ? oA.getPayDatetime() : null;
            LocalDateTime dtB = oB != null ? oB.getPayDatetime() : null;
            if (dtA == null && dtB == null) return 0;
            if (dtA == null) return -1;
            if (dtB == null) return 1;
            return dtA.compareTo(dtB);
        });

        // 4. FIFO 分配已还金额到各 expense
        long[] previouslyRepaidArr = new long[sortedExpenses.size()];
        long repaidPool = 0;
        for (long[] rep : repayments) {
            repaidPool += rep[1];
        }
        long remaining = repaidPool;
        for (int i = 0; i < sortedExpenses.size(); i++) {
            long shareAmount = sortedExpenses.get(i)[1];
            long alloc = Math.min(shareAmount, remaining);
            previouslyRepaidArr[i] = alloc;
            remaining -= alloc;
            if (remaining <= 0) break;
        }

        // 5. 执行实际还款（调用现有 repay 方法）
        repay(req, userId);

        // 6. FIFO 分配本次新还款金额
        long newAmount = req.getAmount();
        long newAmountLeft = newAmount;
        long[] newlyRepaidArr = new long[sortedExpenses.size()];

        for (int i = 0; i < sortedExpenses.size(); i++) {
            if (newAmountLeft <= 0) break;
            long shareAmount = sortedExpenses.get(i)[1];
            long alreadyPaid = previouslyRepaidArr[i];
            long gap = shareAmount - alreadyPaid;
            if (gap <= 0) continue;
            long alloc = Math.min(gap, newAmountLeft);
            newlyRepaidArr[i] = alloc;
            newAmountLeft -= alloc;
        }

        // 7. 构建 FifoItem 列表
        List<FifoItem> settledBills = new ArrayList<>();
        boolean allSettled = true;
        long totalDebt = 0;

        for (int i = 0; i < sortedExpenses.size(); i++) {
            long shareAmount = sortedExpenses.get(i)[1];
            long prevRepaid = previouslyRepaidArr[i];
            long newRepaid = newlyRepaidArr[i];
            long totalRepaid = prevRepaid + newRepaid;
            double progress = shareAmount > 0 ? (double) totalRepaid / shareAmount : 1.0;
            if (progress > 1.0) progress = 1.0;

            String status;
            if (newRepaid == 0) {
                status = totalRepaid >= shareAmount ? "COMPLETED" : "UNCHANGED";
            } else {
                status = totalRepaid >= shareAmount ? "COMPLETED" : "PARTIAL";
            }

            if (totalRepaid < shareAmount) {
                allSettled = false;
            }
            totalDebt += shareAmount;

            Outcome o = sortedExpenseOutcomes.get(i);
            FifoItem item = new FifoItem();
            item.setOutcomeId(o.getId());
            item.setOriginalAmount(shareAmount);
            item.setPreviouslyRepaid(prevRepaid);
            item.setNewlyRepaid(newRepaid);
            item.setStatus(status);
            item.setProgress(progress);
            item.setComment(o.getComment());
            item.setPayDatetime(o.getPayDatetime() != null ? o.getPayDatetime().toString() : null);

            if (o.getStyleId() != null) {
                PayStyle style = payStyleMapper.selectByPrimaryKey(o.getStyleId());
                item.setCategoryName(style != null ? style.getStyleName() : "未分类");
            } else {
                item.setCategoryName("未分类");
            }

            settledBills.add(item);
        }

        // 8. 构建响应
        long totalPreviouslyRepaid = 0;
        for (long v : previouslyRepaidArr) totalPreviouslyRepaid += v;
        long remainingDebt = totalDebt - totalPreviouslyRepaid - newAmount;
        if (remainingDebt < 0) remainingDebt = 0;

        RepayFifoResponse response = new RepayFifoResponse();
        response.setMessage("还款记录成功");
        response.setSettledBills(settledBills);
        response.setAllSettled(allSettled);
        response.setRemainingDebt(remainingDebt);
        response.setTotalRepaidThisTime(newAmount);
        return response;
    }

    // ---- V49: Activity Debt Isolation ----

    @Override
    public List<Map<String, Object>> getActivityDebtsOverview(Integer userId) {
        // Get all activities user belongs to
        List<Activity> activities = activityMapper.selectByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Activity activity : activities) {
            if (activity.getStatus() != null && activity.getStatus() == 2) {
                // Skip settled activities (unless they have remaining debts)
            }

            List<Outcome> activityOutcomes = loadActivityOutcomes(activity.getId());
            if (activityOutcomes.isEmpty()) continue;

            Map<String, Long> debtMap = buildDebtMapFrom(activityOutcomes);

            // Calculate this user's debts/credits within this activity
            long shouldReceive = 0;
            long shouldPay = 0;
            List<Map<String, Object>> members = new ArrayList<>();

            for (Map.Entry<String, Long> e : debtMap.entrySet()) {
                String[] parts = e.getKey().split("-");
                Integer debtor = Integer.valueOf(parts[0]);
                Integer creditor = Integer.valueOf(parts[1]);
                Long amount = e.getValue();

                if (amount <= 0) continue;

                if (creditor.equals(userId)) {
                    shouldReceive += amount;
                    User debtorUser = userMapper.selectByPrimaryKey(debtor);
                    Map<String, Object> memberDebt = new HashMap<>();
                    memberDebt.put("userId", debtor);
                    memberDebt.put("nickname", debtorUser != null ? debtorUser.getNickname() : "");
                    memberDebt.put("amount", amount);
                    memberDebt.put("type", "owesMe");
                    members.add(memberDebt);
                }
                if (debtor.equals(userId)) {
                    shouldPay += amount;
                    User creditorUser = userMapper.selectByPrimaryKey(creditor);
                    Map<String, Object> memberDebt = new HashMap<>();
                    memberDebt.put("userId", creditor);
                    memberDebt.put("nickname", creditorUser != null ? creditorUser.getNickname() : "");
                    memberDebt.put("amount", amount);
                    memberDebt.put("type", "iOwe");
                    members.add(memberDebt);
                }
            }

            // Only include activities where user has active debts
            if (shouldReceive == 0 && shouldPay == 0) continue;

            Map<String, Object> activityDebt = new HashMap<>();
            activityDebt.put("activityId", activity.getId());
            activityDebt.put("activityName", activity.getName());
            activityDebt.put("coverEmoji", activity.getCoverEmoji());
            activityDebt.put("baseCurrency", activity.getBaseCurrency());
            activityDebt.put("status", activity.getStatus());
            activityDebt.put("shouldReceive", shouldReceive);
            activityDebt.put("shouldPay", shouldPay);
            activityDebt.put("members", members);
            result.add(activityDebt);
        }

        return result;
    }
}
