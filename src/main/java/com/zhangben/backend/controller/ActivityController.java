package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.ActivityMapper;
import com.zhangben.backend.mapper.ActivityMemberMapper;
import com.zhangben.backend.mapper.OutcomeMapper;
import com.zhangben.backend.model.Activity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityMemberMapper memberMapper;

    @Autowired
    private OutcomeMapper outcomeMapper;

    /**
     * 创建活动
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestBody Map<String, Object> req) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        String name = (String) req.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("活动名称不能为空");
        }

        Activity activity = new Activity();
        activity.setName(name.trim());
        activity.setDescription((String) req.get("description"));
        activity.setCoverEmoji((String) req.getOrDefault("coverEmoji", "🎉"));
        activity.setCreatorId(userId);
        activity.setStatus((byte) 1);
        activityMapper.insert(activity);

        // 添加创建者为成员
        memberMapper.insert(activity.getId(), userId, "creator");

        Map<String, Object> result = new HashMap<>();
        result.put("id", activity.getId());
        result.put("message", "活动创建成功");
        return result;
    }

    /**
     * 获取我的活动列表
     */
    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        List<Activity> activities = activityMapper.selectByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Activity a : activities) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("name", a.getName());
            item.put("description", a.getDescription());
            item.put("coverEmoji", a.getCoverEmoji());
            item.put("status", a.getStatus());
            item.put("createdAt", a.getCreatedAt());

            // 成员数和消费统计
            item.put("memberCount", memberMapper.countByActivityId(a.getId()));
            Map<String, Object> stats = activityMapper.selectActivityStats(a.getId());
            item.put("totalAmount", stats != null ? stats.get("totalAmount") : 0);
            item.put("outcomeCount", stats != null ? stats.get("outcomeCount") : 0);

            // 成员昵称列表
            List<Map<String, Object>> members = memberMapper.selectByActivityId(a.getId());
            List<String> memberNames = members.stream()
                .map(m -> (String) m.get("nickname"))
                .filter(Objects::nonNull)
                .toList();
            item.put("memberNames", memberNames);

            result.add(item);
        }
        return result;
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 检查权限
        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null) {
            throw new RuntimeException("你不是该活动的成员");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", activity.getId());
        result.put("name", activity.getName());
        result.put("description", activity.getDescription());
        result.put("coverEmoji", activity.getCoverEmoji());
        result.put("status", activity.getStatus());
        result.put("creatorId", activity.getCreatorId());
        result.put("settleTime", activity.getSettleTime());
        result.put("createdAt", activity.getCreatedAt());
        result.put("myRole", myMember.get("role"));

        // 成员列表
        result.put("members", memberMapper.selectByActivityId(id));
        
        // 消费统计
        Map<String, Object> stats = activityMapper.selectActivityStats(id);
        result.put("totalAmount", stats != null ? stats.get("totalAmount") : 0);
        result.put("outcomeCount", stats != null ? stats.get("outcomeCount") : 0);
        
        // 每人消费统计
        result.put("memberStats", activityMapper.selectMemberStats(id));

        return result;
    }

    /**
     * 获取活动的账单列表
     */
    @GetMapping("/{id}/outcomes")
    public List<Map<String, Object>> getOutcomes(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        // 检查权限
        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null) {
            throw new RuntimeException("你不是该活动的成员");
        }

        return outcomeMapper.selectByActivityId(id);
    }

    /**
     * 删除活动的账单（仅活动创建者可用）
     */
    @DeleteMapping("/{id}/outcomes/{outcomeId}")
    public Map<String, Object> deleteOutcome(@PathVariable Integer id, @PathVariable Integer outcomeId) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        // 检查是否是活动创建者
        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null || !"creator".equals(myMember.get("role"))) {
            throw new RuntimeException("只有活动创建者可以删除账单");
        }

        // 检查账单是否属于该活动
        List<Map<String, Object>> outcomes = outcomeMapper.selectByActivityId(id);
        boolean found = outcomes.stream().anyMatch(o -> {
            Object idObj = o.get("id");
            if (idObj instanceof Number) {
                return ((Number) idObj).intValue() == outcomeId;
            }
            return false;
        });
        if (!found) {
            throw new RuntimeException("账单不存在或不属于该活动");
        }

        // 软删除账单
        outcomeMapper.softDeleteById(outcomeId);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "删除成功");
        return result;
    }

    /**
     * 添加成员
     */
    @PostMapping("/{id}/member")
    public Map<String, Object> addMember(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        // 检查权限
        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null || !"creator".equals(myMember.get("role"))) {
            throw new RuntimeException("只有创建者可以添加成员");
        }

        Integer targetUserId = (Integer) req.get("userId");
        if (targetUserId == null) {
            throw new RuntimeException("请指定用户");
        }

        if (memberMapper.selectByActivityAndUser(id, targetUserId) != null) {
            throw new RuntimeException("该用户已是成员");
        }

        memberMapper.insert(id, targetUserId, "member");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "成员添加成功");
        return result;
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/{id}/member/{memberId}")
    public Map<String, Object> removeMember(@PathVariable Integer id, @PathVariable Integer memberId) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null || !"creator".equals(myMember.get("role"))) {
            throw new RuntimeException("只有创建者可以移除成员");
        }

        if (memberId.equals(userId)) {
            throw new RuntimeException("不能移除自己");
        }

        memberMapper.delete(id, memberId);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "成员已移除");
        return result;
    }

    /**
     * 结算活动
     */
    @PostMapping("/{id}/settle")
    public Map<String, Object> settle(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null || !"creator".equals(myMember.get("role"))) {
            throw new RuntimeException("只有创建者可以结算");
        }

        activity.setStatus((byte) 2);
        activity.setSettleTime(new Date());
        activityMapper.update(activity);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "活动已结算");
        result.put("memberStats", activityMapper.selectMemberStats(id));
        return result;
    }

    /**
     * 更新活动
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null || !"creator".equals(myMember.get("role"))) {
            throw new RuntimeException("只有创建者可以修改");
        }

        if (req.containsKey("name")) activity.setName((String) req.get("name"));
        if (req.containsKey("description")) activity.setDescription((String) req.get("description"));
        if (req.containsKey("coverEmoji")) activity.setCoverEmoji((String) req.get("coverEmoji"));

        activityMapper.update(activity);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "更新成功");
        return result;
    }

    /**
     * 删除活动
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        if (!activity.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有创建者可以删除");
        }

        memberMapper.deleteByActivityId(id);
        activityMapper.deleteById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "活动已删除");
        return result;
    }
}
