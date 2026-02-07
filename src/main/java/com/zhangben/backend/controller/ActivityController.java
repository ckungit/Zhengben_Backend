package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.ActivityMapper;
import com.zhangben.backend.mapper.ActivityMemberMapper;
import com.zhangben.backend.mapper.OutcomeMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.Activity;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.ActivityAuthService;
import com.zhangben.backend.service.ActivityEventService;
import com.zhangben.backend.service.ActivityRateService;
import com.zhangben.backend.util.CurrencyUtils;
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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ActivityRateService activityRateService;

    @Autowired
    private ActivityAuthService activityAuthService;

    @Autowired
    private ActivityEventService activityEventService;

    /**
     * 获取用户的语言偏好，默认中文
     */
    private String getUserLanguage(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null && user.getPreferredLanguage() != null && !user.getPreferredLanguage().isEmpty()) {
            return user.getPreferredLanguage();
        }
        return "zh-CN";
    }

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

        // V49: baseCurrency — set at creation, immutable
        String baseCurrency = (String) req.get("baseCurrency");
        if (baseCurrency == null || baseCurrency.isBlank()) {
            baseCurrency = CurrencyUtils.getCurrentUserCurrency();
        }

        // V51: invitePolicy — 1=creator only, 2=any member (default)
        Byte invitePolicy = (byte) 2;
        Object policyObj = req.get("invitePolicy");
        if (policyObj instanceof Number) {
            invitePolicy = ((Number) policyObj).byteValue();
        }

        Activity activity = new Activity();
        activity.setName(name.trim());
        activity.setDescription((String) req.get("description"));
        activity.setCoverEmoji((String) req.getOrDefault("coverEmoji", "🎉"));
        activity.setBaseCurrency(baseCurrency);
        activity.setInvitePolicy(invitePolicy);
        activity.setCreatorId(userId);
        activity.setStatus((byte) 1);
        activityMapper.insert(activity);

        // 添加创建者为成员
        memberMapper.insert(activity.getId(), userId, "creator");

        // V49: Lock rate for creator's primary currency if different from baseCurrency
        User creator = userMapper.selectByPrimaryKey(userId);
        if (creator != null && creator.getPrimaryCurrency() != null) {
            activityRateService.lockRateOnMemberJoin(activity.getId(), creator.getPrimaryCurrency());
        }

        // V51: Log creator join event
        activityEventService.logJoin(activity.getId(), userId,
                creator != null ? creator.getPrimaryCurrency() : null);

        Map<String, Object> result = new HashMap<>();
        result.put("id", activity.getId());
        result.put("baseCurrency", baseCurrency);
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
            item.put("baseCurrency", a.getBaseCurrency());
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
        result.put("baseCurrency", activity.getBaseCurrency());
        result.put("invitePolicy", activity.getInvitePolicy());
        result.put("settleTime", activity.getSettleTime());
        result.put("createdAt", activity.getCreatedAt());
        result.put("myRole", myMember.get("role"));

        // V49: Locked rates for this activity
        result.put("lockedRates", activityRateService.getLockedRates(id));

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

        return outcomeMapper.selectByActivityId(id, getUserLanguage(userId));
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
        List<Map<String, Object>> outcomes = outcomeMapper.selectByActivityId(id, getUserLanguage(userId));
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

        // V49: Lock rate for new member's primary currency
        User newMember = userMapper.selectByPrimaryKey(targetUserId);
        if (newMember != null && newMember.getPrimaryCurrency() != null) {
            activityRateService.lockRateOnMemberJoin(id, newMember.getPrimaryCurrency());
        }

        // V51: Log join event
        activityEventService.logJoin(id, targetUserId,
                newMember != null ? newMember.getPrimaryCurrency() : null);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "成员添加成功");
        return result;
    }

    /**
     * 移除成员（创建者踢人）
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

        // V51: Check no related bills
        int billCount = memberMapper.countUserOutcomes(id, memberId);
        if (billCount > 0) {
            throw new RuntimeException("该成员有相关账单，无法移除");
        }

        // V51: Log event before deletion
        User currentUser = userMapper.selectByPrimaryKey(userId);
        String removedByName = currentUser != null ? currentUser.getNickname() : "";
        activityEventService.logRemoved(id, memberId, removedByName);

        memberMapper.delete(id, memberId);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "成员已移除");
        return result;
    }

    /**
     * V51: 成员主动退出活动
     */
    @PostMapping("/{id}/leave")
    public Map<String, Object> leave(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 不能是创建者
        if (activity.getCreatorId().equals(userId)) {
            throw new RuntimeException("创建者不能退出活动");
        }

        // 必须是成员
        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null) {
            throw new RuntimeException("你不是该活动的成员");
        }

        // 检查无相关账单
        int billCount = memberMapper.countUserOutcomes(id, userId);
        if (billCount > 0) {
            throw new RuntimeException("你有相关账单，无法退出活动");
        }

        // Log event before deletion
        activityEventService.logLeave(id, userId);
        memberMapper.delete(id, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "已退出活动");
        return result;
    }

    /**
     * V51: 获取活动事件日志
     */
    @GetMapping("/{id}/events")
    public List<Map<String, Object>> getEvents(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null) {
            throw new RuntimeException("你不是该活动的成员");
        }

        return activityEventService.getEvents(id);
    }

    /**
     * V51: 更新活动设置（仅创建者）
     */
    @PutMapping("/{id}/settings")
    public Map<String, Object> updateSettings(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        activityAuthService.assertCreator(id, userId);

        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // Update invite policy
        Object policyObj = req.get("invitePolicy");
        if (policyObj instanceof Number) {
            activity.setInvitePolicy(((Number) policyObj).byteValue());
        }

        activityMapper.update(activity);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "设置已更新");
        result.put("invitePolicy", activity.getInvitePolicy());
        return result;
    }

    /**
     * V51: 刷新活动所有锁定汇率
     */
    @PostMapping("/{id}/refresh-rates")
    public Map<String, Object> refreshRates(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        activityAuthService.assertCreator(id, userId);

        activityRateService.refreshAllRates(id);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "汇率已刷新");
        result.put("lockedRates", activityRateService.getLockedRates(id));
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

    // ---- V49: Activity Rate Management ----

    /**
     * V49: 获取活动的锁定汇率列表
     */
    @GetMapping("/{id}/rates")
    public Map<String, Object> getLockedRates(@PathVariable Integer id) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Map<String, Object> myMember = memberMapper.selectByActivityAndUser(id, userId);
        if (myMember == null) {
            throw new RuntimeException("你不是该活动的成员");
        }

        Activity activity = activityMapper.selectById(id);

        Map<String, Object> result = new HashMap<>();
        result.put("baseCurrency", activity.getBaseCurrency());
        result.put("lockedRates", activityRateService.getLockedRates(id));
        result.put("snapshots", activityRateService.getSnapshots(id));
        return result;
    }

    /**
     * V49: 创建者手动更新活动内某币种的锁定汇率
     * 仅影响后续账单，不追溯旧账单
     */
    @PutMapping("/{id}/rates")
    public Map<String, Object> updateLockedRate(@PathVariable Integer id, @RequestBody Map<String, Object> req) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        activityAuthService.assertCreator(id, userId);

        String currencyCode = (String) req.get("currencyCode");
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new RuntimeException("请指定币种代码");
        }

        Object rateObj = req.get("lockedRate");
        if (rateObj == null) {
            throw new RuntimeException("请指定汇率");
        }
        java.math.BigDecimal newRate = new java.math.BigDecimal(rateObj.toString());
        if (newRate.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("汇率必须大于0");
        }

        activityRateService.updateLockedRate(id, currencyCode, newRate);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "汇率已更新（仅影响后续账单）");
        result.put("lockedRates", activityRateService.getLockedRates(id));
        return result;
    }
}
