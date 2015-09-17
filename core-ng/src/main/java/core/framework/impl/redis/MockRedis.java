package core.framework.impl.redis;

import core.framework.api.redis.Redis;
import core.framework.api.util.Maps;

import java.time.Duration;
import java.util.Map;

/**
 * @author neo
 */
public final class MockRedis implements Redis {
    private final Map<String, Object> store = Maps.newConcurrentHashMap();

    @Override
    public String get(String key) {
        Value value = (Value) store.get(key);
        if (value.expired(System.currentTimeMillis())) {
            store.remove(key);
            return null;
        }
        return value.value;
    }

    @Override
    public void set(String key, String value) {
        store.put(key, new Value(value, -1));
    }

    @Override
    public void set(String key, String value, Duration expiration) {
        store.put(key, new Value(value, System.currentTimeMillis() + expiration.toMillis()));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration expiration) {
        Object previous = store.putIfAbsent(key, new Value(value, System.currentTimeMillis() + expiration.toMillis()));
        return previous == null;
    }

    @Override
    public void expire(String key, Duration duration) {
        Value value = (Value) store.get(key);
        value.expirationTime = System.currentTimeMillis() + duration.toMillis();
    }

    @Override
    public void del(String key) {
        store.remove(key);
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        @SuppressWarnings("unchecked")
        Map<String, String> value = (Map<String, String>) store.get(key);
        return value;
    }

    @Override
    public void hmset(String key, Map<String, String> value) {
        @SuppressWarnings("unchecked")
        Map<String, String> hash = (Map<String, String>) store.computeIfAbsent(key, key1 -> Maps.newConcurrentHashMap());
        hash.putAll(value);
    }

    static class Value {
        String value;
        long expirationTime;

        Value(String value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }

        boolean expired(long now) {
            return now >= expirationTime;
        }
    }
}