package com.zhangben.backend.service;

/**
 * V49: Activity authorization service.
 * Controls who can add members, modify rates, etc.
 */
public interface ActivityAuthService {

    /**
     * Check if user can add members to the activity.
     * Currently: only the creator can add members.
     */
    boolean canAddMember(Integer activityId, Integer userId);

    /**
     * Check if user is the creator of the activity.
     */
    boolean isCreator(Integer activityId, Integer userId);

    /**
     * Check if user is a member of the activity.
     */
    boolean isMember(Integer activityId, Integer userId);

    /**
     * Assert the user is the creator, throw if not.
     */
    void assertCreator(Integer activityId, Integer userId);

    /**
     * V51: Check if user can invite others to the activity.
     * Depends on invite_policy: 1=creator only, 2=any member.
     */
    boolean canInvite(Integer activityId, Integer userId);
}
