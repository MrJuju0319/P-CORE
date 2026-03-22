package dev.paracraft.pcore.impl;

import dev.paracraft.pcore.api.ConfigService;
import dev.paracraft.pcore.config.PcoreConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConfigServiceImpl implements ConfigService {
    private final FileConfiguration fileConfiguration;
    private final PcoreConfiguration configuration;

    public ConfigServiceImpl(FileConfiguration fileConfiguration, PcoreConfiguration configuration) {
        this.fileConfiguration = fileConfiguration;
        this.configuration = configuration;
    }

    @Override
    public Optional<String> get(String pluginId, String path) {
        String fullPath = "plugins." + pluginId + "." + path;
        return Optional.ofNullable(fileConfiguration.getString(fullPath));
    }

    @Override
    public Map<String, Object> getAll(String pluginId) {
        ConfigurationSection section = fileConfiguration.getConfigurationSection("plugins." + pluginId);
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> values = new HashMap<>();
        for (String key : section.getKeys(true)) {
            values.put(key, section.get(key));
        }
        return values;
    }

    @Override
    public String namespacePrefix(String pluginId) {
        return configuration.namespaces().getOrDefault(pluginId, pluginId);
    }

    @Override
    public String tablePrefix(String pluginId) {
        return configuration.compatiblePlugins()
                .getOrDefault(pluginId, new PcoreConfiguration.CompatiblePlugin(
                        pluginId,
                        false,
                        PcoreConfiguration.defaultTablePrefix(pluginId),
                        "pcore:" + pluginId
                ))
                .tablePrefix();
    }

    @Override
    public boolean isPluginEnabled(String pluginId) {
        return configuration.compatiblePlugins()
                .getOrDefault(pluginId, new PcoreConfiguration.CompatiblePlugin(
                        pluginId,
                        false,
                        PcoreConfiguration.defaultTablePrefix(pluginId),
                        "pcore:" + pluginId
                ))
                .enabled();
    }

    @Override
    public Set<String> compatiblePlugins() {
        return configuration.compatiblePlugins().keySet();
    }

    @Override
    public Map<String, PcoreConfiguration.CompatiblePlugin> compatiblePluginConfigs() {
        return Collections.unmodifiableMap(configuration.compatiblePlugins());
    }
}
