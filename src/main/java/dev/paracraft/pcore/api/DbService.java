package dev.paracraft.pcore.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface DbService {
    CompletableFuture<Integer> execute(String pluginId, String sql, List<Object> params);

    CompletableFuture<List<Row>> query(String pluginId, String sql, List<Object> params);

    <T> CompletableFuture<T> transaction(String pluginId, Function<DbTx, CompletableFuture<T>> work);
}
