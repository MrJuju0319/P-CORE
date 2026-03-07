package dev.paracraft.pcore.api;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface RedisService {
    CompletableFuture<String> get(String pluginId, String key);

    CompletableFuture<Void> set(String pluginId, String key, String value, Duration ttl);

    CompletableFuture<Long> incr(String pluginId, String key, Duration ttl);

    CompletableFuture<Void> publish(String channel, String payload);

    CompletableFuture<Void> subscribe(String channel, Consumer<String> handler);
}
