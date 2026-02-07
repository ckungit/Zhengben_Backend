package com.zhangben.backend.service;

import java.util.List;
import java.util.Map;

/**
 * V51: Activity event logging service.
 */
public interface ActivityEventService {

    void logJoin(Integer activityId, Integer userId, String userCurrency);

    void logLeave(Integer activityId, Integer userId);

    void logRemoved(Integer activityId, Integer userId, String removedByName);

    List<Map<String, Object>> getEvents(Integer activityId);
}
