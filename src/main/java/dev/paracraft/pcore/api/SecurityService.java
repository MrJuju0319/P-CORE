package dev.paracraft.pcore.api;

public interface SecurityService {
    String pluginId();

    String sign(String pluginId, String payload, long timestamp, String nonce);

    boolean verify(String pluginId, String payload, long timestamp, String nonce, String signature);

    boolean isPluginAllowed(String pluginId);
}
