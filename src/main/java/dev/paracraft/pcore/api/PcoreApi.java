package dev.paracraft.pcore.api;

public interface PcoreApi {
    DbService db();

    RedisService redis();

    IdentityService identity();

    SecurityService security();

    ConfigService config();
}
