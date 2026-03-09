package dev.paracraft.pcore.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.paracraft.pcore.api.DbService;
import dev.paracraft.pcore.api.DbTx;
import dev.paracraft.pcore.api.Row;
import dev.paracraft.pcore.config.PcoreConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class DbServiceImpl implements DbService, AutoCloseable {
    private final HikariDataSource dataSource;
    private final ExecutorService executor;

    public DbServiceImpl(PcoreConfiguration.Db db, ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(db, "db");

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mariadb://" + db.host() + ":" + db.port() + "/" + db.database());
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setUsername(db.user());
        hikari.setPassword(db.password());
        hikari.setMaximumPoolSize(db.poolSize());
        hikari.setMinimumIdle(Math.max(0, Math.min(db.minIdle(), db.poolSize())));
        hikari.setConnectionTimeout(db.connectionTimeout().toMillis());
        hikari.setIdleTimeout(db.idleTimeout().toMillis());
        hikari.setMaxLifetime(db.maxLifetime().toMillis());
        hikari.setPoolName("p-core-mariadb");
        hikari.setInitializationFailTimeout(5000L);

        // Ensure the driver is available in the shaded jar.
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("MariaDB JDBC driver not found in plugin classpath. Ensure shaded jar is used.", exception);
        }

        this.dataSource = new HikariDataSource(hikari);
    }

    @Override
    public CompletableFuture<Integer> execute(String pluginId, String sql, List<Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            validate(pluginId, sql);
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                // Prevent runaway queries from stalling the pool.
                statement.setQueryTimeout(5);
                return statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException("DB execute failed for plugin " + pluginId, exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Row>> query(String pluginId, String sql, List<Object> params) {
        return CompletableFuture.supplyAsync(() -> {
            validate(pluginId, sql);
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                // Prevent runaway queries from stalling the pool.
                statement.setQueryTimeout(5);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return toRows(resultSet);
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("DB query failed for plugin " + pluginId, exception);
            }
        }, executor);
    }

    @Override
    public <T> CompletableFuture<T> transaction(String pluginId, Function<DbTx, CompletableFuture<T>> work) {
        return CompletableFuture.supplyAsync(() -> {
            if (pluginId == null || pluginId.isBlank()) {
                throw new IllegalArgumentException("pluginId is required");
            }
            if (work == null) {
                throw new IllegalArgumentException("work is required");
            }

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                DbTx tx = new DbTxImpl(connection);

                try {
                    T result = work.apply(tx).join();
                    connection.commit();
                    return result;
                } catch (Exception exception) {
                    try {
                        connection.rollback();
                    } catch (SQLException ignored) {
                    }
                    throw exception;
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Transaction failed for plugin " + pluginId, exception);
            }
        }, executor);
    }

    private void validate(String pluginId, String sql) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId is required");
        }
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql is required");
        }
    }

    private static void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private static List<Row> toRows(ResultSet resultSet) throws SQLException {
        List<Row> rows = new ArrayList<>();
        int columnCount = resultSet.getMetaData().getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> data = new HashMap<>();
            for (int column = 1; column <= columnCount; column++) {
                data.put(resultSet.getMetaData().getColumnLabel(column), resultSet.getObject(column));
            }
            rows.add(new Row(data));
        }
        return rows;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static class DbTxImpl implements DbTx {
        private final Connection connection;

        private DbTxImpl(Connection connection) {
            this.connection = connection;
        }

        @Override
        public CompletableFuture<Integer> execute(String sql, List<Object> params) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                // Prevent runaway queries from stalling the pool.
                statement.setQueryTimeout(5);
                return CompletableFuture.completedFuture(statement.executeUpdate());
            } catch (SQLException exception) {
                CompletableFuture<Integer> future = new CompletableFuture<>();
                future.completeExceptionally(exception);
                return future;
            }
        }

        @Override
        public CompletableFuture<List<Row>> query(String sql, List<Object> params) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, params);
                // Prevent runaway queries from stalling the pool.
                statement.setQueryTimeout(5);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return CompletableFuture.completedFuture(toRows(resultSet));
                }
            } catch (SQLException exception) {
                CompletableFuture<List<Row>> future = new CompletableFuture<>();
                future.completeExceptionally(exception);
                return future;
            }
        }
    }
}
