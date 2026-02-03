package com.zhangben.backend.util;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;

/**
 * V39: 货币工具类
 * 使用 SaSession 缓存用户货币设置，减少数据库访问
 */
public class CurrencyUtils {

    private static final String SESSION_KEY_CURRENCY = "primaryCurrency";
    private static final String DEFAULT_CURRENCY = "JPY";

    /**
     * 从 Session 获取当前用户的主要货币
     * 如果 Session 中没有，返回默认值 JPY
     */
    public static String getCurrentUserCurrency() {
        try {
            if (!StpUtil.isLogin()) {
                return DEFAULT_CURRENCY;
            }
            SaSession session = StpUtil.getSession();
            String currency = session.getString(SESSION_KEY_CURRENCY);
            return currency != null ? currency : DEFAULT_CURRENCY;
        } catch (Exception e) {
            return DEFAULT_CURRENCY;
        }
    }

    /**
     * 将用户货币存入 Session
     * 通常在登录成功后调用
     */
    public static void setSessionCurrency(String currency) {
        if (StpUtil.isLogin()) {
            SaSession session = StpUtil.getSession();
            session.set(SESSION_KEY_CURRENCY, currency != null ? currency : DEFAULT_CURRENCY);
        }
    }

    /**
     * 更新 Session 中的货币设置
     * 在用户修改货币设置后调用
     */
    public static void updateSessionCurrency(String currency) {
        setSessionCurrency(currency);
    }

    /**
     * 验证货币代码是否有效
     */
    public static boolean isValidCurrency(String currency) {
        if (currency == null || currency.isEmpty()) {
            return false;
        }
        // 支持的货币列表
        return switch (currency.toUpperCase()) {
            case "JPY", "USD", "CNY", "EUR", "GBP", "KRW", "TWD", "HKD", "SGD", "AUD", "CAD" -> true;
            default -> false;
        };
    }

    /**
     * 获取货币符号
     */
    public static String getCurrencySymbol(String currency) {
        if (currency == null) {
            return "¥";
        }
        return switch (currency.toUpperCase()) {
            case "JPY", "CNY" -> "¥";
            case "USD", "CAD", "AUD", "SGD", "HKD", "TWD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "KRW" -> "₩";
            default -> "¥";
        };
    }

    /**
     * 获取默认货币
     */
    public static String getDefaultCurrency() {
        return DEFAULT_CURRENCY;
    }
}
