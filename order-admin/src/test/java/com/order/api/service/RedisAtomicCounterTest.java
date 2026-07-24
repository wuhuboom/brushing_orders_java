package com.order.api.service;

import com.order.common.core.redis.RedisCache;
import com.order.framework.config.RedisConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class RedisAtomicCounterTest {
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisCache redisCache;

    @BeforeAll
    static void createClient() {
        connectionFactory = new LettuceConnectionFactory(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        RedisTemplate<Object, Object> template =
                new RedisConfig().redisTemplate(connectionFactory);

        redisCache = new RedisCache();
        redisCache.redisTemplate = template;
    }

    @AfterAll
    static void closeClient() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void concurrentIncrementsAreNotLostAndFirstIncrementHasTtl() throws Exception {
        String key = "test:atomic-counter";
        redisCache.deleteObject(key);
        int tasks = 64;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < tasks; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return redisCache.incrementWithExpire(key, 1, TimeUnit.MINUTES);
                }));
            }
            start.countDown();
            Set<Long> returnedValues = new HashSet<>();
            for (Future<Long> future : futures) {
                returnedValues.add(future.get());
            }

            assertEquals(tasks, returnedValues.size());
            assertTrue(returnedValues.contains(1L));
            assertTrue(returnedValues.contains((long) tasks));
            Number storedCount = redisCache.getCacheObject(key);
            assertEquals(tasks, storedCount.intValue());
            long ttl = redisCache.getExpire(key);
            assertTrue(ttl > 0 && ttl <= 60, "counter must have a bounded TTL");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void counterExpiresAutomatically() throws Exception {
        String key = "test:expiring-counter";
        redisCache.deleteObject(key);

        assertEquals(1L, redisCache.incrementWithExpire(key, 1, TimeUnit.SECONDS));
        assertTrue(redisCache.getExpire(key) > 0);

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (Boolean.TRUE.equals(redisCache.hasKey(key)) && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertFalse(Boolean.TRUE.equals(redisCache.hasKey(key)));
    }
}
