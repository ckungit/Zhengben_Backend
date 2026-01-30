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
}
