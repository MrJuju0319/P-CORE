package dev.paracraft.pcore.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PcoreConfiguration(
        Identity identity,
        Security security,
        Db db,
        Redis redis,
        Map<String, String> namespaces,
        Map<String, CompatiblePlugin> compatiblePlugins,
        boolean presenceEnabled,
        Duration heartbeatInterval,
        Duration heartbeatTtl
) {
    private static final List<String> DEFAULT_COMPATIBLE_PLUGINS = List.of("p-2FA", "p-fly", "p-tp", "p-voteparty");

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
                cfg.getString("db.database", "p-core"),
                cfg.getString("db.user", "root"),
                cfg.getString("db.password", ""),
                cfg.getInt("db.poolSize", 10),
                cfg.getInt("db.minIdle", 2),
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

        Map<String, CompatiblePlugin> compatiblePlugins = loadCompatiblePlugins(cfg, security.allowedPlugins());

        return new PcoreConfiguration(
                identity,
                security,
                db,
                redis,
                namespaces,
                compatiblePlugins,
                cfg.getBoolean("server.presence.enabled", true),
                Duration.ofSeconds(cfg.getLong("server.presence.heartbeatIntervalSeconds", 5)),
                Duration.ofSeconds(cfg.getLong("server.presence.ttlSeconds", 15))
        );
    }

    public static Map<String, CompatiblePlugin> loadCompatiblePlugins(FileConfiguration cfg, Set<String> allowedPlugins) {
        Map<String, CompatiblePlugin> compatiblePlugins = new LinkedHashMap<>();
        Set<String> knownPluginIds = new HashSet<>(DEFAULT_COMPATIBLE_PLUGINS);

        ConfigurationSection section = cfg.getConfigurationSection("plugins");
        if (section != null) {
            knownPluginIds.addAll(section.getKeys(false));
        }

        for (String pluginId : knownPluginIds.stream().sorted().toList()) {
            compatiblePlugins.put(pluginId, compatiblePlugin(cfg, pluginId, allowedPlugins));
        }
        return compatiblePlugins;
    }

    public static CompatiblePlugin compatiblePlugin(FileConfiguration cfg, String pluginId, Set<String> allowedPlugins) {
        String basePath = "plugins." + pluginId;
        boolean enabled = cfg.contains(basePath + ".enabled")
                ? cfg.getBoolean(basePath + ".enabled")
                : allowedPlugins.contains(pluginId);

        return new CompatiblePlugin(
                pluginId,
                enabled,
                cfg.getString(basePath + ".tablePrefix", defaultTablePrefix(pluginId)),
                cfg.getString(basePath + ".cachePrefix", "pcore:" + pluginId)
        );
    }

    public static String defaultTablePrefix(String pluginId) {
        return pluginId + "_";
    }

    public record Identity(String serverId, String group, Set<String> tags) {
    }

    public record Security(String sharedSecret, Set<String> allowedPlugins) {
    }

    public record Db(String host, int port, String database, String user, String password,
                     int poolSize, int minIdle, Duration connectionTimeout, Duration idleTimeout, Duration maxLifetime) {
    }

    public record Redis(String host, int port, String password, boolean ssl, long timeoutMs, int dbIndex) {
    }

    public record CompatiblePlugin(String pluginId, boolean enabled, String tablePrefix, String cachePrefix) {
    }
}
