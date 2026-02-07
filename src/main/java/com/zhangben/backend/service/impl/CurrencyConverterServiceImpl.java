package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.ExchangeRateMapper;
import com.zhangben.backend.model.ExchangeRate;
import com.zhangben.backend.service.CurrencyConverterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * V48: USD-anchored currency converter with Redis caching.
 * Redis key: AABILL:RATES:USD_ANCHOR (Hash type)
 * Hash fields: "JPY" → "149.85", "CNY" → "7.245", ...
 * TTL: 24 hours
 */
@Service
public class CurrencyConverterServiceImpl implements CurrencyConverterService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConverterServiceImpl.class);

    private static final String REDIS_KEY = "AABILL:RATES:USD_ANCHOR";
    private static final long CACHE_TTL_HOURS = 24;

    @Autowired
    private ExchangeRateMapper exchangeRateMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        BigDecimal rateFrom = getRateToUsd(from);
        BigDecimal rateTo = getRateToUsd(to);
        if (rateFrom == null || rateTo == null) {
            throw new RuntimeException("Exchange rate not available: " + from + " -> " + to);
        }
        // A→B = amount × (rateTo / rateFrom)
        return amount.multiply(rateTo).divide(rateFrom, 10, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal convertWithManualRate(BigDecimal amount, BigDecimal manualRate) {
        return amount.multiply(manualRate).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getRate(String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            return BigDecimal.ONE;
        }
        BigDecimal rateFrom = getRateToUsd(from);
        BigDecimal rateTo = getRateToUsd(to);
        if (rateFrom == null || rateTo == null) {
            return null;
        }
        return rateTo.divide(rateFrom, 10, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, BigDecimal> getAllRates() {
        // 1. Try Redis Hash
        try {
            Map<Object, Object> cached = redisTemplate.opsForHash().entries(REDIS_KEY);
            if (!cached.isEmpty()) {
                Map<String, BigDecimal> result = new HashMap<>();
                for (Map.Entry<Object, Object> entry : cached.entrySet()) {
                    result.put(entry.getKey().toString(), new BigDecimal(entry.getValue().toString()));
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for rates cache: {}", e.getMessage());
        }

        // 2. Fall back to DB
        return loadFromDbAndCache();
    }

    @Override
    public void refreshCache() {
        loadFromDbAndCache();
        log.info("Exchange rate cache refreshed from DB");
    }

    /**
     * Get rate_to_usd for a single currency.
     * Checks Redis first, then DB.
     */
    private BigDecimal getRateToUsd(String code) {
        String upperCode = code.toUpperCase();

        // USD is always 1.0
        if ("USD".equals(upperCode)) {
            return BigDecimal.ONE;
        }

        // Try Redis
        try {
            Object cached = redisTemplate.opsForHash().get(REDIS_KEY, upperCode);
            if (cached != null) {
                return new BigDecimal(cached.toString());
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to DB: {}", e.getMessage());
        }

        // Try DB
        ExchangeRate rate = exchangeRateMapper.selectByCode(upperCode);
        if (rate != null) {
            // Cache the individual value
            try {
                redisTemplate.opsForHash().put(REDIS_KEY, upperCode, rate.getRateToUsd().toPlainString());
            } catch (Exception e) {
                // ignore cache failure
            }
            return rate.getRateToUsd();
        }

        log.warn("No exchange rate found for currency: {}", upperCode);
        return null;
    }

    /**
     * Load all rates from DB, write to Redis, return as Map.
     */
    private Map<String, BigDecimal> loadFromDbAndCache() {
        List<ExchangeRate> rates = exchangeRateMapper.selectAll();
        Map<String, BigDecimal> result = new HashMap<>();
        Map<String, String> redisEntries = new HashMap<>();

        for (ExchangeRate r : rates) {
            result.put(r.getCode(), r.getRateToUsd());
            redisEntries.put(r.getCode(), r.getRateToUsd().toPlainString());
        }

        if (!redisEntries.isEmpty()) {
            try {
                redisTemplate.delete(REDIS_KEY);
                redisTemplate.opsForHash().putAll(REDIS_KEY, redisEntries);
                redisTemplate.expire(REDIS_KEY, CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Failed to cache rates in Redis: {}", e.getMessage());
            }
        }

        return result;
    }
}
