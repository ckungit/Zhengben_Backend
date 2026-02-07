package com.zhangben.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMemberMapper {
    
    int insert(@Param("activityId") Integer activityId, 
               @Param("userId") Integer userId, 
               @Param("role") String role);
    
    Map<String, Object> selectByActivityAndUser(@Param("activityId") Integer activityId, 
                                                 @Param("userId") Integer userId);
    
    List<Map<String, Object>> selectByActivityId(@Param("activityId") Integer activityId);
    
    int delete(@Param("activityId") Integer activityId, @Param("userId") Integer userId);
    
    int deleteByActivityId(@Param("activityId") Integer activityId);
    
    int countByActivityId(@Param("activityId") Integer activityId);

    /**
     * V51: Count outcomes related to a user in an activity
     * (as payer or as participant)
     */
    int countUserOutcomes(@Param("activityId") Integer activityId, @Param("userId") Integer userId);

    /**
     * V40: Delete all memberships for a user (GDPR)
     */
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * V40: Get first other member of an activity (for ownership transfer)
     */
    Map<String, Object> selectFirstOtherMember(@Param("activityId") Integer activityId, @Param("excludeUserId") Integer excludeUserId);
}
