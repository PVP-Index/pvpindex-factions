package com.pvpindex.factions.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic key-value registry for holding named instances.
 *
 * <p>Instances are stored by their class type. Typed retrieval via
 * {@link #get(Class)} eliminates unchecked casts at call sites.
 *
 * @param <K> key type
 * @param <V> base value type
 */
public class Registry<K, V> {

    private final Map<K, V> store = new LinkedHashMap<>();

    /**
     * Register a value under the given key.
     * Overwrites any existing entry for that key.
     *
     * @param key   registry key
     * @param value value to register
     */
    public void register(final K key, final V value) {
        store.put(key, value);
    }

    /**
     * Retrieve a value by key.
     *
     * @param key registry key
     * @return the stored value, or {@link Optional#empty()} if absent
     */
    public Optional<V> get(final K key) {
        return Optional.ofNullable(store.get(key));
    }

    /**
     * Retrieve a value by key and cast it to the expected type.
     *
     * @param key  registry key
     * @param type expected class of the value
     * @param <T>  expected type
     * @return the value cast to {@code T}, or {@link Optional#empty()} if absent or wrong type
     */
    public <T extends V> Optional<T> get(final K key, final Class<T> type) {
        final V value = store.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /** @return {@code true} if a value is registered under {@code key}. */
    public boolean contains(final K key) {
        return store.containsKey(key);
    }

    /** Remove the entry for {@code key}. */
    public void unregister(final K key) {
        store.remove(key);
    }

    /** Remove all entries. */
    public void clear() {
        store.clear();
    }

    /** @return number of registered entries. */
    public int size() {
        return store.size();
    }

    /**
     * Provide subclass access to the backing map without exposing it publicly.
     *
     * @return the mutable backing map
     */
    protected java.util.Map<K, V> store() {
        return store;
    }
}
