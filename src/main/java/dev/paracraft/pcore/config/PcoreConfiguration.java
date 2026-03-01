package dev.paracraft.pcore.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record PcoreConfiguration(
        Identity identity,
        Security security,
        Db db,
        Redis redis,
        Map<String, String> namespaces,
        boolean presenceEnabled,
        Duration heartbeatInterval,
        Duration heartbeatTtl
) {
    public static PcoreConfiguration from(FileConfiguration cfg) {
        Set<String> tags = new HashSet<>(cfg.getStringList("server.identity.tags"));

        Identity identity = new Identity(
                cfg.getString("server.identity.serverId", ""),
                cfg.getString("server.identity.group", "default"),
                tags
        );

        Security security = new Security(
                cfg.getString("security.sharedSecret", ""),
                new HashSet<>(cfg.getStringList("security.allowedPlugins"))
        );

        Db db = new Db(
                cfg.getString("db.host", "127.0.0.1"),
                cfg.getInt("db.port", 3306),
                cfg.getString("db.database", "minecraft"),
                cfg.getString("db.user", "root"),
                cfg.getString("db.password", ""),
                cfg.getInt("db.poolSize", 10),
                Duration.ofMillis(cfg.getLong("db.connectionTimeoutMs", 3000)),
                Duration.ofMillis(cfg.getLong("db.idleTimeoutMs", 60000)),
                Duration.ofMillis(cfg.getLong("db.maxLifetimeMs", 1800000))
        );

        Redis redis = new Redis(
                cfg.getString("redis.host", "127.0.0.1"),
                cfg.getInt("redis.port", 6379),
                cfg.getString("redis.password", ""),
                cfg.getBoolean("redis.ssl", false),
                cfg.getLong("redis.timeoutMs", 1000),
                cfg.getInt("redis.dbIndex", 0)
        );

        Map<String, String> namespaces = new HashMap<>();
        ConfigurationSection section = cfg.getConfigurationSection("namespaces");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                namespaces.put(key, section.getString(key, key));
            }
        }

        return new PcoreConfiguration(
                identity,
                security,
                db,
                redis,
                namespaces,
                cfg.getBoolean("server.presence.enabled", true),
                Duration.ofSeconds(cfg.getLong("server.presence.heartbeatIntervalSeconds", 5)),
                Duration.ofSeconds(cfg.getLong("server.presence.ttlSeconds", 15))
        );
    }

    public record Identity(String serverId, String group, Set<String> tags) {
    }

    public record Security(String sharedSecret, Set<String> allowedPlugins) {
    }

    public record Db(String host, int port, String database, String user, String password,
                     int poolSize, Duration connectionTimeout, Duration idleTimeout, Duration maxLifetime) {
    }

    public record Redis(String host, int port, String password, boolean ssl, long timeoutMs, int dbIndex) {
    }
}
