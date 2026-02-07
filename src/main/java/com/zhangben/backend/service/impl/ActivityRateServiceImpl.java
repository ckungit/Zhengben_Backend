package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.ActivityMapper;
import com.zhangben.backend.mapper.ActivityRateSnapshotMapper;
import com.zhangben.backend.model.Activity;
import com.zhangben.backend.model.ActivityRateSnapshot;
import com.zhangben.backend.service.ActivityRateService;
import com.zhangben.backend.service.CurrencyConverterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V49: Activity-scoped exchange rate service implementation.
 * On member join: fetches current global rate and locks it permanently.
 * All bills within the activity MUST use these locked rates.
 */
@Service
public class ActivityRateServiceImpl implements ActivityRateService {

    private static final Logger log = LoggerFactory.getLogger(ActivityRateServiceImpl.class);

    @Autowired
    private ActivityRateSnapshotMapper snapshotMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private CurrencyConverterService currencyConverterService;

    @Override
    public void lockRateOnMemberJoin(Integer activityId, String currencyCode) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在: " + activityId);
        }

        String baseCurrency = activity.getBaseCurrency();

        // If the member's currency is the same as activity base currency, no need to lock
        if (baseCurrency.equalsIgnoreCase(currencyCode)) {
            return;
        }

        // Check if already locked
        ActivityRateSnapshot existing = snapshotMapper.selectByActivityAndCurrency(activityId, currencyCode.toUpperCase());
        if (existing != null) {
            log.debug("Rate already locked for activity {} currency {}: {}", activityId, currencyCode, existing.getLockedRate());
            return;
        }

        // Fetch current global rate: baseCurrency → currencyCode
        BigDecimal rate = currencyConverterService.getRate(baseCurrency, currencyCode);
        if (rate == null) {
            log.warn("Cannot lock rate for activity {} currency {}: no global rate available", activityId, currencyCode);
            throw new RuntimeException("无法获取汇率: " + baseCurrency + " -> " + currencyCode);
        }

        // Lock it
        ActivityRateSnapshot snapshot = new ActivityRateSnapshot(activityId, currencyCode.toUpperCase(), rate);
        snapshotMapper.insert(snapshot);
        log.info("Locked rate for activity {}: {} -> {} = {}", activityId, baseCurrency, currencyCode, rate);
    }

    @Override
    public BigDecimal getLockedRate(Integer activityId, String currencyCode) {
        ActivityRateSnapshot snapshot = snapshotMapper.selectByActivityAndCurrency(activityId, currencyCode.toUpperCase());
        return snapshot != null ? snapshot.getLockedRate() : null;
    }

    @Override
    public Map<String, BigDecimal> getLockedRates(Integer activityId) {
        List<ActivityRateSnapshot> snapshots = snapshotMapper.selectByActivityId(activityId);
        Map<String, BigDecimal> result = new HashMap<>();
        for (ActivityRateSnapshot s : snapshots) {
            result.put(s.getCurrencyCode(), s.getLockedRate());
        }
        return result;
    }

    @Override
    public void updateLockedRate(Integer activityId, String currencyCode, BigDecimal newRate) {
        ActivityRateSnapshot existing = snapshotMapper.selectByActivityAndCurrency(activityId, currencyCode.toUpperCase());
        if (existing == null) {
            // No existing rate — insert new one
            ActivityRateSnapshot snapshot = new ActivityRateSnapshot(activityId, currencyCode.toUpperCase(), newRate);
            snapshotMapper.insert(snapshot);
        } else {
            snapshotMapper.updateLockedRate(activityId, currencyCode.toUpperCase(), newRate);
        }
        log.info("Updated locked rate for activity {}: {} = {}", activityId, currencyCode, newRate);
    }

    @Override
    public List<ActivityRateSnapshot> getSnapshots(Integer activityId) {
        return snapshotMapper.selectByActivityId(activityId);
    }

    @Override
    public void refreshAllRates(Integer activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在: " + activityId);
        }
        String baseCurrency = activity.getBaseCurrency();

        List<ActivityRateSnapshot> snapshots = snapshotMapper.selectByActivityId(activityId);
        for (ActivityRateSnapshot snapshot : snapshots) {
            BigDecimal newRate = currencyConverterService.getRate(baseCurrency, snapshot.getCurrencyCode());
            if (newRate != null) {
                snapshotMapper.updateLockedRate(activityId, snapshot.getCurrencyCode(), newRate);
                log.info("Refreshed rate for activity {}: {} -> {} = {}", activityId, baseCurrency, snapshot.getCurrencyCode(), newRate);
            }
        }
    }
}
