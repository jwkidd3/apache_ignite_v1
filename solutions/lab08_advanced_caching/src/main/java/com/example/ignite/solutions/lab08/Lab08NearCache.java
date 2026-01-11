package com.example.ignite.solutions.lab08;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;

/**
 * Lab 08 Exercise 1: Near Cache Configuration
 *
 * Demonstrates:
 * - Client-side near cache for reduced latency
 * - LRU eviction policy for near cache
 * - Performance benefits of local caching
 */
public class Lab08NearCache {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Lab08NearCache <client|server>");
            System.exit(1);
        }

        boolean isClient = args[0].equals("client");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(isClient ? "client-node" : "server-node");
        cfg.setClientMode(isClient);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Near Cache Lab ===");
            System.out.println("Mode: " + (isClient ? "CLIENT" : "SERVER") + "\n");

            if (!isClient) {
                // Server node: Create cache
                CacheConfiguration<Integer, String> cacheCfg =
                    new CacheConfiguration<>("dataCache");
                cacheCfg.setCacheMode(CacheMode.PARTITIONED);
                cacheCfg.setBackups(1);

                IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

                // Populate data
                System.out.println("Server: Populating cache with 1000 entries...");
                for (int i = 0; i < 1000; i++) {
                    cache.put(i, "Value-" + i);
                }
                System.out.println("Server: Data loaded\n");

            } else {
                // Client node: Create near cache
                NearCacheConfiguration<Integer, String> nearCfg =
                    new NearCacheConfiguration<>();
                nearCfg.setNearEvictionPolicyFactory(
                    () -> new LruEvictionPolicy<>(100));

                IgniteCache<Integer, String> cache =
                    ignite.getOrCreateNearCache("dataCache", nearCfg);

                System.out.println("Client: Near cache created (max 100 entries)\n");

                // First access - from server
                System.out.println("=== First Access (from server) ===");
                long start = System.currentTimeMillis();
                String value1 = cache.get(100);
                long time1 = System.currentTimeMillis() - start;
                System.out.println("Value: " + value1);
                System.out.println("Time: " + time1 + " ms\n");

                // Second access - from near cache
                System.out.println("=== Second Access (from near cache) ===");
                start = System.currentTimeMillis();
                String value2 = cache.get(100);
                long time2 = System.currentTimeMillis() - start;
                System.out.println("Value: " + value2);
                System.out.println("Time: " + time2 + " ms");
                if (time2 > 0) {
                    System.out.println("Speedup: " + (float)time1/time2 + "x faster\n");
                } else {
                    System.out.println("Speedup: Near instant (cached locally)\n");
                }

                // Access multiple entries
                System.out.println("=== Accessing Multiple Entries ===");
                for (int i = 0; i < 50; i++) {
                    cache.get(i);
                }
                System.out.println("50 entries now in near cache\n");

                // Check near cache size
                System.out.println("Near cache size: " + cache.localSize());

                System.out.println("\n=== Near Cache Benefits ===");
                System.out.println("- Reduced network latency");
                System.out.println("- Lower load on server nodes");
                System.out.println("- Better performance for frequently accessed data");
                System.out.println("- Automatic invalidation on updates");
            }

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
