package com.lyra.agent.agent;

import java.util.Collection;

/**
 * Registry for managing different agent modes.
 * Allows registration and retrieval of modes by name.
 */
public interface ModeRegistry {
    /**
     * Get a mode by its name.
     * @param name the mode name
     * @return the mode instance, or null if not found
     */
    Mode get(String name);

    /**
     * Register a mode with the registry.
     * @param mode the mode to register
     */
    void register(Mode mode);

    /**
     * Get all registered modes.
     * @return collection of all registered modes
     */
    Collection<Mode> all();
}