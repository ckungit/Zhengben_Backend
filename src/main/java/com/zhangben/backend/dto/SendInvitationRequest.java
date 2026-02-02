package com.zhangben.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * V25: 发送活动邀请请求 DTO
 */
public class SendInvitationRequest {

    @NotNull(message = "活动ID不能为空")
    private Integer activityId;

    @NotNull(message = "被邀请者ID不能为空")
    private Integer inviteeId;

    @Size(max = 200, message = "邀请留言最多200字")
    private String message;

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public Integer getInviteeId() {
        return inviteeId;
    }

    public void setInviteeId(Integer inviteeId) {
        this.inviteeId = inviteeId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
