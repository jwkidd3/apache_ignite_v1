package com.example.ignite.solutions.lab11;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.configuration.CacheConfiguration;

/**
 * Lab 11 Exercise 2: Performance Monitoring
 *
 * Demonstrates:
 * - Cache metrics collection
 * - Cluster metrics monitoring
 * - Data region metrics
 * - Performance analysis
 */
public class Lab11Monitoring {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Performance Monitoring Lab ===\n");

            // Create cache with metrics enabled
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("monitoredCache");
            cfg.setStatisticsEnabled(true);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Generate some load
            System.out.println("Generating load...\n");
            for (int i = 0; i < 1000; i++) {
                cache.put(i, "Value-" + i);
            }

            for (int i = 0; i < 500; i++) {
                cache.get(i % 1000);
            }

            // Some cache misses
            for (int i = 1000; i < 1100; i++) {
                cache.get(i);  // These will miss
            }

            // Cache Metrics
            System.out.println("=== Cache Metrics ===");
            CacheMetrics cacheMetrics = cache.metrics();

            System.out.println("Cache Performance:");
            System.out.println("  Gets: " + cacheMetrics.getCacheGets());
            System.out.println("  Puts: " + cacheMetrics.getCachePuts());
            System.out.println("  Hits: " + cacheMetrics.getCacheHits());
            System.out.println("  Misses: " + cacheMetrics.getCacheMisses());
            System.out.println("  Hit Rate: " +
                String.format("%.2f%%", cacheMetrics.getCacheHitPercentage()));
            System.out.println("  Miss Rate: " +
                String.format("%.2f%%", cacheMetrics.getCacheMissPercentage()));

            System.out.println("\nCache Timing:");
            System.out.println("  Avg Get Time: " +
                String.format("%.3f ms", cacheMetrics.getAverageGetTime()));
            System.out.println("  Avg Put Time: " +
                String.format("%.3f ms", cacheMetrics.getAveragePutTime()));
            System.out.println("  Avg Remove Time: " +
                String.format("%.3f ms", cacheMetrics.getAverageRemoveTime()));

            System.out.println("\nCache Size:");
            System.out.println("  Entries: " + cache.size());
            System.out.println("  Heap Entries: " + cacheMetrics.getHeapEntriesCount());
            System.out.println("  Off-Heap Entries: " + cacheMetrics.getOffHeapEntriesCount());
            System.out.println("  Off-Heap Size: " +
                cacheMetrics.getOffHeapAllocatedSize() / 1024 + " KB");

            // Cluster Metrics
            System.out.println("\n=== Cluster Metrics ===");
            ClusterMetrics clusterMetrics = ignite.cluster().metrics();

            System.out.println("Cluster Performance:");
            System.out.println("  Total Nodes: " + ignite.cluster().nodes().size());
            System.out.println("  Total CPUs: " + clusterMetrics.getTotalCpus());
            System.out.println("  Current CPU Load: " +
                String.format("%.2f%%", clusterMetrics.getCurrentCpuLoad() * 100));
            System.out.println("  Average CPU Load: " +
                String.format("%.2f%%", clusterMetrics.getAverageCpuLoad() * 100));

            System.out.println("\nCluster Memory:");
            System.out.println("  Heap Memory Used: " +
                clusterMetrics.getHeapMemoryUsed() / (1024 * 1024) + " MB");
            System.out.println("  Heap Memory Max: " +
                clusterMetrics.getHeapMemoryMaximum() / (1024 * 1024) + " MB");
            System.out.println("  Non-Heap Memory Used: " +
                clusterMetrics.getNonHeapMemoryUsed() / (1024 * 1024) + " MB");

            System.out.println("\nCluster Activity:");
            System.out.println("  Current Active Jobs: " + clusterMetrics.getCurrentActiveJobs());
            System.out.println("  Total Executed Jobs: " + clusterMetrics.getTotalExecutedJobs());
            System.out.println("  Avg Job Wait Time: " +
                String.format("%.2f ms", clusterMetrics.getAverageJobWaitTime()));
            System.out.println("  Avg Job Execute Time: " +
                String.format("%.2f ms", clusterMetrics.getAverageJobExecuteTime()));

            System.out.println("\nNetwork Metrics:");
            System.out.println("  Sent Messages: " + clusterMetrics.getSentMessagesCount());
            System.out.println("  Received Messages: " + clusterMetrics.getReceivedMessagesCount());
            System.out.println("  Sent Bytes: " + clusterMetrics.getSentBytesCount() / 1024 + " KB");
            System.out.println("  Received Bytes: " + clusterMetrics.getReceivedBytesCount() / 1024 + " KB");

            // Data Region Metrics
            System.out.println("\n=== Data Region Metrics ===");
            ignite.dataRegionMetrics().forEach(metrics -> {
                System.out.println("Region: " + metrics.getName());
                System.out.println("  Total Allocated: " +
                    metrics.getTotalAllocatedSize() / (1024 * 1024) + " MB");
                System.out.println("  Physical Memory: " +
                    metrics.getPhysicalMemorySize() / (1024 * 1024) + " MB");
                System.out.println("  Pages Read: " + metrics.getPagesRead());
                System.out.println("  Pages Written: " + metrics.getPagesWritten());
                System.out.println("  Pages Replaced: " + metrics.getPagesReplaced());
                System.out.println("  Page Fill Factor: " +
                    String.format("%.2f%%", metrics.getPagesFillFactor() * 100));
            });

            System.out.println("\n=== Monitoring Tools ===");
            System.out.println("1. JConsole/VisualVM - JMX monitoring");
            System.out.println("2. Ignite Web Console - cluster management");
            System.out.println("3. Prometheus + Grafana - metrics collection");
            System.out.println("4. Custom JMX beans - application-specific metrics");

            System.out.println("\n=== Key Metrics to Watch ===");
            System.out.println("- Cache hit rate (target > 90%)");
            System.out.println("- GC pause times");
            System.out.println("- CPU utilization");
            System.out.println("- Memory usage (heap and off-heap)");
            System.out.println("- Network traffic");
            System.out.println("- Disk I/O (if persistence enabled)");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
