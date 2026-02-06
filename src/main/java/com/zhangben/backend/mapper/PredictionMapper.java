package com.zhangben.backend.mapper;

import com.zhangben.backend.model.KalmanFilterState;
import com.zhangben.backend.model.UserMonthlySpending;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PredictionMapper {

    KalmanFilterState selectByUserId(@Param("userId") Integer userId);

    int insertKalmanState(KalmanFilterState record);

    int updateByUserId(KalmanFilterState record);

    List<UserMonthlySpending> selectMonthlySpending(@Param("userId") Integer userId);

    int upsertMonthlySpending(UserMonthlySpending record);

    List<UserMonthlySpending> aggregateMonthlySpending(@Param("userId") Integer userId);

    /**
     * 聚合固定支出（is_fixed=true 的分类）
     */
    List<UserMonthlySpending> aggregateFixedSpending(@Param("userId") Integer userId);

    /**
     * 聚合变动支出（is_fixed=false 或 NULL 的分类）
     */
    List<UserMonthlySpending> aggregateVariableSpending(@Param("userId") Integer userId);
}
