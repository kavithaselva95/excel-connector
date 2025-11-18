package com.zeenea.sdk.connector;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
    private final Map<String, String> map = new HashMap<>();
    public void put(String k, String v) { map.put(k, v); }
    public String get(String k) { return map.get(k); }
}
