package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.*;
import com.zhangben.backend.service.DebtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debt")
public class DebtController {

    @Autowired
    private DebtService debtService;

    /**
     * 1. 用户自身全部欠款总览
     *    - 自身应收总金额
     *    - 自身应还总金额
     */
    @GetMapping("/summary")
    public MyDebtSummaryResponse getMySummary() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getMyDebtSummary(userId);
    }

    /**
     * 2. 单个债权人欠款一览
     *    - 债权人姓名
     *    - 债权人款项总金额
     *    - 债权人款项明细（分类、备注、金额、时间、地点）
     */
    @GetMapping("/creditor/{creditorId}")
    public CreditorDebtOverviewResponse getDebtByCreditor(@PathVariable Integer creditorId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getDebtByCreditor(userId, creditorId);
    }

    /**
     * 3. 还款
     *    - 选择债权人
     *    - 输入还款金额（Long）
     *    - 还款方式（styleId）
     *    - 备注
     *    - payDatetime = LocalDateTime.now()
     */
    @PostMapping("/repay")
    public String repay(@RequestBody RepayRequest req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        debtService.repay(req, userId);
        return "Repay recorded";
    }

    /**
     * 4. 自身债权一览
     *    - 列出所有欠我钱的人
     *    - 每个人的欠款总额
     *    - 欠款明细（分类、备注、金额、时间、地点）
     */
    @GetMapping("/my-credit")
    public List<MyCreditOverviewItem> getMyCredit() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getMyCreditOverview(userId);
    }
}