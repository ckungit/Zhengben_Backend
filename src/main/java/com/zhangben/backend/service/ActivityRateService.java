package com.zhangben.backend.service;

import com.zhangben.backend.model.ActivityRateSnapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * V49: Activity-scoped exchange rate service.
 * Manages locked rates within an activity — isolated from global rates.
 */
public interface ActivityRateService {

    /**
     * Lock the current rate for a currency when a member joins.
     * If the currency already has a locked rate in this activity, this is a no-op.
     * @param activityId the activity
     * @param currencyCode the member's primary currency
     */
    void lockRateOnMemberJoin(Integer activityId, String currencyCode);

    /**
     * Get the locked rate for a currency in this activity.
     * Returns null if no locked rate exists.
     */
    BigDecimal getLockedRate(Integer activityId, String currencyCode);

    /**
     * Get all locked rates for an activity.
     * Returns Map of currencyCode → lockedRate.
     */
    Map<String, BigDecimal> getLockedRates(Integer activityId);

    /**
     * Creator manually updates the locked rate for a currency.
     * Only affects future bills — no retroactive changes.
     */
    void updateLockedRate(Integer activityId, String currencyCode, BigDecimal newRate);

    /**
     * Get all rate snapshot records for an activity.
     */
    List<ActivityRateSnapshot> getSnapshots(Integer activityId);

    /**
     * V51: Refresh all locked rates by fetching current global rates.
     */
    void refreshAllRates(Integer activityId);
}
