package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.*;
import com.zhangben.backend.service.DebtService;
import com.zhangben.backend.service.NudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debt")
public class DebtController {

    @Autowired
    private DebtService debtService;

    @Autowired
    private NudgeService nudgeService;

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
    public RepayFifoResponse repay(@RequestBody RepayRequest req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.repayWithFifo(req, userId);
    }

    /**
     * 4. 自身债权一览（别人欠我的钱）
     *    - 列出所有欠我钱的人
     *    - 每个人的欠款总额
     *    - 欠款明细（分类、备注、金额、时间、地点）
     */
    @GetMapping("/my-credit")
    public List<MyCreditOverviewItem> getMyCredit() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getMyCreditOverview(userId);
    }

    /**
     * 5. 自身欠款一览（我欠别人的钱）
     *    - 列出我欠钱的所有人
     *    - 每个人的欠款总额
     *    - 欠款明细
     */
    @GetMapping("/my-debt")
    public List<MyDebtOverviewItem> getMyDebt() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getMyDebtOverview(userId);
    }

    /**
     * V30: 获取待确认的还款列表（作为债权人）
     */
    @GetMapping("/pending-repayments")
    public List<PendingRepaymentItem> getPendingRepayments() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getPendingRepayments(userId);
    }

    /**
     * V30: 确认单笔还款
     */
    @PostMapping("/repayment/{id}/confirm")
    public String confirmRepayment(@PathVariable("id") Integer repaymentId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        debtService.confirmRepayment(repaymentId, userId);
        return "确认成功";
    }

    /**
     * V30: 批量确认还款
     */
    @PostMapping("/repayments/batch-confirm")
    public String batchConfirmRepayments(@RequestBody List<Integer> repaymentIds) {
        Integer userId = StpUtil.getLoginIdAsInt();
        debtService.batchConfirmRepayments(repaymentIds, userId);
        return "批量确认成功";
    }

    /**
     * V32: 获取我发起的待确认还款（作为债务人）
     */
    @GetMapping("/my-pending-repayments")
    public List<MyPendingRepaymentItem> getMyPendingRepayments() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getMyPendingRepayments(userId);
    }

    /**
     * V35: 批量还款（支持代人还款）
     */
    @PostMapping("/batch-repay")
    public String batchRepay(@RequestBody BatchRepayRequest req) {
        Integer userId = StpUtil.getLoginIdAsInt();
        debtService.batchRepay(req, userId);
        return "批量还款成功";
    }

    /**
     * V35: 获取某债权人下好友债务人的欠款信息
     * 用于代人还款时选择要帮谁还款（只返回好友）
     */
    @GetMapping("/debtors-for-creditor/{creditorId}")
    public List<DebtorDebtInfo> getDebtorsForCreditor(@PathVariable Integer creditorId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getDebtorsForCreditor(creditorId, userId);
    }

    /**
     * V35: 获取有好友欠款的债权人列表
     * 用于帮他人还款时选择要还给谁
     */
    @GetMapping("/creditors-for-on-behalf")
    public List<CreditorForOnBehalfItem> getCreditorsForOnBehalf() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getCreditorsWithFriendDebts(userId);
    }

    /**
     * 获取最优结算方案（全局债务简化）
     */
    @GetMapping("/settlements")
    public SettlementResponse getMinimizedSettlements() {
        Integer userId = StpUtil.getLoginIdAsInt();
        return debtService.getMinimizedSettlements(userId);
    }

    /**
     * V41: 催促还款
     * 向债务人发送还款提醒（24小时内只能催促一次）
     */
    @PostMapping("/nudge/{debtorId}")
    public NudgeService.NudgeResult sendNudge(@PathVariable Integer debtorId,
                                               @RequestParam(defaultValue = "false") boolean anonymous) {
        Integer userId = StpUtil.getLoginIdAsInt();
        return nudgeService.sendNudge(userId, debtorId, anonymous);
    }

    /**
     * V41: 检查是否可以催促
     */
    @GetMapping("/nudge/{debtorId}/status")
    public NudgeStatusResponse getNudgeStatus(@PathVariable Integer debtorId) {
        Integer userId = StpUtil.getLoginIdAsInt();
        boolean canNudge = nudgeService.canNudge(userId, debtorId);
        long nextNudgeTime = nudgeService.getNextNudgeTime(userId, debtorId);
        return new NudgeStatusResponse(canNudge, nextNudgeTime);
    }

    /**
     * V41: 催促状态响应
     */
    public static class NudgeStatusResponse {
        private boolean canNudge;
        private long nextNudgeTime;

        public NudgeStatusResponse(boolean canNudge, long nextNudgeTime) {
            this.canNudge = canNudge;
            this.nextNudgeTime = nextNudgeTime;
        }

        public boolean isCanNudge() {
            return canNudge;
        }

        public long getNextNudgeTime() {
            return nextNudgeTime;
        }
    }
}
