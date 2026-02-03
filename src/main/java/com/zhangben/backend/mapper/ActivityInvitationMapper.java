package com.zhangben.backend.mapper;

import com.zhangben.backend.model.ActivityInvitation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * V25: 活动邀请 Mapper
 */
@Mapper
public interface ActivityInvitationMapper {

    /**
     * 插入邀请
     */
    void insert(ActivityInvitation invitation);

    /**
     * 根据 ID 查询
     */
    ActivityInvitation selectById(Integer id);

    /**
     * 查询用户收到的待处理邀请
     */
    List<Map<String, Object>> selectPendingByInvitee(Integer inviteeId);

    /**
     * 查询活动的邀请状态（给邀请者看）
     */
    List<Map<String, Object>> selectByActivity(Integer activityId);

    /**
     * 检查是否已存在邀请
     */
    ActivityInvitation selectByActivityAndInvitee(@Param("activityId") Integer activityId,
                                                   @Param("inviteeId") Integer inviteeId);

    /**
     * 更新邀请状态
     */
    void updateStatus(@Param("id") Integer id, @Param("status") Byte status);

    /**
     * 标记邮件已发送
     */
    void markEmailSent(Integer id);

    /**
     * 删除活动的所有邀请
     */
    void deleteByActivity(Integer activityId);

    /**
     * V40: Delete all invitations involving a user (GDPR)
     */
    void deleteByUser(@Param("userId") Integer userId);
}
