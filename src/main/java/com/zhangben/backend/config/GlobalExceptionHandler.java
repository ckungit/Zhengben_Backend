package com.zhangben.backend.config;

import cn.dev33.satoken.exception.NotLoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理未登录异常 - 静默返回401，不打印堆栈
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Map<String, Object>> handleNotLoginException(NotLoginException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", "未登录或登录已过期");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * V25: 处理参数校验异常 (Jakarta Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);

        // 收集所有校验错误信息
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        result.put("message", message.isEmpty() ? "参数校验失败" : message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理 Redis 连接异常 - 返回 503 服务暂时不可用
     * 作为安全网，防止未被 SaTokenDaoTwoLevel catch 的 Redis 异常泄漏
     */
    @ExceptionHandler({RedisConnectionFailureException.class, RedisSystemException.class})
    public ResponseEntity<Map<String, Object>> handleRedisException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", "服务暂时不可用，请稍后重试");
        logger.error("Redis 异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }

    /**
     * 处理SQL异常 - 不暴露数据库错误详情
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, Object>> handleSQLException(SQLException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "数据操作失败，请稍后重试");
        // 记录完整SQL错误用于调试，但不返回给前端
        logger.error("SQL异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理Spring数据访问异常 - 不暴露数据库错误详情
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "数据操作失败，请稍后重试");
        // 记录完整错误用于调试，但不返回给前端
        logger.error("数据访问异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理业务异常 - RuntimeException
     * 过滤敏感信息，防止SQL等内部错误泄露
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);

        String message = e.getMessage();
        // 检查是否包含敏感信息（SQL、堆栈跟踪等）
        if (message != null && containsSensitiveInfo(message)) {
            result.put("message", "操作失败，请稍后重试");
            logger.error("业务异常（包含敏感信息）: ", e);
        } else {
            result.put("message", message != null ? message : "操作失败");
            logger.warn("业务异常: {}", message);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 检查消息是否包含敏感信息
     */
    private boolean containsSensitiveInfo(String message) {
        if (message == null) return false;
        String lowerMsg = message.toLowerCase();
        return lowerMsg.contains("sql")
            || lowerMsg.contains("jdbc")
            || lowerMsg.contains("mybatis")
            || lowerMsg.contains("hibernate")
            || lowerMsg.contains("database")
            || lowerMsg.contains("table")
            || lowerMsg.contains("column")
            || lowerMsg.contains("insert")
            || lowerMsg.contains("update")
            || lowerMsg.contains("delete")
            || lowerMsg.contains("select")
            || lowerMsg.contains("exception")
            || lowerMsg.contains("stacktrace")
            || lowerMsg.contains("at com.")
            || lowerMsg.contains("at org.")
            || lowerMsg.contains("at java.");
    }

    /**
     * 处理所有其他异常 - 返回通用错误信息
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "服务器内部错误");
        // 记录完整错误日志用于调试
        logger.error("未知异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}