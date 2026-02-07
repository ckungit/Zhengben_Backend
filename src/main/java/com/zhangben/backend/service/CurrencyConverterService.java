package com.zhangben.backend.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * V48: Currency conversion service — USD-anchored model.
 * All rates stored as "1 USD = X units of currency".
 * Cross-currency: A→B = amount × (rateToUsd_B / rateToUsd_A)
 */
public interface CurrencyConverterService {

    /**
     * Convert amount from one currency to another using latest rates.
     * Formula: result = amount × (rateTo / rateFrom)
     */
    BigDecimal convert(BigDecimal amount, String from, String to);

    /**
     * Convert using a manually specified rate (user override).
     */
    BigDecimal convertWithManualRate(BigDecimal amount, BigDecimal manualRate);

    /**
     * Get the cross rate from→to.
     * Returns rateTo / rateFrom.
     */
    BigDecimal getRate(String from, String to);

    /**
     * Get all currencies' rate_to_usd as a Map.
     * Key = currency code, Value = rate_to_usd.
     */
    Map<String, BigDecimal> getAllRates();

    /**
     * Refresh the Redis cache from DB.
     */
    void refreshCache();
}
