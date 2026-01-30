package com.zhangben.backend.mapper;

import com.zhangben.backend.model.PasswordResetToken;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PasswordResetTokenMapper {

    @Insert("INSERT INTO password_reset_token (user_id, token, expires_at, used) " +
            "VALUES (#{userId}, #{token}, #{expiresAt}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PasswordResetToken token);

    @Select("SELECT * FROM password_reset_token WHERE token = #{token}")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "expiresAt", column = "expires_at"),
        @Result(property = "createdAt", column = "created_at")
    })
    PasswordResetToken selectByToken(String token);

    @Update("UPDATE password_reset_token SET used = 1 WHERE id = #{id}")
    int markAsUsed(Integer id);

    @Delete("DELETE FROM password_reset_token WHERE user_id = #{userId} AND used = 0")
    int deleteUnusedByUserId(Integer userId);

    @Delete("DELETE FROM password_reset_token WHERE expires_at < NOW() OR used = 1")
    int cleanExpired();
}
