package com.zhangben.backend.mapper;

import com.zhangben.backend.model.PaymentNudge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * V41: 催促还账 Mapper
 */
@Mapper
public interface PaymentNudgeMapper {

    /**
     * 插入催促记录
     */
    int insert(PaymentNudge nudge);

    /**
     * 检查指定时间范围内是否已催促过
     * @param creditorId 债权人ID
     * @param debtorId 债务人ID
     * @param since 起始时间（通常为24小时前）
     * @return 催促次数
     */
    int countRecentNudges(@Param("creditorId") Integer creditorId,
                          @Param("debtorId") Integer debtorId,
                          @Param("since") LocalDateTime since);

    /**
     * 获取最近一次催促记录
     */
    PaymentNudge selectLatestNudge(@Param("creditorId") Integer creditorId,
                                    @Param("debtorId") Integer debtorId);

    /**
     * 清理过期的催促记录（可选，用于定期清理）
     * @param before 清理此时间之前的记录
     */
    int deleteOldRecords(@Param("before") LocalDateTime before);
}
