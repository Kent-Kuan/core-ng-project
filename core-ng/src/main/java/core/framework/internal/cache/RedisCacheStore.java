package core.framework.internal.cache;

import core.framework.internal.redis.RedisException;
import core.framework.internal.redis.RedisImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;

/**
 * @author neo
 */
public class RedisCacheStore implements CacheStore {
    private final Logger logger = LoggerFactory.getLogger(RedisCacheStore.class);

    private final RedisImpl redis;

    public RedisCacheStore(RedisImpl redis) {
        this.redis = redis;
    }

    @Override
    public byte[] get(String key) {
        try {
            return redis.getBytes(key);
        } catch (UncheckedIOException | RedisException e) {
            logger.warn("failed to connect to redis, error={}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<String, byte[]> getAll(String... keys) {
        try {
            return redis.multiGetBytes(keys);
        } catch (UncheckedIOException | RedisException e) {
            logger.warn("failed to connect to redis, error={}", e.getMessage(), e);
            return Map.of();
        }
    }

    @Override
    public void put(String key, byte[] value, Duration expiration) {
        try {
            redis.set(key, value, expiration, false);
        } catch (UncheckedIOException | RedisException e) {
            logger.warn("failed to connect to redis, error={}", e.getMessage(), e);
        }
    }

    @Override
    public void putAll(Map<String, byte[]> values, Duration expiration) {
        try {
            redis.multiSet(values, expiration);
        } catch (UncheckedIOException | RedisException e) {
            logger.warn("failed to connect to redis, error={}", e.getMessage(), e);
        }
    }

    @Override
    public void delete(String... keys) {
        try {
            redis.del(keys);
        } catch (UncheckedIOException | RedisException e) {
            logger.warn("failed to connect to redis, error={}", e.getMessage(), e);
        }
    }
}
