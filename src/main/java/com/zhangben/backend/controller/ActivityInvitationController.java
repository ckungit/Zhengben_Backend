package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.SendInvitationRequest;
import com.zhangben.backend.mapper.ActivityInvitationMapper;
import com.zhangben.backend.mapper.ActivityMapper;
import com.zhangben.backend.mapper.ActivityMemberMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.Activity;
import com.zhangben.backend.model.ActivityInvitation;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.ActivityAuthService;
import com.zhangben.backend.service.ActivityEventService;
import com.zhangben.backend.service.ActivityRateService;
import com.zhangben.backend.service.EmailService;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V25: 活动邀请控制器
 */
@RestController
@RequestMapping("/api/activity-invitation")
public class ActivityInvitationController {

    @Autowired
    private ActivityInvitationMapper invitationMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityMemberMapper memberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ActivityRateService activityRateService;

    @Autowired
    private ActivityAuthService activityAuthService;

    @Autowired
    private ActivityEventService activityEventService;

    @Value("${app.base-url:https://www.aabillpay.com}")
    private String baseUrl;

    /**
     * 发送邀请
     * 使用 @Valid 进行参数校验
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendInvitation(@Valid @RequestBody SendInvitationRequest req) {
        StpUtil.checkLogin();
        Integer inviterId = StpUtil.getLoginIdAsInt();

        Integer activityId = req.getActivityId();
        Integer inviteeId = req.getInviteeId();
        String message = req.getMessage();

        // 检查活动是否存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return ResponseEntity.badRequest().body("活动不存在");
        }

        // V51: Check invite permission (depends on invite_policy)
        if (!activityAuthService.canInvite(activityId, inviterId)) {
            return ResponseEntity.badRequest().body("你没有邀请权限");
        }

        // 检查被邀请者是否已是成员
        Map<String, Object> inviteeMember = memberMapper.selectByActivityAndUser(activityId, inviteeId);
        if (inviteeMember != null) {
            return ResponseEntity.badRequest().body("该用户已是活动成员");
        }

        // 检查是否已发送过邀请
        ActivityInvitation existing = invitationMapper.selectByActivityAndInvitee(activityId, inviteeId);
        if (existing != null) {
            if (existing.getStatus() == 0) {
                return ResponseEntity.badRequest().body("已发送过邀请，等待对方处理");
            } else if (existing.getStatus() == 1 || existing.getStatus() == 2 || existing.getStatus() == 3) {
                // 重新发送：已接受(退出后)/被拒绝/已取消 的邀请
                invitationMapper.updateStatus(existing.getId(), (byte) 0);
                return ResponseEntity.ok(Map.of("message", "邀请已重新发送", "id", existing.getId()));
            }
        }

        // 创建邀请
        ActivityInvitation invitation = new ActivityInvitation();
        invitation.setActivityId(activityId);
        invitation.setInviterId(inviterId);
        invitation.setInviteeId(inviteeId);
        invitation.setStatus((byte) 0);
        invitation.setInviteMessage(message);
        invitation.setEmailSent((byte) 0);
        invitationMapper.insert(invitation);

        // 异步发送邮件通知
        try {
            User inviter = userMapper.selectByPrimaryKey(inviterId);
            User invitee = userMapper.selectByPrimaryKey(inviteeId);
            if (invitee != null && StrUtil.isNotBlank(invitee.getEmail())) {
                String inviteUrl = baseUrl + "/activities";
                // 使用 Hutool 的 StrUtil.format 进行字符串格式化
                String emailContent = StrUtil.format(
                    "<p>你好，{}！</p>" +
                    "<p><strong>{}</strong> 邀请你加入活动「<strong>{}</strong>」。</p>" +
                    "{}" +
                    "<p>请登录 Pay友 查看并处理邀请：</p>" +
                    "<p><a href=\"{}\">{}</a></p>" +
                    "<p>Pay友，你的 AA 制记账好朋友。</p>",
                    invitee.getNickname(),
                    inviter.getNickname(),
                    activity.getName(),
                    StrUtil.isNotBlank(message) ? "<p>留言：" + message + "</p>" : "",
                    inviteUrl,
                    inviteUrl
                );
                emailService.sendEmail(invitee.getEmail(), invitee.getNickname(), "你收到一个活动邀请 - Pay友", emailContent);
                invitationMapper.markEmailSent(invitation.getId());
            }
        } catch (Exception e) {
            // 邮件发送失败不影响邀请
            System.err.println("发送邀请邮件失败: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "邀请已发送", "id", invitation.getId()));
    }

    /**
     * 获取我收到的待处理邀请
     */
    @GetMapping("/pending")
    public List<Map<String, Object>> getPendingInvitations() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();
        return invitationMapper.selectPendingByInvitee(userId);
    }

    /**
     * 获取活动的邀请状态（给邀请者/创建者看）
     */
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<?> getActivityInvitations(@PathVariable Integer activityId) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        // 检查是否是活动成员
        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(activityId, userId);
        if (myMember == null) {
            return ResponseEntity.badRequest().body("你不是该活动的成员");
        }

        return ResponseEntity.ok(invitationMapper.selectByActivity(activityId));
    }

    /**
     * 接受邀请
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptInvitation(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        ActivityInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            return ResponseEntity.badRequest().body("邀请不存在");
        }

        if (!invitation.getInviteeId().equals(userId)) {
            return ResponseEntity.badRequest().body("无权操作此邀请");
        }

        if (invitation.getStatus() != 0) {
            return ResponseEntity.badRequest().body("邀请已处理");
        }

        // 更新邀请状态
        invitationMapper.updateStatus(id, (byte) 1);

        // 添加为活动成员
        Map<String, Object> existingMember = memberMapper.selectByActivityAndUser(invitation.getActivityId(), userId);
        if (existingMember == null) {
            memberMapper.insert(invitation.getActivityId(), userId, "member");
        }

        // V49: Lock rate for new member's primary currency
        User invitee = userMapper.selectByPrimaryKey(userId);
        if (invitee != null && invitee.getPrimaryCurrency() != null) {
            activityRateService.lockRateOnMemberJoin(invitation.getActivityId(), invitee.getPrimaryCurrency());
        }

        // V51: Log join event
        activityEventService.logJoin(invitation.getActivityId(), userId,
                invitee != null ? invitee.getPrimaryCurrency() : null);

        return ResponseEntity.ok(Map.of("message", "已加入活动"));
    }

    /**
     * 拒绝邀请
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectInvitation(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        ActivityInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            return ResponseEntity.badRequest().body("邀请不存在");
        }

        if (!invitation.getInviteeId().equals(userId)) {
            return ResponseEntity.badRequest().body("无权操作此邀请");
        }

        if (invitation.getStatus() != 0) {
            return ResponseEntity.badRequest().body("邀请已处理");
        }

        // 更新邀请状态
        invitationMapper.updateStatus(id, (byte) 2);

        return ResponseEntity.ok(Map.of("message", "已拒绝邀请"));
    }

    /**
     * V51: 取消邀请（邀请人取消已发出的待处理邀请）
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelInvitation(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        ActivityInvitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            return ResponseEntity.badRequest().body("邀请不存在");
        }

        if (!invitation.getInviterId().equals(userId)) {
            return ResponseEntity.badRequest().body("只有邀请人可以取消邀请");
        }

        if (invitation.getStatus() != 0) {
            return ResponseEntity.badRequest().body("邀请已处理，无法取消");
        }

        int updated = invitationMapper.cancelByInviter(id, userId);
        if (updated == 0) {
            return ResponseEntity.badRequest().body("取消失败");
        }

        return ResponseEntity.ok(Map.of("message", "邀请已取消"));
    }
}
