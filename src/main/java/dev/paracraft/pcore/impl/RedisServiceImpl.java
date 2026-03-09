package dev.paracraft.pcore.impl;

import dev.paracraft.pcore.api.RedisService;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RedisServiceImpl implements RedisService, AutoCloseable {
    private static final String ROOT_PREFIX = "pcore";

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisAsyncCommands<String, String> commands;
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

    public RedisServiceImpl(String namespace, dev.paracraft.pcore.config.PcoreConfiguration.Redis redis) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(redis.host())
                .withPort(redis.port())
                .withDatabase(redis.dbIndex())
                .withTimeout(Duration.ofMillis(redis.timeoutMs()));
        if (!redis.password().isBlank()) {
            builder.withPassword(redis.password().toCharArray());
        }
        if (redis.ssl()) {
            builder.withSsl(true);
        }
        RedisURI uri = builder.build();
        this.client = RedisClient.create(uri);
        this.connection = client.connect();
        this.pubSubConnection = client.connectPubSub();
        this.commands = connection.async();
    }

    @Override
    public CompletableFuture<String> get(String pluginId, String key) {
        return commands.get(namespaced(pluginId, key)).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> set(String pluginId, String key, String value, Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be > 0");
        }
        SetArgs args = SetArgs.Builder.ex(ttl);
        return commands.set(namespaced(pluginId, key), value, args)
                .thenAccept(ignored -> {})
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Long> incr(String pluginId, String key, Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be > 0");
        }
        String namespacedKey = namespaced(pluginId, key);
        return commands.incr(namespacedKey)
                .thenCompose(value -> commands.expire(namespacedKey, ttl).thenApply(expire -> value))
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> publish(String channel, String payload) {
        return commands.publish(channel, payload)
                .thenAccept(ignored -> {})
                .toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> subscribe(String channel, Consumer<String> handler) {
        pubSubConnection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String ch, String message) {
                if (channel.equals(ch)) {
                    handler.accept(message);
                }
            }
        });
        return pubSubConnection.async().subscribe(channel)
                .thenAccept(ignored -> {})
                .toCompletableFuture();
    }

    private String namespaced(String pluginId, String key) {
        String pid = (pluginId == null) ? "" : pluginId.trim();
        String k = (key == null) ? "" : key.trim();
        if (pid.isEmpty()) {
            throw new IllegalArgumentException("pluginId is required");
        }
        if (k.isEmpty()) {
            throw new IllegalArgumentException("key is required");
        }
        // Keep the namespace stable: pcore:<pluginId>:<key>
        return ROOT_PREFIX + ":" + pid + ":" + k;
    }

    public RedisAsyncCommands<String, String> rawCommands() {
        return commands;
    }

    @Override
    public void close() {
        pubSubConnection.close();
        connection.close();
        client.shutdown();
    }
}
