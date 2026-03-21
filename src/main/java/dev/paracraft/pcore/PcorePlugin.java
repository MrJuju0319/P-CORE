package dev.paracraft.pcore;

import dev.paracraft.pcore.api.PcoreApi;
import dev.paracraft.pcore.config.PcoreConfiguration;
import dev.paracraft.pcore.impl.ConfigServiceImpl;
import dev.paracraft.pcore.impl.DbServiceImpl;
import dev.paracraft.pcore.impl.IdentityServiceImpl;
import dev.paracraft.pcore.impl.PcoreApiImpl;
import dev.paracraft.pcore.impl.RedisServiceImpl;
import dev.paracraft.pcore.impl.SecurityServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PcorePlugin extends JavaPlugin {
    private ExecutorService ioExecutor;
    private ScheduledExecutorService scheduler;

    private DbServiceImpl dbService;
    private RedisServiceImpl redisService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean updated = synchronizeCompatiblePlugins();
        if (updated) {
            saveConfig();
        }

        try {
            PcoreConfiguration config = PcoreConfiguration.from(getConfig());
            validate(config);

            this.ioExecutor = Executors.newFixedThreadPool(Math.max(4, config.db().poolSize()));
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.dbService = new DbServiceImpl(config.db(), ioExecutor);
            this.redisService = new RedisServiceImpl("pcore", config.redis());

            SecurityServiceImpl security = new SecurityServiceImpl(config);
            IdentityServiceImpl identity = new IdentityServiceImpl(config, redisService);
            ConfigServiceImpl configService = new ConfigServiceImpl(getConfig(), config);

            PcoreApi api = new PcoreApiImpl(dbService, redisService, identity, security, configService);
            Bukkit.getServicesManager().register(PcoreApi.class, api, this, ServicePriority.Highest);

            if (config.presenceEnabled()) {
                scheduler.scheduleAtFixedRate(
                        () -> identity.heartbeat().exceptionally(ex -> {
                            getLogger().warning("Heartbeat failed: " + ex.getMessage());
                            return null;
                        }),
                        0,
                        config.heartbeatInterval().toSeconds(),
                        TimeUnit.SECONDS
                );
            }

            getLogger().info("p-core enabled with serverId=" + config.identity().serverId()
                    + " and compatible plugins=" + config.compatiblePlugins().keySet());
        } catch (Exception exception) {
            getLogger().severe("Failed to initialize p-core: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);

        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        if (dbService != null) {
            dbService.close();
        }
        if (redisService != null) {
            redisService.close();
        }
    }

    private boolean synchronizeCompatiblePlugins() {
        boolean updated = false;
        for (PcoreConfiguration.CompatiblePlugin plugin : PcoreConfiguration.loadCompatiblePlugins(getConfig(),
                Set.copyOf(getConfig().getStringList("security.allowedPlugins"))).values()) {
            String basePath = "plugins." + plugin.pluginId();
            if (!getConfig().contains(basePath + ".enabled")) {
                getConfig().set(basePath + ".enabled", plugin.enabled());
                updated = true;
            }
            if (!getConfig().contains(basePath + ".tablePrefix")) {
                getConfig().set(basePath + ".tablePrefix", plugin.tablePrefix());
                updated = true;
            }
            if (!getConfig().contains(basePath + ".cachePrefix")) {
                getConfig().set(basePath + ".cachePrefix", plugin.cachePrefix());
                updated = true;
            }
        }
        return updated;
    }

    private void validate(PcoreConfiguration cfg) {
        if (cfg.identity().serverId().isBlank()) {
            throw new IllegalStateException("server.identity.serverId is mandatory");
        }
        if (cfg.security().sharedSecret().isBlank()) {
            throw new IllegalStateException("security.sharedSecret is mandatory");
        }
    }
}
