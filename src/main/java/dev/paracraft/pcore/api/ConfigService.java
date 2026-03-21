package dev.paracraft.pcore.api;

import dev.paracraft.pcore.config.PcoreConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ConfigService {
    Optional<String> get(String pluginId, String path);

    Map<String, Object> getAll(String pluginId);

    String namespacePrefix(String pluginId);

    String tablePrefix(String pluginId);

    boolean isPluginEnabled(String pluginId);

    Set<String> compatiblePlugins();

    Map<String, PcoreConfiguration.CompatiblePlugin> compatiblePluginConfigs();
}
