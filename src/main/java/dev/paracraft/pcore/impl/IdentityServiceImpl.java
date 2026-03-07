package dev.paracraft.pcore.impl;

import dev.paracraft.pcore.api.IdentityService;
import dev.paracraft.pcore.api.ServerInfo;
import dev.paracraft.pcore.config.PcoreConfiguration;
import io.lettuce.core.api.async.RedisAsyncCommands;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IdentityServiceImpl implements IdentityService {
    private final PcoreConfiguration configuration;
    private final RedisServiceImpl redis;

    public IdentityServiceImpl(PcoreConfiguration configuration, RedisServiceImpl redis) {
        this.configuration = configuration;
        this.redis = redis;
    }

    @Override
    public String serverId() {
        return configuration.identity().serverId();
    }

    @Override
    public String group() {
        return configuration.identity().group();
    }

    @Override
    public Set<String> tags() {
        return Collections.unmodifiableSet(configuration.identity().tags());
    }

    @Override
    public CompletableFuture<Void> heartbeat() {
        if (!configuration.presenceEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        String payload = group() + "|" + String.join(",", tags()) + "|" + Instant.now().toEpochMilli();
        return redis.set("pcore", "servers:" + serverId(), payload, configuration.heartbeatTtl());
    }

    @Override
    public CompletableFuture<Map<String, ServerInfo>> listServers() {
        RedisAsyncCommands<String, String> commands = redis.rawCommands();
        return commands.keys("pcore:pcore:servers:*")
                .toCompletableFuture()
                .thenCompose(keys -> {
                    Map<String, CompletableFuture<String>> futures = new HashMap<>();
                    for (String key : keys) {
                        futures.put(key, commands.get(key).toCompletableFuture());
                    }
                    return CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new))
                            .thenApply(ignored -> futures.entrySet().stream().collect(Collectors.toMap(
                                    entry -> entry.getKey().replace("pcore:pcore:servers:", ""),
                                    entry -> parse(entry.getKey(), entry.getValue().join())
                            )));
                });
    }

    private ServerInfo parse(String key, String payload) {
        if (payload == null || payload.isBlank()) {
            return new ServerInfo(key, "unknown", Set.of(), Instant.EPOCH);
        }
        String[] parts = payload.split("\\|");
        Set<String> parsedTags = parts.length > 1 && !parts[1].isBlank()
                ? Set.of(parts[1].split(","))
                : Set.of();
        Instant lastSeen = parts.length > 2 ? Instant.ofEpochMilli(Long.parseLong(parts[2])) : Instant.now();
        return new ServerInfo(key, parts[0], parsedTags, lastSeen);
    }
}
