package dev.paracraft.pcore;

import dev.paracraft.pcore.config.PcoreConfiguration;
import dev.paracraft.pcore.impl.ConfigServiceImpl;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigServiceImplTest {
    @Test
    void namespaceShouldUseMappingOrPluginId() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("plugins.p-2FA.sample", "ok");

        PcoreConfiguration cfg = new PcoreConfiguration(
                new PcoreConfiguration.Identity("srv", "group", Set.of()),
                new PcoreConfiguration.Security("secret", Set.of()),
                new PcoreConfiguration.Db("localhost", 3306, "db", "u", "p", 10,
                        Duration.ofSeconds(3), Duration.ofSeconds(60), Duration.ofSeconds(120)),
                new PcoreConfiguration.Redis("localhost", 6379, "", false, 1000, 0),
                Map.of("p-2FA", "p2fa"),
                true,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15)
        );

        ConfigServiceImpl service = new ConfigServiceImpl(yaml, cfg);

        assertEquals("p2fa", service.namespacePrefix("p-2FA"));
        assertEquals("p-fly", service.namespacePrefix("p-fly"));
    }
}
