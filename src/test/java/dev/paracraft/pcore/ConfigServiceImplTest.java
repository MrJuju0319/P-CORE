package dev.paracraft.pcore;

import dev.paracraft.pcore.config.PcoreConfiguration;
import dev.paracraft.pcore.impl.ConfigServiceImpl;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigServiceImplTest {
    @Test
    void namespaceShouldUseMappingOrPluginId() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("plugins.p-2FA.sample", "ok");
        yaml.set("plugins.p-2FA.enabled", true);
        yaml.set("plugins.p-2FA.tablePrefix", "p-2FA_");

        PcoreConfiguration cfg = new PcoreConfiguration(
                new PcoreConfiguration.Identity("srv", "group", Set.of()),
                new PcoreConfiguration.Security("secret", Set.of()),
                new PcoreConfiguration.Db("localhost", 3306, "db", "u", "p", 10, 2,
                        Duration.ofSeconds(3), Duration.ofSeconds(60), Duration.ofSeconds(120)),
                new PcoreConfiguration.Redis("localhost", 6379, "", false, Duration.ofSeconds(5), Duration.ofSeconds(5), true, true, 0),
                Map.of("p-2FA", "p-2FA"),
                Map.of(
                        "p-2FA", new PcoreConfiguration.CompatiblePlugin("p-2FA", true, "p-2FA_", "pcore:p-2FA"),
                        "p-fly", new PcoreConfiguration.CompatiblePlugin("p-fly", false, "p-fly_", "pcore:p-fly")
                ),
                true,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15)
        );

        ConfigServiceImpl service = new ConfigServiceImpl(yaml, cfg);

        assertEquals("p-2FA", service.namespacePrefix("p-2FA"));
        assertEquals("p-fly", service.namespacePrefix("p-fly"));
        assertEquals("p-2FA_", service.tablePrefix("p-2FA"));
        assertEquals("p-fly_", service.tablePrefix("p-fly"));
        assertTrue(service.isPluginEnabled("p-2FA"));
        assertFalse(service.isPluginEnabled("p-fly"));
    }
}
