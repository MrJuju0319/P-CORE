package dev.paracraft.pcore.api;

import java.util.HashMap;
import java.util.Map;

/**
 * DB row wrapper that is also a Map for backward compatibility with plugins
 * that still cast query results to Map<String, Object>.
 */
public class Row extends HashMap<String, Object> {
    public Row() {
        super();
    }

    public Row(Map<String, Object> columns) {
        super(columns);
    }

    public Map<String, Object> columns() {
        return this;
    }

    public Object get(String column) {
        return super.get(column);
    }
}
