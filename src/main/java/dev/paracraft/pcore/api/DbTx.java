package dev.paracraft.pcore.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DbTx {
    CompletableFuture<Integer> execute(String sql, List<Object> params);

    CompletableFuture<List<Row>> query(String sql, List<Object> params);
}
