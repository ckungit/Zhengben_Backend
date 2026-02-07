package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.ActivityEventMapper;
import com.zhangben.backend.model.ActivityEvent;
import com.zhangben.backend.service.ActivityEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ActivityEventServiceImpl implements ActivityEventService {

    @Autowired
    private ActivityEventMapper eventMapper;

    @Override
    public void logJoin(Integer activityId, Integer userId, String userCurrency) {
        ActivityEvent event = new ActivityEvent();
        event.setActivityId(activityId);
        event.setUserId(userId);
        event.setEventType("join");
        event.setUserCurrency(userCurrency);
        eventMapper.insert(event);
    }

    @Override
    public void logLeave(Integer activityId, Integer userId) {
        ActivityEvent event = new ActivityEvent();
        event.setActivityId(activityId);
        event.setUserId(userId);
        event.setEventType("leave");
        eventMapper.insert(event);
    }

    @Override
    public void logRemoved(Integer activityId, Integer userId, String removedByName) {
        ActivityEvent event = new ActivityEvent();
        event.setActivityId(activityId);
        event.setUserId(userId);
        event.setEventType("removed");
        event.setExtraInfo(removedByName);
        eventMapper.insert(event);
    }

    @Override
    public List<Map<String, Object>> getEvents(Integer activityId) {
        return eventMapper.selectByActivityId(activityId);
    }
}
