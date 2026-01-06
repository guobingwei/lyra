package com.lyra.agent.agent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of ModeRegistry.
 */
public class SimpleModeRegistry implements ModeRegistry {
    private final Map<String, Mode> modes;

    public SimpleModeRegistry() {
        this.modes = new ConcurrentHashMap<>();
    }

    @Override
    public Mode get(String name) {
        return modes.get(name);
    }

    @Override
    public void register(Mode mode) {
        modes.put(mode.name(), mode);
    }

    @Override
    public Collection<Mode> all() {
        return modes.values();
    }
}