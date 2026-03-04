# p-core — Data Hub Paper/Folia

`p-core` est un plugin **infrastructure** pour serveurs Minecraft Paper/Folia.
Il centralise l'accès MariaDB + Redis et expose une API interne commune pour les plugins métier (`p-2FA`, `p-fly`, `p-tp`, `p-voteparty`, etc.).

## Objectifs

- Un seul pool MariaDB partagé (HikariCP) par serveur.
- Un seul client Redis partagé (Lettuce) par serveur.
- Configuration centralisée des credentials et namespaces.
- Identification serveur (`serverId`, `group`, `tags`) + heartbeat optionnel.
- Sécurisation des échanges inter-plugins via signature HMAC.
- API asynchrone compatible Paper **et** Folia (retours en `CompletableFuture`).

---

## Installation

## Prérequis

- Java 21+
- Maven 3.9+
- Serveur Paper/Folia récent
- MariaDB + Redis accessibles

## Build

```bash
mvn clean package
```

Le jar final (avec dépendances embarquées via shade) est généré dans `target/p-core-0.1.0-SNAPSHOT.jar` (`original-...jar` = non-shadé).

## Déploiement

1. Copier le jar dans `plugins/`.
2. Démarrer le serveur une première fois pour générer `plugins/p-core/config.yml`.
3. Arrêter le serveur.
4. Mettre à jour la configuration (voir section suivante).
5. Redémarrer.

---

## Configuration (`plugins/p-core/config.yml`)

> **Important**: `server.identity.serverId` et `security.sharedSecret` sont obligatoires.

### Exemple complet

```yaml
server:
  identity:
    serverId: "survival-01"
  presence:
    enabled: true
    heartbeatIntervalSeconds: 5
    ttlSeconds: 15

security:
  sharedSecret: "CHANGE_ME_SUPER_SECRET"
  allowedPlugins: ["p-2FA", "p-fly", "p-tp", "p-voteparty"]

db:
  host: "127.0.0.1"
  port: 3306
  database: "minecraft"
  user: "root"
  password: "password"
  poolSize: 10
  connectionTimeoutMs: 3000
  idleTimeoutMs: 60000
  maxLifetimeMs: 1800000

redis:
  host: "127.0.0.1"
  port: 6379
  password: ""
  ssl: false
  timeoutMs: 1000
  dbIndex: 0

namespaces:
  p-2FA: "p2fa"
  p-fly: "pfly"
  p-tp: "ptp"
  p-voteparty: "pvoteparty"

plugins:
  p-2FA:
    tablePrefix: "p2fa_"
    cachePrefix: "pcore:p-2FA"
```

### Variables principales

- `server.identity.serverId`: identifiant stable du serveur (ex: `survival-01`).
- `server.identity.group`: groupe logique (optionnel, défaut `default`).
- `server.identity.tags`: tags libres (optionnel, défaut `[]`).
- `server.presence.enabled`: active heartbeat Redis.
- `server.presence.heartbeatIntervalSeconds`: fréquence heartbeat.
- `server.presence.ttlSeconds`: durée de vie d’un heartbeat côté Redis.
- `security.sharedSecret`: secret HMAC (ne jamais exposer).
- `security.allowedPlugins`: liste blanche de plugins autorisés.
- `db.host|port|database|user|password`: credentials MariaDB.
- `db.poolSize`: taille maximale du pool Hikari unique.
- `db.minIdle`: connexions minimales conservées par le pool (évite le mode fixed-size si `< poolSize`).
- `db.connectionTimeoutMs|idleTimeoutMs|maxLifetimeMs`: tuning pool/timeouts.
- `redis.host|port|password|ssl|timeoutMs|dbIndex`: connexion Redis.
- `namespaces`: mapping `pluginId -> préfixe logique`.
- `plugins.<pluginId>.*`: options custom accessibles via `ConfigService`.

### Conseils de configuration

- Utiliser un `serverId` **stable** (ne pas générer aléatoirement à chaque boot).
- Utiliser un `sharedSecret` long et aléatoire (rotation manuelle recommandée).
- Garder `poolSize` cohérent avec les capacités MariaDB.
- En environnement multi-serveurs, normaliser `group` et `tags` pour filtrer facilement les instances vivantes.

---



## Dépannage (erreurs courantes)

### `No suitable driver` au démarrage

Si vous voyez `java.sql.SQLException: No suitable driver`:

1. Vérifiez que vous déployez bien le jar `target/p-core-0.1.0-SNAPSHOT.jar` (et pas `original-...jar`).
2. Vérifiez que `db.host`, `db.port`, `db.database` sont corrects.
3. Vérifiez que le serveur peut joindre MariaDB (firewall/réseau).

`p-core` charge explicitement le driver `org.mariadb.jdbc.Driver` et embarque les dépendances nécessaires dans le jar de release.

### Warning Hikari `idleTimeout has no effect because the pool is operating as a fixed size pool`

Ce warning apparaît si `minIdle == poolSize`.

- Réglez `db.minIdle` avec une valeur **inférieure** à `db.poolSize` (ex: `minIdle: 2`, `poolSize: 10`).
- Sinon, c’est informatif uniquement, mais `idleTimeout` ne sera pas appliqué.


## API interne côté plugins clients (détaillée)

Cette section explique **comment un plugin métier** doit intégrer `p-core` proprement.

## 1) Détecter p-core et choisir le mode

Au `onEnable`, votre plugin choisit automatiquement:

- **Managed** (prioritaire): `p-core` présent + `PcoreApi` disponible.
- **Standalone**: fallback local si `p-core` absent ou indisponible.

```java
import dev.paracraft.pcore.api.PcoreApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class MyPluginBootstrap {
    private PcoreApi pcoreApi;
    private boolean managedMode;

    public void init() {
        Plugin pcore = Bukkit.getPluginManager().getPlugin("p-core");
        if (pcore != null && pcore.isEnabled()) {
            this.pcoreApi = Bukkit.getServicesManager().load(PcoreApi.class);
        }

        this.managedMode = (this.pcoreApi != null);
    }

    public boolean isManagedMode() {
        return managedMode;
    }
}
```

## 2) Règles d’intégration obligatoires

- Définir un `pluginId` stable (`p-2FA`, `p-fly`, etc.).
- En **managed mode**, ne jamais créer:
  - pool JDBC/Hikari local
  - client Redis local
- Toute IO passe par les services `p-core` et doit rester asynchrone.
- Le thread principal (Paper) et les region threads (Folia) ne doivent jamais être bloqués (`join()`, `get()` interdits sur le main thread).

## 3) Services disponibles

- `api.db()` → `DbService`
- `api.redis()` → `RedisService`
- `api.identity()` → `IdentityService`
- `api.security()` → `SecurityService`
- `api.config()` → `ConfigService`

## 4) Exemple `DbService`

```java
String pluginId = "p-fly";

api.db().execute(
    pluginId,
    "UPDATE pfly_players SET fly_enabled = ? WHERE uuid = ?",
    java.util.List.of(true, playerUuid.toString())
).thenAccept(rows -> {
    // succès
}).exceptionally(ex -> {
    getLogger().warning("DB execute error: " + ex.getMessage());
    return null;
});

api.db().query(
    pluginId,
    "SELECT fly_enabled FROM pfly_players WHERE uuid = ?",
    java.util.List.of(playerUuid.toString())
).thenAccept(rows -> {
    if (!rows.isEmpty()) {
        Object value = rows.getFirst().get("fly_enabled");
        // traitement
    }
});
```

Compatibilité:

- `Row` est manipulable directement (`row.get("col")`, `row.columns()`).
- `Row` implémente aussi `Map<String, Object>` pour compatibilité avec des plugins existants qui castent encore le résultat en `Map`.

### Transaction

```java
api.db().transaction("p-tp", tx ->
    tx.execute(
        "INSERT INTO ptp_requests(sender_uuid,target_uuid,created_at) VALUES(?,?,?)",
        java.util.List.of(sender, target, System.currentTimeMillis())
    ).thenCompose(inserted ->
        tx.execute(
            "UPDATE ptp_stats SET sent = sent + 1 WHERE uuid = ?",
            java.util.List.of(sender)
        )
    ).thenApply(updated -> true)
);
```

## 5) Exemple `RedisService`

```java
api.redis().set("p-2FA", "session:" + playerUuid, "PENDING", java.time.Duration.ofMinutes(5));

api.redis().get("p-2FA", "session:" + playerUuid)
    .thenAccept(state -> {
        // state = PENDING / OK / null
    });

api.redis().incr("p-voteparty", "votes:today", java.time.Duration.ofDays(1))
    .thenAccept(total -> {
        // compteur journalier
    });

api.redis().publish("pcore:events", "vote:received:" + playerUuid);
```

> Convention de clé appliquée par p-core: `pcore:{pluginId}:{key}`.

## 6) Exemple `IdentityService`

```java
String serverId = api.identity().serverId();
String group = api.identity().group();
java.util.Set<String> tags = api.identity().tags();

api.identity().heartbeat(); // utile si déclenchement manuel souhaité

api.identity().listServers().thenAccept(servers -> {
    // map<serverId, ServerInfo>
});
```

## 7) Exemple `SecurityService`

```java
String pluginId = "p-2FA";
String payload = "{\"action\":\"grant\",\"uuid\":\"...\"}";
long ts = System.currentTimeMillis();
String nonce = java.util.UUID.randomUUID().toString();

String signature = api.security().sign(pluginId, payload, ts, nonce);
boolean ok = api.security().verify(pluginId, payload, ts, nonce, signature);
```

Détails sécurité:

- signature: `HmacSHA256(pluginId|serverId|timestamp|nonce|payload)`
- anti-replay: nonce refusé si déjà vu + fenêtre temporelle de 60s
- allow-list via `security.allowedPlugins`

## 8) Exemple `ConfigService`

```java
String namespace = api.config().namespacePrefix("p-fly");
// ex: "pfly"

api.config().get("p-fly", "features.fastTakeoff")
    .ifPresent(value -> {
        // lire une option custom centralisée
    });

java.util.Map<String, Object> all = api.config().getAll("p-fly");
```

## 9) Pattern conseillé côté plugin métier

Créer une interface locale `StorageProvider` et deux implémentations:

- `ManagedStorageProvider` (utilise `PcoreApi`)
- `StandaloneStorageProvider` (votre impl locale)

Au boot, choisir dynamiquement selon la présence de `p-core`.

Ce pattern évite de dupliquer la logique métier et facilite la migration progressive.

---

## Sécurité HMAC

Signature basée sur:

`pluginId|serverId|timestamp|nonce|payload`

Algorithme: `HmacSHA256`.

Anti-replay:

- fenêtre de validité timestamp: 60s
- nonce unique par plugin sur 60s

---

## Commandes d'exploitation (shell)

### Build & tests

```bash
mvn clean test
mvn clean package
```

### Vérifications utiles

```bash
# afficher dépendances
mvn dependency:tree

# lancer uniquement les tests sécurité
mvn -Dtest=SecurityServiceImplTest test
```

---

## Observabilité recommandée

- Logger la latence des requêtes SQL/Redis.
- Logger les erreurs de signature/nonce.
- Surveiller:
  - saturation du pool Hikari
  - temps de réponse Redis
  - taux d'erreurs par pluginId

---

## Statut actuel

Version initiale livrée + documentation API client détaillée.

- API interne `PcoreApi` + services principaux.
- Implémentation MariaDB partagée (HikariCP).
- Implémentation Redis partagée (Lettuce).
- Identity + heartbeat Redis.
- Security HMAC + anti-replay en mémoire.
- Configuration centralisée + namespaces.
- Tests unitaires de base.
- Guide d’intégration détaillé pour plugins clients.
