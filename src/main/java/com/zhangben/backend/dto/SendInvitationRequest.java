package com.zhangben.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * V25: 发送活动邀请请求 DTO
 * 使用 Jakarta Validation 进行参数校验
 */
@Data
public class SendInvitationRequest {

    @NotNull(message = "活动ID不能为空")
    private Integer activityId;

    @NotNull(message = "被邀请者ID不能为空")
    private Integer inviteeId;

    @Size(max = 200, message = "邀请留言最多200字")
    private String message;
}
