package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service demonstrating Spring dependency injection with Ignite.
 *
 * This shows how to use Ignite in a typical Spring application where
 * the Ignite instance is autowired into service classes.
 */
@Service
public class CacheService {

    private final Ignite ignite;

    @Autowired
    public CacheService(Ignite ignite) {
        this.ignite = ignite;
    }

    public String getNodeName() {
        return ignite.name();
    }

    public int getServerCount() {
        return ignite.cluster().forServers().nodes().size();
    }

    public <K, V> V get(String cacheName, K key) {
        IgniteCache<K, V> cache = ignite.cache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache not found: " + cacheName);
        }
        return cache.get(key);
    }

    public <K, V> void put(String cacheName, K key, V value) {
        IgniteCache<K, V> cache = ignite.cache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache not found: " + cacheName);
        }
        cache.put(key, value);
    }

    public int getCacheSize(String cacheName) {
        IgniteCache<?, ?> cache = ignite.cache(cacheName);
        if (cache == null) {
            return -1;
        }
        return cache.size();
    }

    public void printCacheContents(String cacheName, int count) {
        System.out.println("\n" + cacheName + ":");
        for (int i = 1; i <= count; i++) {
            Object value = get(cacheName, i);
            System.out.println("  " + i + " -> " + value);
        }
    }
}
