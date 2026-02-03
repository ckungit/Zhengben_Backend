package com.zhangben.backend.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * V38: 用户支付方式 DTO
 */
public class UserPaymentMethodDto {

    private String methodCode;
    private Boolean enabled;
    private Object detailConfig;  // JSON 对象
    private Integer displayOrder;

    // Constructors
    public UserPaymentMethodDto() {}

    public UserPaymentMethodDto(String methodCode, Boolean enabled) {
        this.methodCode = methodCode;
        this.enabled = enabled;
    }

    // Getters and Setters
    public String getMethodCode() {
        return methodCode;
    }

    public void setMethodCode(String methodCode) {
        this.methodCode = methodCode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Object getDetailConfig() {
        return detailConfig;
    }

    public void setDetailConfig(Object detailConfig) {
        this.detailConfig = detailConfig;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
