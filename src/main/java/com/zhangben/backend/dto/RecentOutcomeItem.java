package com.zhangben.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 最近支出记录项
 */
public class RecentOutcomeItem {

    private Integer id;
    private Long amount;           // 总金额
    private Long perAmount;        // 人均金额
    private String comment;        // 备注
    private Byte repayFlag;        // 1=支付，2=还款
    private Integer styleId;       // 分类ID
    private String styleName;      // 分类名称
    private LocalDateTime payDatetime;
    private List<String> participantNames; // 参与者昵称列表
    private List<Integer> participantIds;  // 参与者ID列表
    
    // 还款相关
    private Integer targetUserId;   // 还款对象ID（repayFlag=2时有值）
    private String targetUserName;  // 还款对象昵称（repayFlag=2时有值）

    // 活动相关
    private Integer activityId;     // 关联的活动ID
    private String activityName;    // 活动名称

    // V29: 账单创建者
    private Integer creatorId;

    // V35: 记录类型 - "expense"(支出), "repayment"(还款), "income"(收入)
    private String recordType;

    // V35: 付款人信息（用于收入记录，显示谁还给了我）
    private Integer payerId;
    private String payerName;

    // V47: Multi-currency fields
    private Long originalAmount;        // 原始币种金额 (cents)
    private String originalCurrency;    // 原始交易币种
    private String currency;            // 结算币种

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getPerAmount() {
        return perAmount;
    }

    public void setPerAmount(Long perAmount) {
        this.perAmount = perAmount;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Byte getRepayFlag() {
        return repayFlag;
    }

    public void setRepayFlag(Byte repayFlag) {
        this.repayFlag = repayFlag;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public LocalDateTime getPayDatetime() {
        return payDatetime;
    }

    public void setPayDatetime(LocalDateTime payDatetime) {
        this.payDatetime = payDatetime;
    }

    public List<String> getParticipantNames() {
        return participantNames;
    }

    public void setParticipantNames(List<String> participantNames) {
        this.participantNames = participantNames;
    }

    public List<Integer> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<Integer> participantIds) {
        this.participantIds = participantIds;
    }

    public Integer getStyleId() {
        return styleId;
    }

    public void setStyleId(Integer styleId) {
        this.styleId = styleId;
    }

    public Integer getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public Integer getPayerId() {
        return payerId;
    }

    public void setPayerId(Integer payerId) {
        this.payerId = payerId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public Long getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Long originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
