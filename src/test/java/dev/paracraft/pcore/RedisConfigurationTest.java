package dev.paracraft.pcore;

import dev.paracraft.pcore.config.PcoreConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConfigurationTest {
    @Test
    void shouldSupportDedicatedRedisTimeoutSettings() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("redis.host", "127.0.0.1");
        yaml.set("redis.port", 6379);
        yaml.set("redis.connectTimeoutMs", 4000);
        yaml.set("redis.commandTimeoutMs", 7000);
        yaml.set("redis.autoReconnect", true);
        yaml.set("redis.pingBeforeActivateConnection", false);

        PcoreConfiguration configuration = PcoreConfiguration.from(yaml);

        assertEquals(Duration.ofSeconds(4), configuration.redis().connectTimeout());
        assertEquals(Duration.ofSeconds(7), configuration.redis().commandTimeout());
        assertTrue(configuration.redis().autoReconnect());
        assertFalse(configuration.redis().pingBeforeActivateConnection());
    }

    @Test
    void shouldFallbackToLegacyRedisTimeoutSetting() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("redis.timeoutMs", 2500);

        PcoreConfiguration configuration = PcoreConfiguration.from(yaml);

        assertEquals(Duration.ofMillis(2500), configuration.redis().connectTimeout());
        assertEquals(Duration.ofMillis(2500), configuration.redis().commandTimeout());
        assertTrue(configuration.redis().autoReconnect());
        assertTrue(configuration.redis().pingBeforeActivateConnection());
    }
}
