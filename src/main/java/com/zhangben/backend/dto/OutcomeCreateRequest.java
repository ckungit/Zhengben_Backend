package com.zhangben.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OutcomeCreateRequest {

    // 总金额（必填）
    private Long amount;

    // 参与者ID列表（必填，至少一个）
    private List<Integer> targetUserIds;

    // 是否包含本人（payer）
    private Boolean includeSelf;

    // 1 = 支付款项，2 = 还钱记录
    private Byte repayFlag;

    // 支付分类ID
    private Integer styleId;

    // 备注
    private String comment;

    // GPS
    private Double latitude;
    private Double longitude;

    // 支付时间（可选，不传则用当前时间）
    private LocalDateTime payDatetime;

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

	
}
