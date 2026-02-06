package com.zhangben.backend.config;

import cn.dev33.satoken.dao.SaTokenDao;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 双缓存 DAO: Caffeine (L1) + Redis (L2)
 *
 * 读路径: Caffeine → miss → Redis → 回填 Caffeine → return
 * 写路径: 写 Caffeine + 写 Redis (Redis 失败仅 warn)
 * 删路径: 双删
 *
 * Redis 宕机时自动降级到 Caffeine-only 模式，已登录用户在 TTL 内可正常访问。
 */
@Component
@Primary
@SuppressWarnings({"unchecked", "rawtypes"})
public class SaTokenDaoTwoLevel implements SaTokenDao {

    private static final Logger logger = LoggerFactory.getLogger(SaTokenDaoTwoLevel.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate objectRedisTemplate;
    private final RedisHealthMonitor redisHealthMonitor;

    /** String 缓存: token → session-id 等映射 */
    private final Cache<String, String> stringCache;
    /** Object 缓存: session 对象 */
    private final Cache<String, Object> objectCache;
    /** Timeout 缓存: key → 过期时间戳(ms) */
    private final Cache<String, Long> timeoutCache;

    public SaTokenDaoTwoLevel(StringRedisTemplate stringRedisTemplate,
                               @Qualifier("redisTemplate") RedisTemplate objectRedisTemplate,
                               RedisHealthMonitor redisHealthMonitor) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectRedisTemplate = objectRedisTemplate;
        this.redisHealthMonitor = redisHealthMonitor;

        this.stringCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        this.objectCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        this.timeoutCache = Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        logger.info("【SaTokenDao】双缓存初始化完成: Caffeine L1 + Redis L2");
    }

    // ==================== String 读写 ====================

    @Override
    public String get(String key) {
        // L1
        String value = stringCache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                value = stringRedisTemplate.opsForValue().get(key);
                if (value != null) {
                    stringCache.put(key, value);
                }
                return value;
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis get 失败, key={}: {}", key, e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void set(String key, String value, long timeout) {
        // L1
        stringCache.put(key, value);
        recordTimeout(key, timeout);
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                if (timeout == SaTokenDao.NEVER_EXPIRE) {
                    stringRedisTemplate.opsForValue().set(key, value);
                } else {
                    stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis set 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public void update(String key, String value) {
        // L1
        stringCache.put(key, value);
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                long expire = getExpireFromRedis(key);
                if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
                    return;
                }
                if (expire == SaTokenDao.NEVER_EXPIRE) {
                    stringRedisTemplate.opsForValue().set(key, value);
                } else {
                    stringRedisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis update 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public void delete(String key) {
        // 双删
        stringCache.invalidate(key);
        objectCache.invalidate(key);
        timeoutCache.invalidate(key);
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                stringRedisTemplate.delete(key);
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis delete 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public long getTimeout(String key) {
        // L1
        Long expireAt = timeoutCache.getIfPresent(key);
        if (expireAt != null) {
            if (expireAt == -1L) {
                return SaTokenDao.NEVER_EXPIRE;
            }
            long remaining = (expireAt - System.currentTimeMillis()) / 1000;
            return remaining > 0 ? remaining : SaTokenDao.NOT_VALUE_EXPIRE;
        }
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                return getExpireFromRedis(key);
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis getTimeout 失败, key={}: {}", key, e.getMessage());
            }
        }
        return SaTokenDao.NOT_VALUE_EXPIRE;
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        recordTimeout(key, timeout);
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                if (timeout == SaTokenDao.NEVER_EXPIRE) {
                    stringRedisTemplate.persist(key);
                } else {
                    stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis updateTimeout 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    // ==================== Object 读写 ====================

    @Override
    public Object getObject(String key) {
        // L1
        Object value = objectCache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                value = objectRedisTemplate.opsForValue().get(key);
                if (value != null) {
                    objectCache.put(key, value);
                }
                return value;
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis getObject 失败, key={}: {}", key, e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        // L1
        objectCache.put(key, object);
        recordTimeout(key, timeout);
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                if (timeout == SaTokenDao.NEVER_EXPIRE) {
                    objectRedisTemplate.opsForValue().set(key, object);
                } else {
                    objectRedisTemplate.opsForValue().set(key, object, timeout, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis setObject 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public void updateObject(String key, Object object) {
        // L1
        objectCache.put(key, object);
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                long expire = getExpireFromRedis(key);
                if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
                    return;
                }
                if (expire == SaTokenDao.NEVER_EXPIRE) {
                    objectRedisTemplate.opsForValue().set(key, object);
                } else {
                    objectRedisTemplate.opsForValue().set(key, object, expire, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis updateObject 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public void deleteObject(String key) {
        objectCache.invalidate(key);
        timeoutCache.invalidate(key);
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                objectRedisTemplate.delete(key);
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis deleteObject 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public long getObjectTimeout(String key) {
        // L1
        Long expireAt = timeoutCache.getIfPresent(key);
        if (expireAt != null) {
            if (expireAt == -1L) {
                return SaTokenDao.NEVER_EXPIRE;
            }
            long remaining = (expireAt - System.currentTimeMillis()) / 1000;
            return remaining > 0 ? remaining : SaTokenDao.NOT_VALUE_EXPIRE;
        }
        // L2
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                return getExpireFromRedis(key);
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis getObjectTimeout 失败, key={}: {}", key, e.getMessage());
            }
        }
        return SaTokenDao.NOT_VALUE_EXPIRE;
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        recordTimeout(key, timeout);
        if (redisHealthMonitor.isRedisAvailable()) {
            try {
                if (timeout == SaTokenDao.NEVER_EXPIRE) {
                    objectRedisTemplate.persist(key);
                } else {
                    objectRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.warn("【SaTokenDao】Redis updateObjectTimeout 失败, key={}: {}", key, e.getMessage());
            }
        }
    }

    // ==================== 搜索 (仅 Redis) ====================

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        if (!redisHealthMonitor.isRedisAvailable()) {
            logger.warn("【SaTokenDao】Redis 不可用，searchData 降级返回空列表");
            return new ArrayList<>();
        }
        try {
            String pattern = prefix + "*" + keyword + "*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return new ArrayList<>();
            }
            List<String> list = new ArrayList<>(keys);
            return list.subList(Math.min(start, list.size()), Math.min(start + size, list.size()));
        } catch (Exception e) {
            logger.warn("【SaTokenDao】Redis searchData 失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==================== 内部工具 ====================

    /**
     * 记录 key 的过期时间到 Caffeine
     */
    private void recordTimeout(String key, long timeout) {
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            timeoutCache.put(key, -1L);
        } else if (timeout > 0) {
            timeoutCache.put(key, System.currentTimeMillis() + timeout * 1000);
        }
    }

    /**
     * 从 Redis 获取 key 的过期时间 (秒)
     */
    private long getExpireFromRedis(String key) {
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (expire == null || expire == -2) {
            return SaTokenDao.NOT_VALUE_EXPIRE;
        }
        if (expire == -1) {
            return SaTokenDao.NEVER_EXPIRE;
        }
        return expire;
    }
}
