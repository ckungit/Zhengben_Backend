package com.zhangben.backend.controller;

import com.zhangben.backend.service.CurrencyConverterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * V48: Exchange rate API endpoints — USD-anchored model
 */
@RestController
@RequestMapping("/api/exchange-rates")
public class ExchangeRateController {

    @Autowired
    private CurrencyConverterService currencyConverterService;

    /**
     * Get all currencies' rate_to_usd.
     * Returns { "USD": 1.0, "JPY": 149.85, "CNY": 7.245, ... }
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, BigDecimal>> getLatestRates() {
        Map<String, BigDecimal> rates = currencyConverterService.getAllRates();
        return ResponseEntity.ok(rates);
    }

    /**
     * Get cross rate for a specific currency pair.
     */
    @GetMapping("/rate")
    public ResponseEntity<BigDecimal> getRate(
            @RequestParam String from,
            @RequestParam String to) {
        BigDecimal rate = currencyConverterService.getRate(from, to);
        if (rate == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rate);
    }

    /**
     * Refresh rate cache from DB.
     * Called after sync_rates.sh updates the DB directly.
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncRates() {
        currencyConverterService.refreshCache();
        return ResponseEntity.ok("Rate cache refreshed");
    }
}
