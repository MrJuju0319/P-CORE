package dev.paracraft.pcore.api;

import java.time.Instant;
import java.util.Set;

public record ServerInfo(String serverId, String group, Set<String> tags, Instant lastSeen) {
}
