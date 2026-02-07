package com.zhangben.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V48: USD-anchored exchange rate entity.
 * Each row represents one currency's rate relative to USD.
 * rate_to_usd = "1 USD = X units of this currency"
 */
public class ExchangeRate {

    private String code;            // PK: "USD", "JPY", "CNY", etc.
    private BigDecimal rateToUsd;   // 1 USD = X units of this currency
    private LocalDateTime updatedAt;

    public ExchangeRate() {}

    public ExchangeRate(String code, BigDecimal rateToUsd) {
        this.code = code;
        this.rateToUsd = rateToUsd;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getRateToUsd() {
        return rateToUsd;
    }

    public void setRateToUsd(BigDecimal rateToUsd) {
        this.rateToUsd = rateToUsd;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
