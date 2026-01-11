package com.example.ignite.solutions.lab08;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;

/**
 * Lab 08 Exercise 3: Eviction Policies
 *
 * Demonstrates:
 * - LRU eviction policy for on-heap memory management
 * - On-heap cache size limits
 * - Automatic eviction of least recently used entries
 */
public class Lab08Eviction {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Eviction Policies Lab ===\n");

            // LRU Eviction Policy
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("evictionCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setOnheapCacheEnabled(true);

            LruEvictionPolicy<Integer, String> evictionPolicy = new LruEvictionPolicy<>();
            evictionPolicy.setMaxSize(100);  // Keep only 100 entries in heap

            cfg.setEvictionPolicyFactory(() -> evictionPolicy);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            System.out.println("Cache created with LRU eviction (max 100 entries on-heap)");
            System.out.println("Adding 200 entries...\n");

            // Add 200 entries
            for (int i = 0; i < 200; i++) {
                cache.put(i, "Value-" + i);
            }

            System.out.println("Total cache size: " + cache.size());
            System.out.println("On-heap size: " + cache.localSize());
            System.out.println("(Others moved to off-heap or disk)\n");

            // Access some entries to make them "recent"
            System.out.println("Accessing entries 0-9 to make them recent...");
            for (int i = 0; i < 10; i++) {
                cache.get(i);
            }

            System.out.println("\n=== Eviction Strategies ===");
            System.out.println("1. LRU (Least Recently Used):");
            System.out.println("   - Evicts entries not accessed recently");
            System.out.println("   - Good for frequently accessed hot data");
            System.out.println("");
            System.out.println("2. FIFO (First In First Out):");
            System.out.println("   - Evicts oldest entries first");
            System.out.println("   - Simpler, predictable behavior");
            System.out.println("");
            System.out.println("3. Random:");
            System.out.println("   - Randomly evicts entries");
            System.out.println("   - Low overhead");

            System.out.println("\n=== Eviction vs Expiry ===");
            System.out.println("Eviction: Memory management (size-based)");
            System.out.println("Expiry: Data lifecycle (time-based)");
            System.out.println("Both can work together!");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
