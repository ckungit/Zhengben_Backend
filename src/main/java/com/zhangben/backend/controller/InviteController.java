package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.ActivityMapper;
import com.zhangben.backend.mapper.ActivityMemberMapper;
import com.zhangben.backend.mapper.FavoredUserMapper;
import com.zhangben.backend.mapper.InviteLinkMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.Activity;
import com.zhangben.backend.model.FavoredUser;
import com.zhangben.backend.model.InviteLink;
import com.zhangben.backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/invite")
public class InviteController {

    @Autowired
    private InviteLinkMapper inviteMapper;

    @Autowired
    private FavoredUserMapper favoredUserMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityMemberMapper activityMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Value("${app.base-url:https://www.aabillpay.com}")
    private String baseUrl;

    /**
     * 创建邀请链接
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> req) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        String type = (String) req.getOrDefault("type", "friend");
        Integer activityId = (Integer) req.get("activityId");
        Integer maxUses = (Integer) req.getOrDefault("maxUses", 0);
        Integer expireDays = (Integer) req.get("expireDays");

        // 如果是活动邀请，检查活动是否已结算
        if ("activity".equals(type) && activityId != null) {
            Activity activity = activityMapper.selectById(activityId);
            if (activity == null) {
                throw new RuntimeException("活动不存在");
            }
            if (activity.getStatus() == 2) {
                throw new RuntimeException("活动已结算，无法生成邀请链接");
            }
        }

        // 生成唯一邀请码
        String code = generateCode();

        InviteLink link = new InviteLink();
        link.setCode(code);
        link.setType(type);
        link.setCreatorId(userId);
        link.setActivityId(activityId);
        link.setMaxUses(maxUses);
        link.setStatus((byte) 1);

        if (expireDays != null && expireDays > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, expireDays);
            link.setExpiresAt(cal.getTime());
        }

        inviteMapper.insert(link);

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("url", baseUrl + "/invite/" + code);
        result.put("message", "邀请链接创建成功");
        return result;
    }

    /**
     * 获取我创建的邀请链接
     */
    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        List<InviteLink> links = inviteMapper.selectByCreatorId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (InviteLink link : links) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", link.getId());
            item.put("code", link.getCode());
            item.put("type", link.getType());
            item.put("activityId", link.getActivityId());
            item.put("maxUses", link.getMaxUses());
            item.put("usedCount", link.getUsedCount());
            item.put("expiresAt", link.getExpiresAt());
            item.put("status", link.getStatus());
            item.put("createdAt", link.getCreatedAt());
            item.put("url", baseUrl + "/invite/" + link.getCode());

            // 检查是否有效
            boolean isValid = link.getStatus() == 1;
            if (link.getExpiresAt() != null && link.getExpiresAt().before(new Date())) {
                isValid = false;
            }
            if (link.getMaxUses() > 0 && link.getUsedCount() >= link.getMaxUses()) {
                isValid = false;
            }
            item.put("isValid", isValid);

            result.add(item);
        }
        return result;
    }

    /**
     * 查看邀请链接信息（无需登录）
     */
    @GetMapping("/info/{code}")
    public Map<String, Object> info(@PathVariable String code) {
        InviteLink link = inviteMapper.selectByCode(code);
        if (link == null) {
            throw new RuntimeException("邀请链接不存在");
        }

        // 检查有效性
        if (link.getStatus() != 1) {
            throw new RuntimeException("邀请链接已失效");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().before(new Date())) {
            throw new RuntimeException("邀请链接已过期");
        }
        if (link.getMaxUses() > 0 && link.getUsedCount() >= link.getMaxUses()) {
            throw new RuntimeException("邀请链接已达使用上限");
        }

        // 如果是活动邀请，检查活动是否已结算
        if ("activity".equals(link.getType()) && link.getActivityId() != null) {
            Activity activity = activityMapper.selectById(link.getActivityId());
            if (activity != null && activity.getStatus() == 2) {
                throw new RuntimeException("活动已结算，邀请链接已失效");
            }
        }

        // 获取创建者信息
        User creator = userMapper.selectByPrimaryKey(link.getCreatorId());

        Map<String, Object> result = new HashMap<>();
        result.put("type", link.getType());
        result.put("creatorName", creator != null ? creator.getNickname() : "未知");
        result.put("activityId", link.getActivityId());

        return result;
    }

    /**
     * 使用邀请链接
     */
    @PostMapping("/use/{code}")
    public Map<String, Object> use(@PathVariable String code) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        InviteLink link = inviteMapper.selectByCode(code);
        if (link == null) {
            throw new RuntimeException("邀请链接不存在");
        }

        // 检查有效性
        if (link.getStatus() != 1) {
            throw new RuntimeException("邀请链接已失效");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().before(new Date())) {
            throw new RuntimeException("邀请链接已过期");
        }
        if (link.getMaxUses() > 0 && link.getUsedCount() >= link.getMaxUses()) {
            throw new RuntimeException("邀请链接已达使用上限");
        }

        // 不能使用自己的链接
        if (link.getCreatorId().equals(userId)) {
            throw new RuntimeException("不能使用自己的邀请链接");
        }

        // 检查是否已使用过
        if (inviteMapper.checkUsage(link.getId(), userId) > 0) {
            throw new RuntimeException("你已使用过此邀请链接");
        }

        Map<String, Object> result = new HashMap<>();

        if ("friend".equals(link.getType())) {
            // 好友邀请 - 互相添加为常用联系人
            try {
                FavoredUser fav1 = new FavoredUser();
                fav1.setUserId(link.getCreatorId());
                fav1.setFavoredUserId(userId);
                favoredUserMapper.insertSelective(fav1);
            } catch (Exception ignored) {}
            try {
                FavoredUser fav2 = new FavoredUser();
                fav2.setUserId(userId);
                fav2.setFavoredUserId(link.getCreatorId());
                favoredUserMapper.insertSelective(fav2);
            } catch (Exception ignored) {}

            User creator = userMapper.selectByPrimaryKey(link.getCreatorId());
            result.put("message", "已成功添加 " + (creator != null ? creator.getNickname() : "") + " 为好友");

        } else if ("activity".equals(link.getType())) {
            // 活动邀请 - 加入活动
            if (link.getActivityId() == null) {
                throw new RuntimeException("活动不存在");
            }

            // 检查活动是否已结算
            Activity activity = activityMapper.selectById(link.getActivityId());
            if (activity == null) {
                throw new RuntimeException("活动不存在");
            }
            if (activity.getStatus() == 2) {
                throw new RuntimeException("活动已结算，无法加入");
            }

            // 检查是否已是成员
            if (activityMemberMapper.selectByActivityAndUser(link.getActivityId(), userId) != null) {
                throw new RuntimeException("你已是该活动的成员");
            }

            activityMemberMapper.insert(link.getActivityId(), userId, "member");
            result.put("message", "已成功加入活动");
            result.put("activityId", link.getActivityId());
        }

        // 记录使用
        inviteMapper.insertUsage(link.getId(), userId);
        inviteMapper.incrementUsedCount(link.getId());

        return result;
    }

    /**
     * 禁用邀请链接
     */
    @PostMapping("/disable/{id}")
    public Map<String, Object> disable(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        InviteLink link = inviteMapper.selectByCode(String.valueOf(id));
        // 简化：直接用ID查
        inviteMapper.updateStatus(id, (byte) 0);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "邀请链接已禁用");
        return result;
    }

    /**
     * 删除邀请链接
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id) {
        StpUtil.checkLogin();
        inviteMapper.deleteById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "邀请链接已删除");
        return result;
    }

    /**
     * 生成唯一邀请码
     */
    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
