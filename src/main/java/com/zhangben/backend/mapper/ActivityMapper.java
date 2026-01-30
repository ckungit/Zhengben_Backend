package com.zhangben.backend.mapper;

import com.zhangben.backend.model.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {
    
    int insert(Activity activity);
    
    Activity selectById(@Param("id") Integer id);
    
    List<Activity> selectByUserId(@Param("userId") Integer userId);
    
    int update(Activity activity);
    
    int deleteById(@Param("id") Integer id);
    
    Map<String, Object> selectActivityStats(@Param("activityId") Integer activityId);
    
    List<Map<String, Object>> selectMemberStats(@Param("activityId") Integer activityId);
}
