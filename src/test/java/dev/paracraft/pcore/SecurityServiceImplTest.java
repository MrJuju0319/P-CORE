package dev.paracraft.pcore;

import dev.paracraft.pcore.config.PcoreConfiguration;
import dev.paracraft.pcore.impl.SecurityServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityServiceImplTest {
    private SecurityServiceImpl newService() {
        PcoreConfiguration cfg = new PcoreConfiguration(
                new PcoreConfiguration.Identity("srv-01", "survival", Set.of("eu")),
                new PcoreConfiguration.Security("top-secret", Set.of("p-2FA")),
                new PcoreConfiguration.Db("localhost", 3306, "db", "u", "p", 10, 2,
                        Duration.ofSeconds(3), Duration.ofSeconds(60), Duration.ofSeconds(120)),
                new PcoreConfiguration.Redis("localhost", 6379, "", false, 1000, 0),
                Map.of("p-2FA", "p-2FA"),
                Map.of(
                        "p-2FA", new PcoreConfiguration.CompatiblePlugin("p-2FA", true, "p-2FA_", "pcore:p-2FA"),
                        "p-fly", new PcoreConfiguration.CompatiblePlugin("p-fly", false, "p-fly_", "pcore:p-fly")
                ),
                true,
                Duration.ofSeconds(5),
                Duration.ofSeconds(15)
        );
        return new SecurityServiceImpl(cfg);
    }

    @Test
    void signAndVerifyShouldPass() {
        SecurityServiceImpl service = newService();
        long ts = System.currentTimeMillis();
        String nonce = "abc-123";
        String payload = "{\"value\":1}";

        String signature = service.sign("p-2FA", payload, ts, nonce);

        assertTrue(service.verify("p-2FA", payload, ts, nonce, signature));
    }

    @Test
    void verifyShouldRejectReplay() {
        SecurityServiceImpl service = newService();
        long ts = System.currentTimeMillis();
        String nonce = "same-nonce";
        String payload = "hello";
        String signature = service.sign("p-2FA", payload, ts, nonce);

        assertTrue(service.verify("p-2FA", payload, ts, nonce, signature));
        assertFalse(service.verify("p-2FA", payload, ts, nonce, signature));
    }

    @Test
    void verifyShouldRejectDisabledPlugin() {
        SecurityServiceImpl service = newService();
        long ts = System.currentTimeMillis();
        String signature = service.sign("p-fly", "x", ts, "nonce");
        assertFalse(service.verify("p-fly", "x", ts, "nonce", signature));
    }
}
