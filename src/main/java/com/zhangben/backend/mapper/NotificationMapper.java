package com.zhangben.backend.mapper;

import com.zhangben.backend.model.Notification;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface NotificationMapper {

    int insert(Notification notification);

    int insertSelective(Notification notification);

    Notification selectByPrimaryKey(Long id);

    List<Notification> selectByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    List<Notification> selectUnreadByUserId(@Param("userId") Integer userId);

    int countUnreadByUserId(@Param("userId") Integer userId);

    int markAsRead(@Param("id") Long id);

    int markAllAsRead(@Param("userId") Integer userId);

    int deleteByPrimaryKey(Long id);

    /**
     * V40: Delete all notifications for a user (GDPR)
     */
    int deleteByUserId(@Param("userId") Integer userId);
}
