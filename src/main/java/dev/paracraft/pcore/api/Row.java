package dev.paracraft.pcore.api;

import java.util.Map;

public record Row(Map<String, Object> columns) {
    public Object get(String column) {
        return columns.get(column);
    }
}
