package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.dto.MyCreditOverviewItem;
import com.zhangben.backend.dto.PredictionResponse;
import com.zhangben.backend.dto.SettlementResponse;
import com.zhangben.backend.mapper.StatsMapper;
import com.zhangben.backend.mapper.UserMapper;
import com.zhangben.backend.model.User;
import com.zhangben.backend.service.DebtService;
import com.zhangben.backend.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    @Autowired
    private StatsMapper statsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DebtService debtService;

    @Autowired
    private PredictionService predictionService;

    private String getUserLanguage(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user != null && user.getPreferredLanguage() != null && !user.getPreferredLanguage().isEmpty()) {
            return user.getPreferredLanguage();
        }
        return "zh-CN";
    }

    /**
     * 获取消费洞察聚合数据
     */
    @GetMapping("/all")
    public Map<String, Object> getInsightsAll() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();
        String language = getUserLanguage(userId);

        Map<String, Object> result = new HashMap<>();

        // 1. 月度趋势（6个月）- 合并支出和应付
        List<Map<String, Object>> monthlyPaid = statsMapper.getMonthlyStats(userId, 6);
        List<Map<String, Object>> monthlyOwed = statsMapper.getMonthlyOwedStats(userId, 6);

        // 构建应付按月索引
        Map<String, Double> owedByMonth = new HashMap<>();
        for (Map<String, Object> item : monthlyOwed) {
            String month = (String) item.get("month");
            Object amount = item.get("totalAmount");
            owedByMonth.put(month, amount != null ? ((Number) amount).doubleValue() / 100.0 : 0);
        }

        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (Map<String, Object> item : monthlyPaid) {
            Map<String, Object> trend = new HashMap<>();
            String month = (String) item.get("month");
            Object paidAmount = item.get("totalAmount");
            double totalPaid = paidAmount != null ? ((Number) paidAmount).doubleValue() / 100.0 : 0;
            double totalOwed = owedByMonth.getOrDefault(month, 0.0);

            trend.put("month", month);
            trend.put("totalPaid", totalPaid);
            trend.put("totalOwed", totalOwed);
            monthlyTrend.add(trend);
        }

        // 补充仅有应付没有支出的月份
        for (Map.Entry<String, Double> e : owedByMonth.entrySet()) {
            boolean exists = monthlyTrend.stream()
                    .anyMatch(t -> e.getKey().equals(t.get("month")));
            if (!exists) {
                Map<String, Object> trend = new HashMap<>();
                trend.put("month", e.getKey());
                trend.put("totalPaid", 0.0);
                trend.put("totalOwed", e.getValue());
                monthlyTrend.add(trend);
            }
        }
        monthlyTrend.sort((a, b) -> ((String) a.get("month")).compareTo((String) b.get("month")));
        result.put("monthlyTrend", monthlyTrend);

        // 2. 最大债务人（别人欠我最多的人）
        List<MyCreditOverviewItem> credits = debtService.getMyCreditOverview(userId);
        if (!credits.isEmpty()) {
            MyCreditOverviewItem top = credits.stream()
                    .max(Comparator.comparingLong(MyCreditOverviewItem::getTotalAmount))
                    .orElse(null);
            if (top != null) {
                Map<String, Object> topDebtor = new HashMap<>();
                topDebtor.put("userId", top.getDebtorId());
                topDebtor.put("userName", top.getDebtorName());
                topDebtor.put("avatarUrl", top.getDebtorAvatarUrl());
                topDebtor.put("amount", top.getTotalAmount() / 100.0);
                result.put("topDebtor", topDebtor);
            } else {
                result.put("topDebtor", null);
            }
        } else {
            result.put("topDebtor", null);
        }

        // 3. 本月最高消费分类
        List<Map<String, Object>> categoryStats = statsMapper.getCategoryStats(userId, 1, language);
        if (!categoryStats.isEmpty()) {
            Map<String, Object> topCat = categoryStats.get(0);
            Map<String, Object> topCategory = new HashMap<>();
            topCategory.put("categoryName", topCat.get("category"));
            Object catAmount = topCat.get("totalAmount");
            topCategory.put("amount", catAmount != null ? ((Number) catAmount).doubleValue() / 100.0 : 0);
            topCategory.put("count", topCat.get("count"));
            result.put("topCategory", topCategory);
        } else {
            result.put("topCategory", null);
        }

        // 4. 本月余额概览
        Map<String, Object> overview = statsMapper.getOverviewStats(userId, 1);
        Map<String, Object> monthlyBalance = new HashMap<>();
        Object totalExpense = overview != null ? overview.get("totalExpense") : 0;
        monthlyBalance.put("totalPaid", totalExpense != null ? ((Number) totalExpense).doubleValue() / 100.0 : 0);

        List<Map<String, Object>> thisMonthOwed = statsMapper.getMonthlyOwedStats(userId, 1);
        double totalOwedThisMonth = 0;
        for (Map<String, Object> item : thisMonthOwed) {
            Object amt = item.get("totalAmount");
            if (amt != null) totalOwedThisMonth += ((Number) amt).doubleValue() / 100.0;
        }
        monthlyBalance.put("totalOwed", totalOwedThisMonth);
        result.put("monthlyBalance", monthlyBalance);

        // 5. 最优结算方案
        SettlementResponse settlement = debtService.getMinimizedSettlements(userId);
        result.put("settlement", settlement);

        return result;
    }

    /**
     * V44: 支出预测
     */
    @GetMapping("/prediction")
    public PredictionResponse getPrediction() {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();
        String language = getUserLanguage(userId);
        return predictionService.getPrediction(userId, language);
    }
}
