package com.example.ignite.solutions.lab11;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DataPageEvictionMode;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.List;

/**
 * Lab 11 Exercise 1: JVM Tuning for Ignite
 *
 * Demonstrates:
 * - JVM information and configuration
 * - Memory configuration for Ignite
 * - Data region settings
 * - Thread pool tuning
 */
public class Lab11JVMTuning {

    public static void main(String[] args) {
        System.out.println("=== JVM Tuning Lab ===\n");

        printJVMInfo();
        printGCInfo();

        IgniteConfiguration cfg = createOptimizedConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\nIgnite node started with optimized configuration\n");

            printMemoryInfo(ignite);

            System.out.println("=== JVM Tuning Recommendations ===");
            System.out.println("1. Heap Size:");
            System.out.println("   - Set -Xms equal to -Xmx");
            System.out.println("   - Use 60-70% of available RAM");
            System.out.println("   - Leave memory for off-heap and OS");

            System.out.println("\n2. Garbage Collector:");
            System.out.println("   - Use G1GC (recommended for Ignite)");
            System.out.println("   - Alternative: ZGC for low-latency (Java 15+)");
            System.out.println("   - Avoid CMS (deprecated)");

            System.out.println("\n3. GC Tuning:");
            System.out.println("   - MaxGCPauseMillis: 200-500ms");
            System.out.println("   - G1HeapRegionSize: 16-64MB");
            System.out.println("   - Monitor GC logs regularly");

            System.out.println("\n4. Off-Heap Memory:");
            System.out.println("   - Configure MaxDirectMemorySize");
            System.out.println("   - Should exceed data region sizes");
            System.out.println("   - Monitor with JConsole/VisualVM");

            System.out.println("\n=== Recommended JVM Options ===");
            System.out.println("-Xms4g -Xmx4g");
            System.out.println("-XX:+UseG1GC");
            System.out.println("-XX:G1HeapRegionSize=32m");
            System.out.println("-XX:MaxGCPauseMillis=200");
            System.out.println("-XX:InitiatingHeapOccupancyPercent=45");
            System.out.println("-XX:+ParallelRefProcEnabled");
            System.out.println("-XX:+AlwaysPreTouch");
            System.out.println("-XX:MaxDirectMemorySize=8g");
            System.out.println("-XX:+HeapDumpOnOutOfMemoryError");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printJVMInfo() {
        Runtime runtime = Runtime.getRuntime();

        System.out.println("=== JVM Information ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("JVM Name: " + System.getProperty("java.vm.name"));

        System.out.println("\n=== Memory Information ===");
        System.out.println("Max Heap: " + runtime.maxMemory() / (1024 * 1024) + " MB");
        System.out.println("Total Heap: " + runtime.totalMemory() / (1024 * 1024) + " MB");
        System.out.println("Free Heap: " + runtime.freeMemory() / (1024 * 1024) + " MB");
        System.out.println("Used Heap: " +
            (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + " MB");
        System.out.println("Processors: " + runtime.availableProcessors());
    }

    private static void printGCInfo() {
        System.out.println("\n=== Garbage Collector Information ===");

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("GC Name: " + gcBean.getName());
            System.out.println("  Collections: " + gcBean.getCollectionCount());
            System.out.println("  Collection Time: " + gcBean.getCollectionTime() + " ms");
            System.out.println("  Memory Pools: " + String.join(", ", gcBean.getMemoryPoolNames()));
        }

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        System.out.println("\nHeap Memory Usage: " +
            memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024) + " MB / " +
            memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024) + " MB");
        System.out.println("Non-Heap Memory Usage: " +
            memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024) + " MB");
    }

    private static IgniteConfiguration createOptimizedConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("optimized-node");

        // Data storage configuration
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Default data region (off-heap)
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("Default_Region");
        defaultRegion.setInitialSize(256L * 1024 * 1024);  // 256 MB initial
        defaultRegion.setMaxSize(1L * 1024 * 1024 * 1024); // 1 GB max
        defaultRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        defaultRegion.setEvictionThreshold(0.9);
        defaultRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);

        // WAL configuration for better performance (in-memory only for this demo)
        storageCfg.setWalMode(WALMode.NONE);

        cfg.setDataStorageConfiguration(storageCfg);

        // Thread pools
        int cpus = Runtime.getRuntime().availableProcessors();
        cfg.setPublicThreadPoolSize(cpus * 2);
        cfg.setSystemThreadPoolSize(cpus);
        cfg.setStripedPoolSize(cpus);

        return cfg;
    }

    private static void printMemoryInfo(Ignite ignite) {
        System.out.println("=== Ignite Memory Configuration ===");

        ignite.dataRegionMetrics().forEach(metrics -> {
            System.out.println("Region: " + metrics.getName());
            System.out.println("  Total Allocated: " +
                metrics.getTotalAllocatedSize() / (1024 * 1024) + " MB");
            System.out.println("  Physical Memory: " +
                metrics.getPhysicalMemorySize() / (1024 * 1024) + " MB");
            System.out.println("  Allocation Rate: " +
                String.format("%.2f", metrics.getAllocationRate()) + " pages/sec");
            System.out.println("  Eviction Rate: " +
                String.format("%.2f", metrics.getEvictionRate()) + " pages/sec");
        });
    }
}
