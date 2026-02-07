package com.zhangben.backend.mapper;

import com.zhangben.backend.model.ActivityEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityEventMapper {

    void insert(ActivityEvent event);

    List<Map<String, Object>> selectByActivityId(@Param("activityId") Integer activityId);
}
