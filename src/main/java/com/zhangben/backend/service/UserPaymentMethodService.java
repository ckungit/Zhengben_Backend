package com.zhangben.backend.service;

import com.zhangben.backend.dto.UserPaymentMethodDto;

import java.util.List;

/**
 * V38: 用户支付方式服务接口
 */
public interface UserPaymentMethodService {

    /**
     * 获取用户所有已启用的支付方式
     */
    List<UserPaymentMethodDto> getEnabledMethods(Integer userId);

    /**
     * 获取用户所有支付方式配置（包括未启用的）
     */
    List<UserPaymentMethodDto> getAllMethods(Integer userId);

    /**
     * 批量更新用户支付方式
     */
    void batchUpdateMethods(Integer userId, List<UserPaymentMethodDto> methods);

    /**
     * 更新单个支付方式的启用状态
     */
    void updateMethodStatus(Integer userId, String methodCode, boolean enabled);

    /**
     * 更新支付方式详细配置
     */
    void updateMethodConfig(Integer userId, String methodCode, String configJson);
}
