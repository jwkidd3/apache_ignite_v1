package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lab 3 Exercise 4: Batch Operations
 *
 * This exercise demonstrates efficient batch operations
 * using putAll, getAll, and removeAll.
 */
public class BatchOperations {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Batch Operations Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("batchCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Individual puts vs batch put
            System.out.println("1. Performance: Individual vs Batch Operations\n");

            // Individual puts
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                cache.put(i, "Value-" + i);
            }
            long individualTime = System.currentTimeMillis() - startTime;
            System.out.println("   Individual puts (1000): " + individualTime + " ms");

            cache.clear();

            // Batch put (putAll)
            startTime = System.currentTimeMillis();
            Map<Integer, String> batch = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                batch.put(i, "Value-" + i);
            }
            cache.putAll(batch);
            long batchTime = System.currentTimeMillis() - startTime;
            System.out.println("   Batch put (putAll): " + batchTime + " ms");
            if (batchTime > 0) {
                System.out.println("   Performance gain: " +
                    String.format("%.2f", (float)individualTime / batchTime) + "x faster");
            }

            // Batch get (getAll)
            System.out.println("\n2. Batch GET Operations:\n");

            Set<Integer> keys = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                keys.add(i);
            }

            Map<Integer, String> values = cache.getAll(keys);
            System.out.println("   Retrieved " + values.size() + " entries");
            System.out.println("   Sample: " + values.get(0));

            // Batch remove (removeAll)
            System.out.println("\n3. Batch REMOVE Operations:\n");

            Set<Integer> keysToRemove = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                keysToRemove.add(i);
            }

            cache.removeAll(keysToRemove);
            System.out.println("   Removed " + keysToRemove.size() + " entries");
            System.out.println("   Cache size now: " + cache.size());

            // Replace all
            System.out.println("\n4. Replace Operations:\n");

            Map<Integer, String> replacements = new HashMap<>();
            for (int i = 50; i < 100; i++) {
                replacements.put(i, "Updated-" + i);
            }

            cache.putAll(replacements);
            System.out.println("   Updated " + replacements.size() + " entries");

            System.out.println("\n=== Performance Best Practices ===");
            System.out.println("- Use putAll/getAll/removeAll for bulk operations");
            System.out.println("- Batch size: 500-1000 entries for optimal performance");
            System.out.println("- Reduce network round trips");
            System.out.println("- Use async operations for concurrent processing");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
