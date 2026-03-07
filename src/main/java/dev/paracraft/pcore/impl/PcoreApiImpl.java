package dev.paracraft.pcore.impl;

import dev.paracraft.pcore.api.ConfigService;
import dev.paracraft.pcore.api.DbService;
import dev.paracraft.pcore.api.IdentityService;
import dev.paracraft.pcore.api.PcoreApi;
import dev.paracraft.pcore.api.RedisService;
import dev.paracraft.pcore.api.SecurityService;

public record PcoreApiImpl(
        DbService db,
        RedisService redis,
        IdentityService identity,
        SecurityService security,
        ConfigService config
) implements PcoreApi {
}
