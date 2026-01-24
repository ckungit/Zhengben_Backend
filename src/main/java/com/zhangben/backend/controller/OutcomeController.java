package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.OutcomeCreateRequest;
import com.zhangben.backend.service.OutcomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outcome")
public class OutcomeController {

    @Autowired
    private OutcomeService outcomeService;

    @PostMapping("/create")
    public ResponseEntity<?> createOutcome(@RequestBody OutcomeCreateRequest req) {

        StpUtil.checkLogin(); // 必须登录

        outcomeService.createOutcome(req);

        return ResponseEntity.ok("Outcome created");
    }
}