package com.browserautomation.action;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the parameters for a browser action, parsed from LLM output.
 */
public class ActionParameters {

    private final Map<String, Object> params;

    public ActionParameters() {
        this.params = new HashMap<>();
    }

    public ActionParameters(Map<String, Object> params) {
        this.params = params != null ? new HashMap<>(params) : new HashMap<>();
    }

    public String getString(String key) {
        Object val = params.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    public String getString(String key, String defaultValue) {
        String val = getString(key);
        return val != null ? val : defaultValue;
    }

    public Integer getInt(String key) {
        Object val = params.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public int getInt(String key, int defaultValue) {
        Integer val = getInt(key);
        return val != null ? val : defaultValue;
    }

    public Boolean getBoolean(String key) {
        Object val = params.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return null;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean val = getBoolean(key);
        return val != null ? val : defaultValue;
    }

    public Object get(String key) {
        return params.get(key);
    }

    public void put(String key, Object value) {
        params.put(key, value);
    }

    public boolean has(String key) {
        return params.containsKey(key);
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(params);
    }
}
