package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhangben.backend.mapper.StatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsMapper statsMapper;

    /**
     * 获取消费统计总览
     */
    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(defaultValue = "6") Integer months) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Map<String, Object> result = new HashMap<>();

        // 总览数据
        Map<String, Object> overview = statsMapper.getOverviewStats(userId, months);
        result.put("totalExpense", overview != null ? overview.get("totalExpense") : 0);
        result.put("expenseCount", overview != null ? overview.get("expenseCount") : 0);
        result.put("avgExpense", overview != null ? overview.get("avgExpense") : 0);

        return result;
    }

    /**
     * 获取月度消费趋势
     */
    @GetMapping("/monthly")
    public List<Map<String, Object>> monthly(@RequestParam(defaultValue = "6") Integer months) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        List<Map<String, Object>> data = statsMapper.getMonthlyStats(userId, months);
        
        // 格式化金额（分转元）
        for (Map<String, Object> item : data) {
            Object amount = item.get("totalAmount");
            if (amount != null) {
                item.put("totalAmount", ((Number) amount).doubleValue() / 100);
            }
        }
        
        return data;
    }

    /**
     * 获取分类消费统计
     */
    @GetMapping("/category")
    public List<Map<String, Object>> category(@RequestParam(defaultValue = "6") Integer months) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        List<Map<String, Object>> data = statsMapper.getCategoryStats(userId, months);
        
        // 格式化金额
        for (Map<String, Object> item : data) {
            Object amount = item.get("totalAmount");
            if (amount != null) {
                item.put("totalAmount", ((Number) amount).doubleValue() / 100);
            }
        }
        
        return data;
    }

    /**
     * 获取消费伙伴排行
     */
    @GetMapping("/partners")
    public List<Map<String, Object>> partners(@RequestParam(defaultValue = "6") Integer months) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        List<Map<String, Object>> data = statsMapper.getPartnerStats(userId, months);
        
        // 格式化金额
        for (Map<String, Object> item : data) {
            Object amount = item.get("totalAmount");
            if (amount != null) {
                item.put("totalAmount", ((Number) amount).doubleValue() / 100);
            }
        }
        
        return data;
    }

    /**
     * 获取每日消费趋势
     */
    @GetMapping("/daily")
    public List<Map<String, Object>> daily(@RequestParam(defaultValue = "30") Integer days) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        List<Map<String, Object>> data = statsMapper.getDailyAvgStats(userId, days);
        
        // 格式化金额
        for (Map<String, Object> item : data) {
            Object amount = item.get("totalAmount");
            if (amount != null) {
                item.put("totalAmount", ((Number) amount).doubleValue() / 100);
            }
        }
        
        return data;
    }

    /**
     * 获取完整统计数据（一次性获取所有）
     */
    @GetMapping("/all")
    public Map<String, Object> all(@RequestParam(defaultValue = "6") Integer months) {
        StpUtil.checkLogin();
        Integer userId = StpUtil.getLoginIdAsInt();

        Map<String, Object> result = new HashMap<>();

        // 总览
        Map<String, Object> overview = statsMapper.getOverviewStats(userId, months);
        if (overview != null) {
            Object total = overview.get("totalExpense");
            Object avg = overview.get("avgExpense");
            overview.put("totalExpense", total != null ? ((Number) total).doubleValue() / 100 : 0);
            overview.put("avgExpense", avg != null ? ((Number) avg).doubleValue() / 100 : 0);
        }
        result.put("overview", overview);

        // 月度趋势
        List<Map<String, Object>> monthly = statsMapper.getMonthlyStats(userId, months);
        for (Map<String, Object> item : monthly) {
            Object amount = item.get("totalAmount");
            if (amount != null) {
                item.put("totalAmount", ((Number) amount).doubleValue() / 100);
            }
        }
        result.put("monthly", monthly);

        // 分类统计
        List<Map<String, Object>> category = statsMapper.getCategoryStats(userId, months);
        for (Map<String, Object> item : category) {
            Object amount = item.get("totalAmount");
            if (amount != null) {
                item.put("totalAmount", ((Number) amount).doubleValue() / 100);
            }
        }
        result.put("category", category);

        // 消费伙伴
        List<Map<String, Object>> partners = statsMapper.getPartnerStats(userId, months);
        for (Map<String, Object> item : partners) {
            Object amount = item.get("totalAmount");
            if (amount != null) {
                item.put("totalAmount", ((Number) amount).doubleValue() / 100);
            }
        }
        result.put("partners", partners);

        return result;
    }
}
