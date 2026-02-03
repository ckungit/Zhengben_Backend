package com.zhangben.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangben.backend.dto.UserPaymentMethodDto;
import com.zhangben.backend.mapper.UserPaymentMethodMapper;
import com.zhangben.backend.model.UserPaymentMethod;
import com.zhangben.backend.service.UserPaymentMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * V38: 用户支付方式服务实现
 */
@Service
public class UserPaymentMethodServiceImpl implements UserPaymentMethodService {

    @Autowired
    private UserPaymentMethodMapper paymentMethodMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<UserPaymentMethodDto> getEnabledMethods(Integer userId) {
        List<UserPaymentMethod> methods = paymentMethodMapper.selectEnabledByUserId(userId);
        return convertToDtoList(methods);
    }

    @Override
    public List<UserPaymentMethodDto> getAllMethods(Integer userId) {
        List<UserPaymentMethod> methods = paymentMethodMapper.selectAllByUserId(userId);
        return convertToDtoList(methods);
    }

    @Override
    @Transactional
    public void batchUpdateMethods(Integer userId, List<UserPaymentMethodDto> methods) {
        // 先获取用户现有的配置
        List<UserPaymentMethod> existing = paymentMethodMapper.selectAllByUserId(userId);

        for (UserPaymentMethodDto dto : methods) {
            UserPaymentMethod model = new UserPaymentMethod();
            model.setUserId(userId);
            model.setMethodCode(dto.getMethodCode());
            model.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : false);
            model.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);

            // 处理 detailConfig
            if (dto.getDetailConfig() != null) {
                try {
                    model.setDetailConfig(objectMapper.writeValueAsString(dto.getDetailConfig()));
                } catch (JsonProcessingException e) {
                    model.setDetailConfig(null);
                }
            }

            // 使用 upsert
            paymentMethodMapper.upsert(model);
        }
    }

    @Override
    @Transactional
    public void updateMethodStatus(Integer userId, String methodCode, boolean enabled) {
        UserPaymentMethod existing = paymentMethodMapper.selectByUserIdAndCode(userId, methodCode);

        if (existing != null) {
            paymentMethodMapper.updateEnabledStatus(userId, methodCode, enabled);
        } else if (enabled) {
            // 如果记录不存在且要启用，则创建新记录
            UserPaymentMethod newMethod = new UserPaymentMethod();
            newMethod.setUserId(userId);
            newMethod.setMethodCode(methodCode);
            newMethod.setEnabled(true);
            newMethod.setDisplayOrder(0);
            paymentMethodMapper.insert(newMethod);
        }
    }

    @Override
    @Transactional
    public void updateMethodConfig(Integer userId, String methodCode, String configJson) {
        UserPaymentMethod existing = paymentMethodMapper.selectByUserIdAndCode(userId, methodCode);

        if (existing != null) {
            existing.setDetailConfig(configJson);
            paymentMethodMapper.update(existing);
        } else {
            // 如果记录不存在，创建一个新的启用记录
            UserPaymentMethod newMethod = new UserPaymentMethod();
            newMethod.setUserId(userId);
            newMethod.setMethodCode(methodCode);
            newMethod.setEnabled(true);
            newMethod.setDetailConfig(configJson);
            newMethod.setDisplayOrder(0);
            paymentMethodMapper.insert(newMethod);
        }
    }

    /**
     * 转换为 DTO 列表
     */
    private List<UserPaymentMethodDto> convertToDtoList(List<UserPaymentMethod> methods) {
        List<UserPaymentMethodDto> dtoList = new ArrayList<>();

        for (UserPaymentMethod method : methods) {
            UserPaymentMethodDto dto = new UserPaymentMethodDto();
            dto.setMethodCode(method.getMethodCode());
            // 确保 enabled 始终是 Boolean 类型，处理数据库返回的各种可能值
            Boolean enabled = method.getEnabled();
            dto.setEnabled(enabled != null && enabled);
            dto.setDisplayOrder(method.getDisplayOrder());

            // 解析 JSON 配置
            if (method.getDetailConfig() != null && !method.getDetailConfig().isEmpty()) {
                try {
                    dto.setDetailConfig(objectMapper.readValue(method.getDetailConfig(), Object.class));
                } catch (JsonProcessingException e) {
                    dto.setDetailConfig(null);
                }
            }

            dtoList.add(dto);
        }

        return dtoList;
    }
}
