package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.OutcomeCreateRequest;
import com.zhangben.backend.dto.RecentOutcomeItem;
import com.zhangben.backend.service.OutcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outcome")
public class OutcomeController {

    @Autowired
    private OutcomeService outcomeService;

    /**
     * 创建支出记录
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOutcome(@RequestBody OutcomeCreateRequest req) {
        StpUtil.checkLogin();
        try {
            outcomeService.createOutcome(req);
            return ResponseEntity.ok("记录创建成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取当前用户最近的支出记录
     * @param limit 返回数量，默认10条
     */
    @GetMapping("/recent")
    public List<RecentOutcomeItem> getRecentOutcomes(
            @RequestParam(defaultValue = "10") Integer limit) {
        Integer userId = StpUtil.getLoginIdAsInt();
        return outcomeService.getRecentOutcomes(userId, limit);
    }

    /**
     * 删除支出记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOutcome(@PathVariable("id") Integer id) {
        Integer userId = StpUtil.getLoginIdAsInt();
        try {
            outcomeService.deleteOutcome(id, userId);
            return ResponseEntity.ok("删除成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取单个支出记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOutcome(@PathVariable("id") Integer id) {
        Integer userId = StpUtil.getLoginIdAsInt();
        try {
            return ResponseEntity.ok(outcomeService.getOutcomeById(id, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 更新支出记录（仅限创建者在1天内可修改）
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOutcome(@PathVariable("id") Integer id, @RequestBody OutcomeCreateRequest req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        try {
            outcomeService.updateOutcome(id, userId, req);
            return ResponseEntity.ok("更新成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Seek-based 分页获取历史记录（支持月份/日期筛选）
     * @param lastId 上一页最后一条记录的ID（首页不传）
     * @param limit 每页数量，默认20
     * @param month 月份筛选 yyyy-MM（可选）
     * @param day 日期筛选 yyyy-MM-dd（可选，优先于month）
     */
    @GetMapping("/history")
    public ResponseEntity<?> getPagedHistory(
            @RequestParam(required = false) Integer lastId,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String day) {
        Integer userId = StpUtil.getLoginIdAsInt();
        Map<String, Object> result = outcomeService.getPagedHistory(userId, lastId, limit, month, day);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户有记录的月份列表
     */
    @GetMapping("/months")
    public ResponseEntity<?> getAvailableMonths() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return ResponseEntity.ok(outcomeService.getAvailableMonths(userId));
    }

    /**
     * 获取用户某月有记录的日期列表
     * @param month 月份 yyyy-MM
     */
    @GetMapping("/days")
    public ResponseEntity<?> getActiveDays(@RequestParam String month) {
        Integer userId = StpUtil.getLoginIdAsInt();
        List<Integer> days = outcomeService.getActiveDays(userId, month);
        return ResponseEntity.ok(days);
    }

    /**
     * 搜索历史账单
     * @param keyword 搜索关键词（搜索备注）
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate 结束日期（格式：yyyy-MM-dd）
     * @param styleId 分类ID
     * @param limit 返回数量
     * @param offset 偏移量
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchOutcomes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer styleId,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {
        Integer userId = StpUtil.getLoginIdAsInt();

        List<RecentOutcomeItem> items = outcomeService.searchOutcomes(
                userId, keyword, startDate, endDate, styleId, limit, offset);
        Integer total = outcomeService.countSearchOutcomes(
                userId, keyword, startDate, endDate, styleId);

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("limit", limit);
        result.put("offset", offset);

        return ResponseEntity.ok(result);
    }
}
