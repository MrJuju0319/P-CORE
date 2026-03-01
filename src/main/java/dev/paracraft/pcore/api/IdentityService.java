package dev.paracraft.pcore.api;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface IdentityService {
    String serverId();

    String group();

    Set<String> tags();

    CompletableFuture<Void> heartbeat();

    CompletableFuture<Map<String, ServerInfo>> listServers();
}
