package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.OutcomeCreateRequest;
import com.zhangben.backend.dto.RecentOutcomeItem;
import com.zhangben.backend.service.OutcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        outcomeService.createOutcome(req);
        return ResponseEntity.ok("记录创建成功");
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
}
