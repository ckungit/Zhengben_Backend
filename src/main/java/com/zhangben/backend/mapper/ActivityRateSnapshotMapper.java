package com.zhangben.backend.mapper;

import com.zhangben.backend.model.ActivityRateSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * V49: Activity rate snapshot data access
 */
@Mapper
public interface ActivityRateSnapshotMapper {

    ActivityRateSnapshot selectByActivityAndCurrency(@Param("activityId") Integer activityId,
                                                      @Param("currencyCode") String currencyCode);

    List<ActivityRateSnapshot> selectByActivityId(@Param("activityId") Integer activityId);

    void insert(ActivityRateSnapshot snapshot);

    void updateLockedRate(@Param("activityId") Integer activityId,
                          @Param("currencyCode") String currencyCode,
                          @Param("lockedRate") BigDecimal lockedRate);

    void deleteByActivityId(@Param("activityId") Integer activityId);
}
