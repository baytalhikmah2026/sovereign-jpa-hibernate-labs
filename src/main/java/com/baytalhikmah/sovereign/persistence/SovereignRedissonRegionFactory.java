package com.baytalhikmah.sovereign.persistence;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.hibernate.RedissonRegionFactory;

/** Hibernate's second-level cache in Redis (via Redisson), with the Redis address taken from the JPA properties. */
public class SovereignRedissonRegionFactory extends RedissonRegionFactory {

    public static final String REDIS_ADDRESS = "sovereign.cache.redis-address";

    @Override
    @SuppressWarnings("rawtypes")
    protected RedissonClient createRedissonClient(StandardServiceRegistry registry, Map properties) {
        Object address = properties.get(REDIS_ADDRESS);
        Config config = new Config();
        config.useSingleServer().setAddress(address == null ? "redis://127.0.0.1:6379" : address.toString());
        return Redisson.create(config);
    }

}
