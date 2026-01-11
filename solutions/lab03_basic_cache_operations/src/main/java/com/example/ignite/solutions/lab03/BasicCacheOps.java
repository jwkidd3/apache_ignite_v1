package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Lab 3 Exercise 1: Basic Cache Operations
 *
 * This exercise demonstrates fundamental CRUD operations
 * on Ignite caches.
 */
public class BasicCacheOps {

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cache-ops-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Basic Cache Operations Lab ===\n");

            // Create a cache configuration
            CacheConfiguration<Integer, String> cacheCfg =
                new CacheConfiguration<>("myCache");

            // Get or create cache
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // PUT operation
            System.out.println("1. PUT Operations:");
            cache.put(1, "Hello");
            cache.put(2, "World");
            cache.put(3, "Apache Ignite");
            System.out.println("   Added 3 entries to cache");

            // GET operation
            System.out.println("\n2. GET Operations:");
            String value1 = cache.get(1);
            String value2 = cache.get(2);
            System.out.println("   Key 1: " + value1);
            System.out.println("   Key 2: " + value2);

            // GET non-existent key
            String value99 = cache.get(99);
            System.out.println("   Key 99: " + value99 + " (null expected)");

            // CONTAINS operation
            System.out.println("\n3. CONTAINS Operations:");
            System.out.println("   Contains key 1: " + cache.containsKey(1));
            System.out.println("   Contains key 99: " + cache.containsKey(99));

            // REPLACE operation
            System.out.println("\n4. REPLACE Operations:");
            boolean replaced = cache.replace(1, "Hello", "Hi");
            System.out.println("   Replaced 'Hello' with 'Hi': " + replaced);
            System.out.println("   New value: " + cache.get(1));

            // Try to replace with wrong old value
            replaced = cache.replace(1, "Hello", "Hey");
            System.out.println("   Tried wrong old value: " + replaced + " (false expected)");

            // REMOVE operation
            System.out.println("\n5. REMOVE Operations:");
            boolean removed = cache.remove(3);
            System.out.println("   Removed key 3: " + removed);
            System.out.println("   Key 3 exists: " + cache.containsKey(3));

            // Conditional remove
            cache.put(4, "Test");
            removed = cache.remove(4, "Test");
            System.out.println("   Conditional remove: " + removed);

            // GET AND PUT
            System.out.println("\n6. GET AND PUT Operations:");
            String oldValue = cache.getAndPut(1, "Greetings");
            System.out.println("   Old value: " + oldValue);
            System.out.println("   New value: " + cache.get(1));

            // GET AND REMOVE
            System.out.println("\n7. GET AND REMOVE Operations:");
            oldValue = cache.getAndRemove(1);
            System.out.println("   Removed value: " + oldValue);
            System.out.println("   Key exists: " + cache.containsKey(1));

            // PUT IF ABSENT
            System.out.println("\n8. PUT IF ABSENT Operations:");
            boolean putResult = cache.putIfAbsent(5, "New Value");
            System.out.println("   Put if absent (new key): " + putResult);
            putResult = cache.putIfAbsent(5, "Another Value");
            System.out.println("   Put if absent (existing key): " + putResult);

            // Cache size
            System.out.println("\n9. Cache Statistics:");
            System.out.println("   Cache size: " + cache.size());

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
