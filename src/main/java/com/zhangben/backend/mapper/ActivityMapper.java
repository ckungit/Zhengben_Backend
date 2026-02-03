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

    /**
     * V40: Get activities created by a user (GDPR)
     */
    List<Activity> selectByCreatorId(@Param("creatorId") Integer creatorId);

    /**
     * V40: Update activity creator (for ownership transfer)
     */
    int updateCreatorId(@Param("id") Integer id, @Param("newCreatorId") Integer newCreatorId);
}
