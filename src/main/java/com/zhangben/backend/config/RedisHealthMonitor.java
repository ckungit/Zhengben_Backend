package com.zhangben.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 健康监控
 * 定期 PING Redis，跟踪可用状态，供 SaTokenDaoTwoLevel 查询以跳过已知宕机的 Redis 调用
 */
@Component
public class RedisHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthMonitor.class);

    private final RedisConnectionFactory connectionFactory;
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);

    /** Redis 宕机起始时间 (毫秒), 0 表示正常 */
    private volatile long downSince = 0;
    /** 上次告警时间 */
    private volatile long lastAlertTime = 0;
    /** 持续宕机告警间隔: 5 分钟 */
    private static final long ALERT_INTERVAL_MS = 5 * 60 * 1000;

    public RedisHealthMonitor(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * 每 30 秒检查一次 Redis 连接
     */
    @Scheduled(fixedRate = 30000)
    public void checkRedisHealth() {
        try {
            connectionFactory.getConnection().ping();
            if (!redisAvailable.getAndSet(true)) {
                long downtimeMs = System.currentTimeMillis() - downSince;
                logger.warn("【Redis 监控】RECOVERED - Redis 已恢复，宕机时长: {}s", downtimeMs / 1000);
                downSince = 0;
                lastAlertTime = 0;
            }
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (redisAvailable.getAndSet(false)) {
                // 首次检测到宕机
                downSince = now;
                lastAlertTime = now;
                logger.error("【Redis 监控】DOWN - Redis 连接失败: {}", e.getMessage());
            } else if (now - lastAlertTime >= ALERT_INTERVAL_MS) {
                // 持续宕机，每 5 分钟重复告警
                long downtimeMs = now - downSince;
                lastAlertTime = now;
                logger.error("【Redis 监控】STILL DOWN - Redis 持续不可用，已宕机 {}s", downtimeMs / 1000);
            }
        }
    }

    /**
     * 查询 Redis 是否可用
     */
    public boolean isRedisAvailable() {
        return redisAvailable.get();
    }
}
