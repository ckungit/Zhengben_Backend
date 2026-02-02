package com.zhangben.backend.mapper;

import com.zhangben.backend.model.SystemConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemConfigMapper {

    /**
     * 根据配置键查询
     */
    @Select("SELECT * FROM system_config WHERE config_key = #{key}")
    @Results(id = "configResultMap", value = {
        @Result(property = "configKey", column = "config_key"),
        @Result(property = "configValue", column = "config_value"),
        @Result(property = "configType", column = "config_type"),
        @Result(property = "isSecret", column = "is_secret"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    SystemConfig selectByKey(String key);

    /**
     * 根据前缀查询配置（用于获取某个功能的所有配置）
     */
    @Select("SELECT * FROM system_config WHERE config_key LIKE CONCAT(#{prefix}, '%') ORDER BY config_key")
    @ResultMap("configResultMap")
    List<SystemConfig> selectByPrefix(String prefix);

    /**
     * 查询所有非敏感配置
     */
    @Select("SELECT * FROM system_config WHERE is_secret = 0 ORDER BY config_key")
    @ResultMap("configResultMap")
    List<SystemConfig> selectAllPublic();

    /**
     * 查询所有配置（管理用）
     */
    @Select("SELECT * FROM system_config ORDER BY config_key")
    @ResultMap("configResultMap")
    List<SystemConfig> selectAll();

    /**
     * 更新配置值
     */
    @Update("UPDATE system_config SET config_value = #{configValue}, updated_at = NOW() WHERE config_key = #{configKey}")
    int updateValue(SystemConfig config);

    /**
     * 插入新配置
     */
    @Insert("INSERT INTO system_config (config_key, config_value, config_type, description, is_secret) " +
            "VALUES (#{configKey}, #{configValue}, #{configType}, #{description}, #{isSecret})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemConfig config);

    /**
     * 删除配置
     */
    @Delete("DELETE FROM system_config WHERE config_key = #{key}")
    int deleteByKey(String key);

    /**
     * 根据前缀查询非敏感配置
     */
    @Select("SELECT * FROM system_config WHERE config_key LIKE CONCAT(#{prefix}, '%') AND is_secret = 0 ORDER BY config_key")
    @ResultMap("configResultMap")
    List<SystemConfig> selectPublicByPrefix(String prefix);
}
