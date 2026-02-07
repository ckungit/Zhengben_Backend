package com.zhangben.backend.mapper;

import com.zhangben.backend.model.ExchangeRate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * V48: Exchange rate data access — USD-anchored model
 */
@Mapper
public interface ExchangeRateMapper {

    ExchangeRate selectByCode(@Param("code") String code);

    List<ExchangeRate> selectAll();

    void upsert(@Param("code") String code, @Param("rateToUsd") BigDecimal rateToUsd);

    void batchUpsert(@Param("rates") List<ExchangeRate> rates);
}
