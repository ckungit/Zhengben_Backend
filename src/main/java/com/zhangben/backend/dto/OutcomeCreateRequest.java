package com.zhangben.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * V42: 添加 Bean Validation 注解
 */
public class OutcomeCreateRequest {

    // 总金额（必填）
    @NotNull(message = "金额不能为空")
    @Min(value = 1, message = "金额必须大于0")
    private Long amount;

    // 参与者ID列表（注册用户）
    private List<Integer> targetUserIds;

    // 参与者份额映射 userId -> shares（V18新增：自定义份额功能）
    // 如果不提供，默认每人1份；如果提供，会覆盖默认值
    private Map<Integer, Integer> participantShares;

    // 是否包含本人（payer）
    private Boolean includeSelf;

    // 本人的份额数（V18新增：自定义份额功能，默认1）
    @Min(value = 1, message = "份额至少为1")
    private Integer selfShares;

    // 额外参与人数（未注册用户）
    @Min(value = 0, message = "额外参与人数不能为负")
    private Integer extraParticipants;

    // 额外参与者的总份额数（V18新增：自定义份额功能，默认等于extraParticipants）
    @Min(value = 0, message = "额外份额数不能为负")
    private Integer extraShares;

    // 1 = 支付款项，2 = 还钱记录
    private Byte repayFlag;

    // 支付分类ID
    private Integer styleId;

    // 备注
    @Size(max = 500, message = "备注最多500个字符")
    private String comment;

    // GPS
    private Double latitude;
    private Double longitude;

    // 支付时间（可选，不传则用当前时间）
    private LocalDateTime payDatetime;

    // 关联的活动ID（可选）
    private Integer activityId;

    // V47: Multi-currency fields
    private String originalCurrency;          // 原始交易币种 (e.g., "THB")
    private BigDecimal exchangeRateSnapshot;  // 汇率快照 (原始→结算)
    private Long convertedAmountSnapshot;     // 换算后金额 (结算币种, cents)
    private String targetCurrencySnapshot;    // 结算币种 (e.g., "JPY")

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public List<Integer> getTargetUserIds() {
        return targetUserIds;
    }

    public void setTargetUserIds(List<Integer> targetUserIds) {
        this.targetUserIds = targetUserIds;
    }

    public Boolean getIncludeSelf() {
        return includeSelf;
    }

    public void setIncludeSelf(Boolean includeSelf) {
        this.includeSelf = includeSelf;
    }

    public Integer getExtraParticipants() {
        return extraParticipants;
    }

    public void setExtraParticipants(Integer extraParticipants) {
        this.extraParticipants = extraParticipants;
    }

    public Byte getRepayFlag() {
        return repayFlag;
    }

    public void setRepayFlag(Byte repayFlag) {
        this.repayFlag = repayFlag;
    }

    public Integer getStyleId() {
        return styleId;
    }

    public void setStyleId(Integer styleId) {
        this.styleId = styleId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getPayDatetime() {
        return payDatetime;
    }

    public void setPayDatetime(LocalDateTime payDatetime) {
        this.payDatetime = payDatetime;
    }

    public Map<Integer, Integer> getParticipantShares() {
        return participantShares;
    }

    public void setParticipantShares(Map<Integer, Integer> participantShares) {
        this.participantShares = participantShares;
    }

    public Integer getSelfShares() {
        return selfShares;
    }

    public void setSelfShares(Integer selfShares) {
        this.selfShares = selfShares;
    }

    public Integer getExtraShares() {
        return extraShares;
    }

    public void setExtraShares(Integer extraShares) {
        this.extraShares = extraShares;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    // V47: Multi-currency getters/setters

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public BigDecimal getExchangeRateSnapshot() {
        return exchangeRateSnapshot;
    }

    public void setExchangeRateSnapshot(BigDecimal exchangeRateSnapshot) {
        this.exchangeRateSnapshot = exchangeRateSnapshot;
    }

    public Long getConvertedAmountSnapshot() {
        return convertedAmountSnapshot;
    }

    public void setConvertedAmountSnapshot(Long convertedAmountSnapshot) {
        this.convertedAmountSnapshot = convertedAmountSnapshot;
    }

    public String getTargetCurrencySnapshot() {
        return targetCurrencySnapshot;
    }

    public void setTargetCurrencySnapshot(String targetCurrencySnapshot) {
        this.targetCurrencySnapshot = targetCurrencySnapshot;
    }
}
