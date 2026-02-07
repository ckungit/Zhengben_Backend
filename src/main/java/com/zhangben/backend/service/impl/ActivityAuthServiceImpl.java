package com.zhangben.backend.service.impl;

import com.zhangben.backend.mapper.ActivityMapper;
import com.zhangben.backend.mapper.ActivityMemberMapper;
import com.zhangben.backend.model.Activity;
import com.zhangben.backend.service.ActivityAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * V49: Activity authorization service implementation.
 */
@Service
public class ActivityAuthServiceImpl implements ActivityAuthService {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityMemberMapper memberMapper;

    @Override
    public boolean canAddMember(Integer activityId, Integer userId) {
        // Currently only creator can add members
        return isCreator(activityId, userId);
    }

    @Override
    public boolean isCreator(Integer activityId, Integer userId) {
        Activity activity = activityMapper.selectById(activityId);
        return activity != null && activity.getCreatorId().equals(userId);
    }

    @Override
    public boolean isMember(Integer activityId, Integer userId) {
        Map<String, Object> member = memberMapper.selectByActivityAndUser(activityId, userId);
        return member != null;
    }

    @Override
    public void assertCreator(Integer activityId, Integer userId) {
        if (!isCreator(activityId, userId)) {
            throw new RuntimeException("只有活动创建者可以执行此操作");
        }
    }

    @Override
    public boolean canInvite(Integer activityId, Integer userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) return false;

        // invite_policy: 1=creator only, 2=any member (default)
        Byte policy = activity.getInvitePolicy();
        if (policy != null && policy == 1) {
            return isCreator(activityId, userId);
        }
        return isMember(activityId, userId);
    }
}
