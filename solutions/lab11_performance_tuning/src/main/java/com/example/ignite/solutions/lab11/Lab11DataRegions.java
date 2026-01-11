package com.example.ignite.solutions.lab11;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataPageEvictionMode;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lab 11 Exercise: Data Regions and Memory Management
 *
 * Demonstrates:
 * - Multiple data region configuration
 * - Memory management with different eviction policies
 * - Monitoring memory usage across regions
 * - Memory pressure handling
 */
public class Lab11DataRegions {

    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("=== Data Regions and Memory Management Lab ===\n");

        IgniteConfiguration cfg = createMemoryOptimizedConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started with custom memory configuration\n");

            // Display initial memory state
            displayMemoryRegions(ignite);

            // Create caches in different regions
            IgniteCache<Integer, String> hotCache = createCacheInRegion(
                ignite, "hotDataCache", "hot-data-region");
            IgniteCache<Integer, byte[]> warmCache = createCacheInRegion(
                ignite, "warmDataCache", "warm-data-region");
            IgniteCache<Integer, String> coldCache = createCacheInRegion(
                ignite, "coldDataCache", "cold-data-region");

            // Start memory monitoring
            ScheduledExecutorService monitor = startMemoryMonitor(ignite);

            // Simulate workload on different regions
            System.out.println("\n=== Simulating Workload ===");

            // Hot data - frequently accessed, small entries
            System.out.println("\nPopulating hot data region (small, frequent access)...");
            for (int i = 0; i < 10000; i++) {
                hotCache.put(i, "HotValue-" + i);
            }
            System.out.println("Hot data region populated with 10,000 entries");

            // Warm data - larger entries, less frequent
            System.out.println("\nPopulating warm data region (larger entries)...");
            byte[] largeValue = new byte[4096]; // 4KB entries
            for (int i = 0; i < 5000; i++) {
                warmCache.put(i, largeValue);
                if (i % 1000 == 0 && i > 0) {
                    System.out.println("  Inserted " + i + " entries to warm region...");
                }
            }
            System.out.println("Warm data region populated with 5,000 entries");

            // Cold data - infrequently accessed
            System.out.println("\nPopulating cold data region...");
            for (int i = 0; i < 1000; i++) {
                coldCache.put(i, "ColdValue-" + i);
            }
            System.out.println("Cold data region populated with 1,000 entries");

            // Display memory state after population
            System.out.println("\n=== Memory State After Population ===");
            displayMemoryRegions(ignite);

            // Demonstrate eviction by filling up warm region
            System.out.println("\n=== Triggering Eviction in Warm Region ===");
            System.out.println("Adding more data to trigger eviction...\n");

            for (int i = 5000; i < 15000; i++) {
                warmCache.put(i, largeValue);
                if (i % 2000 == 0) {
                    displayRegionMetrics(ignite, "warm-data-region");
                }
            }

            // Final memory state
            System.out.println("\n=== Final Memory State ===");
            displayMemoryRegions(ignite);

            // Display configuration recommendations
            displayConfigurationTips();

            // Cleanup
            running = false;
            monitor.shutdown();

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createMemoryOptimizedConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("data-regions-node");

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // System region (for internal use)
        storageCfg.setSystemRegionInitialSize(50L * 1024 * 1024);   // 50 MB
        storageCfg.setSystemRegionMaxSize(100L * 1024 * 1024);      // 100 MB

        // =================================================================
        // Region 1: Hot Data Region
        // For frequently accessed, small objects
        // =================================================================
        DataRegionConfiguration hotRegion = new DataRegionConfiguration();
        hotRegion.setName("hot-data-region");
        hotRegion.setInitialSize(64L * 1024 * 1024);    // 64 MB initial
        hotRegion.setMaxSize(256L * 1024 * 1024);       // 256 MB max
        hotRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_LRU);
        hotRegion.setEvictionThreshold(0.9);            // Evict when 90% full
        hotRegion.setEmptyPagesPoolSize(100);           // Pool for empty pages
        hotRegion.setMetricsEnabled(true);
        hotRegion.setMetricsSubIntervalCount(10);
        hotRegion.setMetricsRateTimeInterval(60000);    // 1 minute

        // =================================================================
        // Region 2: Warm Data Region
        // For larger objects, with aggressive eviction
        // =================================================================
        DataRegionConfiguration warmRegion = new DataRegionConfiguration();
        warmRegion.setName("warm-data-region");
        warmRegion.setInitialSize(64L * 1024 * 1024);   // 64 MB initial
        warmRegion.setMaxSize(128L * 1024 * 1024);      // 128 MB max (small to demo eviction)
        warmRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        warmRegion.setEvictionThreshold(0.8);           // Start evicting at 80%
        warmRegion.setMetricsEnabled(true);

        // =================================================================
        // Region 3: Cold Data Region
        // For infrequently accessed data
        // =================================================================
        DataRegionConfiguration coldRegion = new DataRegionConfiguration();
        coldRegion.setName("cold-data-region");
        coldRegion.setInitialSize(32L * 1024 * 1024);   // 32 MB initial
        coldRegion.setMaxSize(64L * 1024 * 1024);       // 64 MB max
        coldRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_LRU);
        coldRegion.setEvictionThreshold(0.9);
        coldRegion.setMetricsEnabled(true);

        // Set default region
        storageCfg.setDefaultDataRegionConfiguration(hotRegion);

        // Add additional regions
        storageCfg.setDataRegionConfigurations(warmRegion, coldRegion);

        cfg.setDataStorageConfiguration(storageCfg);

        return cfg;
    }

    private static <K, V> IgniteCache<K, V> createCacheInRegion(
            Ignite ignite, String cacheName, String regionName) {

        CacheConfiguration<K, V> cacheCfg = new CacheConfiguration<>(cacheName);
        cacheCfg.setDataRegionName(regionName);
        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg.setBackups(0);
        cacheCfg.setStatisticsEnabled(true);

        return ignite.getOrCreateCache(cacheCfg);
    }

    private static void displayMemoryRegions(Ignite ignite) {
        System.out.println("=== Data Region Metrics ===");

        ignite.dataRegionMetrics().forEach(metrics -> {
            System.out.println("\nRegion: " + metrics.getName());
            System.out.println("  Total Allocated: " + formatBytes(metrics.getTotalAllocatedSize()));
            System.out.println("  Physical Memory: " + formatBytes(metrics.getPhysicalMemorySize()));
            System.out.println("  Fill Factor: " +
                String.format("%.2f%%", metrics.getPagesFillFactor() * 100));
            System.out.println("  Pages Filled: " + metrics.getTotalAllocatedPages());
            System.out.println("  Allocation Rate: " +
                String.format("%.2f", metrics.getAllocationRate()) + " pages/sec");
            System.out.println("  Eviction Rate: " +
                String.format("%.2f", metrics.getEvictionRate()) + " pages/sec");
        });
    }

    private static void displayRegionMetrics(Ignite ignite, String regionName) {
        ignite.dataRegionMetrics().stream()
            .filter(m -> m.getName().equals(regionName))
            .findFirst()
            .ifPresent(metrics -> {
                System.out.println("  [" + regionName + "] " +
                    "Allocated: " + formatBytes(metrics.getTotalAllocatedSize()) +
                    " | Physical: " + formatBytes(metrics.getPhysicalMemorySize()) +
                    " | Fill: " + String.format("%.1f%%", metrics.getPagesFillFactor() * 100) +
                    " | Eviction Rate: " + String.format("%.2f", metrics.getEvictionRate()) + " pages/sec");
            });
    }

    private static ScheduledExecutorService startMemoryMonitor(Ignite ignite) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            if (!running) return;

            Runtime runtime = Runtime.getRuntime();
            long usedHeap = runtime.totalMemory() - runtime.freeMemory();
            long maxHeap = runtime.maxMemory();

            System.out.println("\n[Memory Monitor] Heap: " +
                formatBytes(usedHeap) + " / " + formatBytes(maxHeap) +
                " (" + String.format("%.1f%%", (double) usedHeap / maxHeap * 100) + ")");

        }, 5, 10, TimeUnit.SECONDS);

        return executor;
    }

    private static void displayConfigurationTips() {
        System.out.println("\n=== Data Region Configuration Best Practices ===");

        System.out.println("\n1. REGION SIZING:");
        System.out.println("   - Size regions based on expected data volume");
        System.out.println("   - Leave headroom for growth (20-30%)");
        System.out.println("   - Monitor usage patterns over time");

        System.out.println("\n2. EVICTION POLICIES:");
        System.out.println("   - RANDOM_LRU: General use, low overhead");
        System.out.println("   - RANDOM_2_LRU: Better accuracy, slightly more overhead");
        System.out.println("   - Set evictionThreshold based on workload (0.7-0.9)");

        System.out.println("\n3. REGION SEPARATION:");
        System.out.println("   - Hot data: Small region, fast access, higher eviction threshold");
        System.out.println("   - Warm data: Medium region, moderate eviction");
        System.out.println("   - Cold data: Can use persistence, lower memory footprint");

        System.out.println("\n4. MONITORING:");
        System.out.println("   - Enable metrics on all regions");
        System.out.println("   - Track fill factors and eviction rates");
        System.out.println("   - Set up alerts for high memory usage");

        System.out.println("\n5. MEMORY ALLOCATION:");
        System.out.println("   - Total region sizes should not exceed MaxDirectMemorySize");
        System.out.println("   - Leave memory for JVM heap and OS");
        System.out.println("   - Example: 32GB RAM -> 8GB heap, 20GB off-heap, 4GB OS");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
