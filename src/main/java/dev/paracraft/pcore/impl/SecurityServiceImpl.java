package dev.paracraft.pcore.impl;

import dev.paracraft.pcore.api.SecurityService;
import dev.paracraft.pcore.config.PcoreConfiguration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityServiceImpl implements SecurityService {
    private static final String HMAC_ALGO = "HmacSHA256";

    private final PcoreConfiguration configuration;
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();
    private final Duration nonceTtl = Duration.ofSeconds(60);

    public SecurityServiceImpl(PcoreConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String pluginId() {
        return "p-core";
    }

    @Override
    public String sign(String pluginId, String payload, long timestamp, String nonce) {
        String message = String.join("|", pluginId, configuration.identity().serverId(), String.valueOf(timestamp), nonce, payload);
        return hmac(message, configuration.security().sharedSecret());
    }

    @Override
    public boolean verify(String pluginId, String payload, long timestamp, String nonce, String signature) {
        if (!isPluginAllowed(pluginId)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > nonceTtl.toMillis()) {
            return false;
        }

        cleanupNonces(now);
        String nonceKey = pluginId + ":" + nonce;
        if (nonceCache.putIfAbsent(nonceKey, now) != null) {
            return false;
        }

        String expected = sign(pluginId, payload, timestamp, nonce);
        return expected.equals(signature);
    }

    @Override
    public boolean isPluginAllowed(String pluginId) {
        PcoreConfiguration.CompatiblePlugin compatiblePlugin = configuration.compatiblePlugins().get(pluginId);
        if (compatiblePlugin != null) {
            return compatiblePlugin.enabled();
        }
        if (configuration.security().allowedPlugins().isEmpty()) {
            return true;
        }
        return configuration.security().allowedPlugins().contains(pluginId);
    }

    private void cleanupNonces(long now) {
        nonceCache.entrySet().removeIf(entry -> (now - entry.getValue()) > nonceTtl.toMillis());
    }

    private String hmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create HMAC", exception);
        }
    }
}
