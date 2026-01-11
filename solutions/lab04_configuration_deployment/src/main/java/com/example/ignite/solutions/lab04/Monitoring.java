package com.example.ignite.solutions.lab04;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.configuration.CacheConfiguration;

/**
 * Lab 4 Exercise 5-6: Monitoring and Metrics
 *
 * This exercise demonstrates configuring logging and
 * accessing cache and cluster metrics.
 */
public class Monitoring {

    public static void main(String[] args) throws Exception {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Monitoring and Metrics Lab ===\n");

            // Create cache with metrics enabled
            CacheConfiguration<Integer, String> cacheCfg =
                new CacheConfiguration<>("monitoredCache");
            cacheCfg.setStatisticsEnabled(true);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Perform some operations
            System.out.println("Performing cache operations...");
            for (int i = 0; i < 100; i++) {
                cache.put(i, "Value-" + i);
            }

            for (int i = 0; i < 50; i++) {
                cache.get(i);
            }

            // Try to get non-existent keys (misses)
            for (int i = 100; i < 120; i++) {
                cache.get(i);
            }

            // Display cache metrics
            CacheMetrics metrics = cache.metrics();

            System.out.println("\n=== Cache Metrics ===");
            System.out.println("Cache gets: " + metrics.getCacheGets());
            System.out.println("Cache puts: " + metrics.getCachePuts());
            System.out.println("Cache hits: " + metrics.getCacheHits());
            System.out.println("Cache misses: " + metrics.getCacheMisses());
            System.out.println("Hit percentage: " +
                String.format("%.2f%%", metrics.getCacheHitPercentage()));
            System.out.println("Miss percentage: " +
                String.format("%.2f%%", metrics.getCacheMissPercentage()));
            System.out.println("Average get time: " +
                String.format("%.3f ms", metrics.getAverageGetTime()));
            System.out.println("Average put time: " +
                String.format("%.3f ms", metrics.getAveragePutTime()));

            // Cluster metrics
            System.out.println("\n=== Cluster Metrics ===");
            System.out.println("Total CPUs: " +
                ignite.cluster().metrics().getTotalCpus());
            System.out.println("Current CPU load: " +
                String.format("%.2f%%",
                    ignite.cluster().metrics().getCurrentCpuLoad() * 100));
            System.out.println("Heap memory used: " +
                ignite.cluster().metrics().getHeapMemoryUsed() / (1024 * 1024) + " MB");
            System.out.println("Heap memory max: " +
                ignite.cluster().metrics().getHeapMemoryMaximum() / (1024 * 1024) + " MB");

            // Data region metrics
            System.out.println("\n=== Data Region Metrics ===");
            ignite.dataRegionMetrics().forEach(drm -> {
                System.out.println("Region: " + drm.getName());
                System.out.println("  Total allocated pages: " + drm.getTotalAllocatedPages());
                System.out.println("  Allocation rate: " +
                    String.format("%.2f", drm.getAllocationRate()) + " pages/sec");
                System.out.println("  Pages fill factor: " +
                    String.format("%.2f%%", drm.getPagesFillFactor() * 100));
            });

            // Additional cache statistics
            System.out.println("\n=== Additional Cache Statistics ===");
            System.out.println("Cache size: " + cache.size());
            System.out.println("Cache removes: " + metrics.getCacheRemovals());
            System.out.println("Cache evictions: " + metrics.getCacheEvictions());

            System.out.println("\n=== Monitoring Best Practices ===");
            System.out.println("- Enable statistics on caches that need monitoring");
            System.out.println("- Monitor hit ratio to measure cache effectiveness");
            System.out.println("- Track average get/put times for performance");
            System.out.println("- Watch heap memory usage for capacity planning");
            System.out.println("- Note: Statistics collection has a small overhead");

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }
}
