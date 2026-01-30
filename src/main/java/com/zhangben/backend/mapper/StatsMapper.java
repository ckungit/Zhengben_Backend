package com.zhangben.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {
    
    List<Map<String, Object>> getMonthlyStats(@Param("userId") Integer userId, @Param("months") Integer months);
    
    List<Map<String, Object>> getCategoryStats(@Param("userId") Integer userId, @Param("months") Integer months);
    
    List<Map<String, Object>> getPartnerStats(@Param("userId") Integer userId, @Param("months") Integer months);
    
    Map<String, Object> getOverviewStats(@Param("userId") Integer userId, @Param("months") Integer months);
    
    List<Map<String, Object>> getDailyAvgStats(@Param("userId") Integer userId, @Param("days") Integer days);
}
