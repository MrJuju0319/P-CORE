package dev.paracraft.pcore.api;

import java.util.Map;
import java.util.Optional;

public interface ConfigService {
    Optional<String> get(String pluginId, String path);

    Map<String, Object> getAll(String pluginId);

    String namespacePrefix(String pluginId);
}
