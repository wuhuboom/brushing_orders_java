package com.order.web.controller.member;

import com.order.common.core.redis.RedisCache;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class RedisCacheExistingKeysTest {
    @Test
    void pipelineUsesConfiguredKeySerializationAndNeverFetchesTokenValues() {
        RedisTemplate template = mock(RedisTemplate.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisKeyCommands keys = mock(RedisKeyCommands.class);
        when(connection.keyCommands()).thenReturn(keys);
        doReturn(new StringRedisSerializer()).when(template).getKeySerializer();
        when(template.executePipelined(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            assertNull(callback.doInRedis(connection));
            return List.of(Boolean.TRUE, Boolean.FALSE, 1L);
        });
        RedisCache cache = new RedisCache();
        cache.redisTemplate = template;

        assertEquals(Set.of("front:user_tokens:7", "user:tokens:中文"), cache.existingKeys(List.of(
                "front:user_tokens:7", "front:user_tokens:8", "user:tokens:中文")));

        verify(keys).exists("front:user_tokens:7".getBytes(StandardCharsets.UTF_8));
        verify(keys).exists("front:user_tokens:8".getBytes(StandardCharsets.UTF_8));
        verify(keys).exists("user:tokens:中文".getBytes(StandardCharsets.UTF_8));
        verify(connection, times(3)).keyCommands();
        verifyNoMoreInteractions(connection, keys);
    }

    @Test
    void emptyAndOversizedBatchesDoNotContactRedis() {
        RedisTemplate template = mock(RedisTemplate.class);
        RedisCache cache = new RedisCache();
        cache.redisTemplate = template;
        assertEquals(Set.of(), cache.existingKeys(List.of()));
        assertThrows(IllegalArgumentException.class, () -> cache.existingKeys(
                IntStream.range(0, 1001).mapToObj(Integer::toString).toList()));
        verifyNoInteractions(template);
    }

    @Test
    void incompletePipelineCannotSilentlyReportMembersAsOffline() {
        RedisTemplate template = mock(RedisTemplate.class);
        when(template.executePipelined(any(RedisCallback.class))).thenReturn(List.of());
        RedisCache cache = new RedisCache();
        cache.redisTemplate = template;
        assertThrows(IllegalStateException.class, () -> cache.existingKeys(List.of("front:user_tokens:7")));
    }
}
