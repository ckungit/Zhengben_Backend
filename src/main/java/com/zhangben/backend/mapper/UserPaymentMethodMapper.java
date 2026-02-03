package com.zhangben.backend.mapper;

import com.zhangben.backend.model.UserPaymentMethod;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * V38: 用户支付方式 Mapper
 */
@Mapper
public interface UserPaymentMethodMapper {

    /**
     * 获取用户所有已启用的支付方式
     */
    @Select("SELECT * FROM user_payment_method WHERE user_id = #{userId} AND enabled = 1 ORDER BY display_order ASC")
    List<UserPaymentMethod> selectEnabledByUserId(@Param("userId") Integer userId);

    /**
     * 获取用户所有支付方式配置
     */
    @Select("SELECT * FROM user_payment_method WHERE user_id = #{userId} ORDER BY display_order ASC")
    List<UserPaymentMethod> selectAllByUserId(@Param("userId") Integer userId);

    /**
     * 获取单个支付方式配置
     */
    @Select("SELECT * FROM user_payment_method WHERE user_id = #{userId} AND method_code = #{methodCode}")
    UserPaymentMethod selectByUserIdAndCode(@Param("userId") Integer userId, @Param("methodCode") String methodCode);

    /**
     * 插入支付方式配置
     */
    @Insert("INSERT INTO user_payment_method (user_id, method_code, enabled, detail_config, display_order) " +
            "VALUES (#{userId}, #{methodCode}, #{enabled}, #{detailConfig}, #{displayOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserPaymentMethod method);

    /**
     * 更新支付方式配置
     */
    @Update("UPDATE user_payment_method SET enabled = #{enabled}, detail_config = #{detailConfig}, " +
            "display_order = #{displayOrder}, updated_at = NOW() WHERE id = #{id}")
    int update(UserPaymentMethod method);

    /**
     * 更新启用状态
     */
    @Update("UPDATE user_payment_method SET enabled = #{enabled}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND method_code = #{methodCode}")
    int updateEnabledStatus(@Param("userId") Integer userId, @Param("methodCode") String methodCode, @Param("enabled") Boolean enabled);

    /**
     * 删除用户的支付方式配置
     */
    @Delete("DELETE FROM user_payment_method WHERE user_id = #{userId} AND method_code = #{methodCode}")
    int deleteByUserIdAndCode(@Param("userId") Integer userId, @Param("methodCode") String methodCode);

    /**
     * 删除用户所有支付方式配置
     */
    @Delete("DELETE FROM user_payment_method WHERE user_id = #{userId}")
    int deleteAllByUserId(@Param("userId") Integer userId);

    /**
     * 插入或更新（upsert）
     */
    @Insert("INSERT INTO user_payment_method (user_id, method_code, enabled, detail_config, display_order) " +
            "VALUES (#{userId}, #{methodCode}, #{enabled}, #{detailConfig}, #{displayOrder}) " +
            "AS new_values " +
            "ON DUPLICATE KEY UPDATE enabled = new_values.enabled, detail_config = new_values.detail_config, " +
            "display_order = new_values.display_order, updated_at = NOW()")
    int upsert(UserPaymentMethod method);
}
