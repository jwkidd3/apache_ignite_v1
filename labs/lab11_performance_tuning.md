# Lab 11: Performance Tuning and Monitoring

## Duration: 55 minutes

## Objectives
- Tune JVM for Ignite workloads
- Monitor performance metrics
- Benchmark and load test strategies

## Prerequisites
- Completed Labs 1-10
- Understanding of JVM basics
- Knowledge of performance concepts

## Part 1: JVM Tuning (15 minutes)

### Exercise 1: JVM Configuration for Ignite

Create `ignite-jvm.sh` script:

```bash
#!/bin/bash

# JVM Options for Ignite

# Heap size (adjust based on available RAM)
# Rule of thumb: 60-70% of available RAM for Ignite
HEAP_SIZE="-Xms4g -Xmx4g"

# Garbage Collector Settings
# G1GC is recommended for Ignite
GC_SETTINGS="-XX:+UseG1GC \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxGCPauseMillis=200 \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:G1HeapRegionSize=32m"

# GC Logging (Java 8)
GC_LOG_JAVA8="-Xloggc:gc.log \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -XX:+PrintGCTimeStamps \
  -XX:+UseGCLogFileRotation \
  -XX:NumberOfGCLogFiles=10 \
  -XX:GCLogFileSize=100M"

# GC Logging (Java 11+)
GC_LOG_JAVA11="-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=10,filesize=100M"

# Performance Settings
PERFORMANCE="-XX:+AlwaysPreTouch \
  -XX:+UseStringDeduplication \
  -XX:+DisableExplicitGC"

# Off-heap memory (for data regions)
OFF_HEAP="-XX:MaxDirectMemorySize=8g"

# Diagnostic options
DIAGNOSTIC="-XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=./heapdump.hprof"

# Export all JVM options
export JVM_OPTS="$HEAP_SIZE $GC_SETTINGS $GC_LOG_JAVA11 $PERFORMANCE $OFF_HEAP $DIAGNOSTIC"

echo "JVM Options set:"
echo "$JVM_OPTS"
```

Create `Lab11JVMTuning.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

public class Lab11JVMTuning {

    public static void main(String[] args) {
        System.out.println("=== JVM Tuning Lab ===\n");

        printJVMInfo();

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
        System.out.println("Processors: " + runtime.availableProcessors());
    }

    private static IgniteConfiguration createOptimizedConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("optimized-node");

        // Data storage configuration
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Default data region (off-heap)
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("Default_Region");
        defaultRegion.setInitialSize(512L * 1024 * 1024);  // 512 MB
        defaultRegion.setMaxSize(2L * 1024 * 1024 * 1024); // 2 GB
        defaultRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        defaultRegion.setEvictionThreshold(0.9);
        defaultRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);

        // WAL configuration for better performance
        storageCfg.setWalMode(WALMode.LOG_ONLY);
        storageCfg.setWalSegmentSize(64 * 1024 * 1024);  // 64 MB

        // Checkpointing
        storageCfg.setCheckpointFrequency(180000);  // 3 minutes

        cfg.setDataStorageConfiguration(storageCfg);

        // Thread pools
        cfg.setPublicThreadPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        cfg.setSystemThreadPoolSize(Runtime.getRuntime().availableProcessors());

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
        });
    }
}
```

## Part 2: Performance Metrics and Monitoring (15 minutes)

### Exercise 2: Comprehensive Monitoring

Create `Lab11Monitoring.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.configuration.CacheConfiguration;

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

            // Data Region Metrics
            System.out.println("\n=== Data Region Metrics ===");
            ignite.dataRegionMetrics().forEach(metrics -> {
                System.out.println("Region: " + metrics.getName());
                System.out.println("  Total Allocated: " +
                    metrics.getTotalAllocatedSize() / (1024 * 1024) + " MB");
                System.out.println("  Physical Memory: " +
                    metrics.getPhysicalMemorySize() / (1024 * 1024) + " MB");
                System.out.println("  Checkpoint Buffer: " +
                    metrics.getCheckpointBufferSize() / (1024 * 1024) + " MB");
                System.out.println("  Pages Read: " + metrics.getPagesRead());
                System.out.println("  Pages Written: " + metrics.getPagesWritten());
                System.out.println("  Pages Replaced: " + metrics.getPagesReplaced());
            });

            System.out.println("\n=== Monitoring Tools ===");
            System.out.println("1. JConsole/VisualVM - JMX monitoring");
            System.out.println("2. Ignite Web Console - cluster management");
            System.out.println("3. Prometheus + Grafana - metrics collection");
            System.out.println("4. Custom JMX beans - application-specific metrics");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 3: Benchmarking and Load Testing (15 minutes)

### Exercise 3: Performance Benchmarking

Create `Lab11Benchmark.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Lab11Benchmark {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;
    private static final int THREAD_COUNT = 10;

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Performance Benchmark Lab ===\n");

            // Test different cache configurations
            benchmarkAtomicCache(ignite);
            benchmarkTransactionalCache(ignite);
            benchmarkBatchOperations(ignite);
            benchmarkConcurrentAccess(ignite);

            System.out.println("\n=== Benchmark Summary ===");
            System.out.println("All benchmarks completed");
            System.out.println("Review results to identify bottlenecks");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void benchmarkAtomicCache(Ignite ignite) {
        System.out.println("=== Benchmark 1: Atomic Cache ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("atomicCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            cache.put(i, "Warmup-" + i);
        }

        // Benchmark PUT
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            cache.put(i, "Value-" + i);
        }
        long putTime = System.currentTimeMillis() - startTime;

        // Benchmark GET
        startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            cache.get(i);
        }
        long getTime = System.currentTimeMillis() - startTime;

        System.out.println("PUT: " + TEST_ITERATIONS + " operations in " + putTime + " ms");
        System.out.println("Throughput: " + (TEST_ITERATIONS * 1000 / putTime) + " ops/sec");
        System.out.println("GET: " + TEST_ITERATIONS + " operations in " + getTime + " ms");
        System.out.println("Throughput: " + (TEST_ITERATIONS * 1000 / getTime) + " ops/sec\n");

        ignite.destroyCache("atomicCache");
    }

    private static void benchmarkTransactionalCache(Ignite ignite) {
        System.out.println("=== Benchmark 2: Transactional Cache ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("txCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Benchmark with transactions
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            try (org.apache.ignite.transactions.Transaction tx =
                     ignite.transactions().txStart()) {
                cache.put(i, "Value-" + i);
                tx.commit();
            }
        }
        long txTime = System.currentTimeMillis() - startTime;

        System.out.println("Transactional PUT: " + TEST_ITERATIONS +
            " operations in " + txTime + " ms");
        System.out.println("Throughput: " + (TEST_ITERATIONS * 1000 / txTime) + " ops/sec\n");

        ignite.destroyCache("txCache");
    }

    private static void benchmarkBatchOperations(Ignite ignite) {
        System.out.println("=== Benchmark 3: Batch Operations ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("batchCache");

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Individual operations
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            cache.put(i, "Value-" + i);
        }
        long individualTime = System.currentTimeMillis() - startTime;

        cache.clear();

        // Batch operations
        startTime = System.currentTimeMillis();
        Map<Integer, String> batch = new HashMap<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            batch.put(i, "Value-" + i);
            if (batch.size() >= 500) {
                cache.putAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            cache.putAll(batch);
        }
        long batchTime = System.currentTimeMillis() - startTime;

        System.out.println("Individual PUT: " + individualTime + " ms");
        System.out.println("Batch PUT: " + batchTime + " ms");
        System.out.println("Speedup: " + (float)individualTime / batchTime + "x\n");

        ignite.destroyCache("batchCache");
    }

    private static void benchmarkConcurrentAccess(Ignite ignite) {
        System.out.println("=== Benchmark 4: Concurrent Access ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("concurrentCache");

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Populate cache
        for (int i = 0; i < 10000; i++) {
            cache.put(i, "Value-" + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicLong operations = new AtomicLong(0);
        Random random = new Random();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                for (int j = 0; j < TEST_ITERATIONS / THREAD_COUNT; j++) {
                    int key = random.nextInt(10000);

                    // Mix of operations
                    if (j % 3 == 0) {
                        cache.put(key, "Updated-" + key);
                    } else {
                        cache.get(key);
                    }

                    operations.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long concurrentTime = System.currentTimeMillis() - startTime;

        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println("Total operations: " + operations.get());
        System.out.println("Time: " + concurrentTime + " ms");
        System.out.println("Throughput: " + (operations.get() * 1000 / concurrentTime) + " ops/sec\n");

        ignite.destroyCache("concurrentCache");
    }
}
```

### Exercise 4: Common Anti-Patterns

Create `Lab11AntiPatterns.java`:

```java
package com.example.ignite;

public class Lab11AntiPatterns {

    public static void main(String[] args) {
        System.out.println("=== Performance Anti-Patterns ===\n");

        System.out.println("1. Small Heap with Large Off-Heap");
        System.out.println("   ❌ -Xmx1g with 10GB off-heap");
        System.out.println("   ✅ -Xmx4g with 10GB off-heap");
        System.out.println("   Reason: GC overhead, metadata needs heap space\n");

        System.out.println("2. Too Many Backups");
        System.out.println("   ❌ setBackups(3) in 4-node cluster");
        System.out.println("   ✅ setBackups(1) or setBackups(2)");
        System.out.println("   Reason: Excessive memory usage, network overhead\n");

        System.out.println("3. Large Transactions");
        System.out.println("   ❌ Updating 10,000 entries in one transaction");
        System.out.println("   ✅ Batch updates in smaller transactions");
        System.out.println("   Reason: Lock contention, memory pressure\n");

        System.out.println("4. Synchronous Operations in Loops");
        System.out.println("   ❌ for (key : keys) cache.put(key, value)");
        System.out.println("   ✅ cache.putAll(map)");
        System.out.println("   Reason: Network round trips\n");

        System.out.println("5. No Affinity Keys");
        System.out.println("   ❌ Related data on different nodes");
        System.out.println("   ✅ Use @AffinityKeyMapped");
        System.out.println("   Reason: Distributed joins are expensive\n");

        System.out.println("6. Missing Indexes");
        System.out.println("   ❌ SELECT * FROM Table WHERE name = 'John'");
        System.out.println("   ✅ CREATE INDEX ON Table(name)");
        System.out.println("   Reason: Full scan instead of index lookup\n");

        System.out.println("7. Overusing Distributed Joins");
        System.out.println("   ❌ setDistributedJoins(true) everywhere");
        System.out.println("   ✅ Design for colocation");
        System.out.println("   Reason: Network overhead\n");

        System.out.println("8. Not Monitoring GC");
        System.out.println("   ❌ No GC logging");
        System.out.println("   ✅ Enable GC logs and monitor");
        System.out.println("   Reason: Hidden performance issues\n");

        System.out.println("9. Default Thread Pool Sizes");
        System.out.println("   ❌ Using default pools for high load");
        System.out.println("   ✅ Tune based on workload");
        System.out.println("   Reason: Thread starvation\n");

        System.out.println("10. Ignoring Metrics");
        System.out.println("   ❌ Not enabling cache statistics");
        System.out.println("   ✅ Monitor hit ratios, latencies");
        System.out.println("   Reason: Can't optimize what you don't measure");
    }
}
```

---

## Verification Steps

### Checklist
- [ ] JVM tuning script created with Ignite-optimized settings
- [ ] Performance metrics collection demonstrated
- [ ] Benchmarking tests executed
- [ ] Common performance anti-patterns identified

### Common Issues

**Issue: Out of memory errors**
- Increase heap size or off-heap memory
- Check for memory leaks in cache entries

**Issue: High GC pause times**
- Use G1GC with appropriate settings
- Consider ZGC for large heaps

## Lab Questions

1. Why is G1GC recommended for Ignite?
2. What is the difference between on-heap and off-heap storage?
3. How do you identify performance bottlenecks?

## Answers

1. **G1GC** provides predictable pause times and handles large heaps efficiently, important for Ignite's memory-intensive workloads.

2. **On-heap** is managed by JVM GC (faster access, GC overhead). **Off-heap** is managed by Ignite (avoids GC, requires serialization).

3. Use metrics (hit ratios, latencies), profiling tools, and benchmarks to identify bottlenecks in memory, CPU, network, or disk.

## Next Steps

In Lab 12, you will:
- Deploy to production environments
- Configure high availability
- Implement security features

## Completion

You have completed Lab 11 when you can:
- Configure JVM for Ignite workloads
- Collect and analyze performance metrics
- Run basic benchmarks

---

## Optional Exercises (If Time Permits)

### Optional: Advanced JVM Tuning

Create `Lab11AdvancedJVMTuning.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * Advanced JVM tuning demonstration for Apache Ignite.
 *
 * Recommended JVM flags for running this example:
 *
 * java -Xms4g -Xmx4g \
 *      -XX:+UseG1GC \
 *      -XX:G1HeapRegionSize=32m \
 *      -XX:MaxGCPauseMillis=200 \
 *      -XX:InitiatingHeapOccupancyPercent=45 \
 *      -XX:G1ReservePercent=15 \
 *      -XX:+ParallelRefProcEnabled \
 *      -XX:+AlwaysPreTouch \
 *      -XX:+UseStringDeduplication \
 *      -XX:MaxDirectMemorySize=8g \
 *      -XX:+HeapDumpOnOutOfMemoryError \
 *      -XX:HeapDumpPath=./heapdump.hprof \
 *      -Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=10,filesize=100M \
 *      Lab11AdvancedJVMTuning
 */
public class Lab11AdvancedJVMTuning {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Advanced JVM Tuning for Ignite ===\n");

        // Display current JVM configuration
        displayJVMConfiguration();

        // Display garbage collector information
        displayGCInfo();

        // Display memory pool information
        displayMemoryPools();

        // Start Ignite with optimized configuration
        IgniteConfiguration cfg = createOptimizedIgniteConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Ignite Started with Optimized JVM Settings ===");

            // Create test cache and generate load
            CacheConfiguration<Integer, byte[]> cacheCfg = new CacheConfiguration<>("jvmTestCache");
            cacheCfg.setStatisticsEnabled(true);

            IgniteCache<Integer, byte[]> cache = ignite.getOrCreateCache(cacheCfg);

            // Allocate objects to trigger GC
            System.out.println("\nGenerating load to observe GC behavior...");
            byte[] data = new byte[1024]; // 1KB per entry
            for (int i = 0; i < 100000; i++) {
                cache.put(i, data);
                if (i % 10000 == 0) {
                    System.out.println("  Inserted " + i + " entries...");
                    displayCurrentMemoryState();
                }
            }

            // Display final GC statistics
            System.out.println("\n=== Final GC Statistics ===");
            displayGCStatistics();

            // JVM tuning recommendations
            displayTuningRecommendations();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }

    private static void displayJVMConfiguration() {
        System.out.println("=== Current JVM Configuration ===");
        Runtime runtime = Runtime.getRuntime();

        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("JVM Name: " + System.getProperty("java.vm.name"));
        System.out.println("Available Processors: " + runtime.availableProcessors());
        System.out.println("Max Heap Size: " + formatBytes(runtime.maxMemory()));
        System.out.println("Total Heap Size: " + formatBytes(runtime.totalMemory()));
        System.out.println("Free Heap Size: " + formatBytes(runtime.freeMemory()));
        System.out.println("Used Heap Size: " + formatBytes(runtime.totalMemory() - runtime.freeMemory()));

        // Display JVM input arguments
        System.out.println("\nJVM Arguments:");
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            System.out.println("  " + arg);
        }
    }

    private static void displayGCInfo() {
        System.out.println("\n=== Garbage Collector Information ===");
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("GC Name: " + gcBean.getName());
            System.out.println("  Memory Pools: " + String.join(", ", gcBean.getMemoryPoolNames()));
            System.out.println("  Collection Count: " + gcBean.getCollectionCount());
            System.out.println("  Collection Time: " + gcBean.getCollectionTime() + " ms");
        }
    }

    private static void displayMemoryPools() {
        System.out.println("\n=== Memory Pool Information ===");
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean pool : pools) {
            MemoryUsage usage = pool.getUsage();
            if (usage != null) {
                System.out.println("Pool: " + pool.getName() + " (" + pool.getType() + ")");
                System.out.println("  Init: " + formatBytes(usage.getInit()));
                System.out.println("  Used: " + formatBytes(usage.getUsed()));
                System.out.println("  Committed: " + formatBytes(usage.getCommitted()));
                System.out.println("  Max: " + formatBytes(usage.getMax()));
            }
        }
    }

    private static void displayCurrentMemoryState() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        System.out.println("    Heap: " + formatBytes(heapUsage.getUsed()) + " / " +
            formatBytes(heapUsage.getMax()) + " (" +
            String.format("%.1f%%", (double) heapUsage.getUsed() / heapUsage.getMax() * 100) + ")");
        System.out.println("    Non-Heap: " + formatBytes(nonHeapUsage.getUsed()));
    }

    private static void displayGCStatistics() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long totalCollections = 0;
        long totalTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("Collector: " + gcBean.getName());
            System.out.println("  Collections: " + gcBean.getCollectionCount());
            System.out.println("  Total Time: " + gcBean.getCollectionTime() + " ms");

            if (gcBean.getCollectionCount() > 0) {
                System.out.println("  Avg Time/Collection: " +
                    String.format("%.2f", (double) gcBean.getCollectionTime() / gcBean.getCollectionCount()) + " ms");
            }

            totalCollections += gcBean.getCollectionCount();
            totalTime += gcBean.getCollectionTime();
        }

        System.out.println("\nTotal GC Overhead:");
        System.out.println("  Total Collections: " + totalCollections);
        System.out.println("  Total GC Time: " + totalTime + " ms");
    }

    private static IgniteConfiguration createOptimizedIgniteConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("jvm-tuned-node");

        // Optimize thread pools based on available processors
        int cpus = Runtime.getRuntime().availableProcessors();
        cfg.setPublicThreadPoolSize(cpus * 2);
        cfg.setSystemThreadPoolSize(cpus);
        cfg.setStripedPoolSize(cpus);
        cfg.setServiceThreadPoolSize(cpus);

        // Configure data storage with optimal settings
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("default");
        defaultRegion.setInitialSize(256L * 1024 * 1024);  // 256 MB
        defaultRegion.setMaxSize(1024L * 1024 * 1024);     // 1 GB
        defaultRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        cfg.setDataStorageConfiguration(storageCfg);

        return cfg;
    }

    private static void displayTuningRecommendations() {
        System.out.println("\n=== JVM Tuning Recommendations for Ignite ===");

        System.out.println("\n1. HEAP SIZING:");
        System.out.println("   - Set -Xms equal to -Xmx to avoid heap resizing");
        System.out.println("   - Allocate 60-70% of available RAM to heap");
        System.out.println("   - Reserve remaining for off-heap memory and OS");
        System.out.println("   Example: 32GB RAM -> -Xms20g -Xmx20g");

        System.out.println("\n2. G1GC CONFIGURATION:");
        System.out.println("   -XX:+UseG1GC                         # Enable G1GC");
        System.out.println("   -XX:G1HeapRegionSize=32m              # Region size (16-64MB)");
        System.out.println("   -XX:MaxGCPauseMillis=200              # Target pause time");
        System.out.println("   -XX:InitiatingHeapOccupancyPercent=45 # Start concurrent GC");
        System.out.println("   -XX:G1ReservePercent=15               # Reserve for promotion");

        System.out.println("\n3. OFF-HEAP MEMORY:");
        System.out.println("   -XX:MaxDirectMemorySize=8g            # For Ignite data regions");
        System.out.println("   Note: Should exceed total data region sizes");

        System.out.println("\n4. PERFORMANCE FLAGS:");
        System.out.println("   -XX:+AlwaysPreTouch                   # Pre-touch heap pages");
        System.out.println("   -XX:+UseStringDeduplication           # Reduce string memory");
        System.out.println("   -XX:+ParallelRefProcEnabled           # Parallel reference processing");
        System.out.println("   -XX:+DisableExplicitGC                # Ignore System.gc() calls");

        System.out.println("\n5. DIAGNOSTICS:");
        System.out.println("   -XX:+HeapDumpOnOutOfMemoryError");
        System.out.println("   -XX:HeapDumpPath=./heapdump.hprof");
        System.out.println("   -Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=10,filesize=100M");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
```

### JVM Flags Reference Script

Create `ignite-jvm-optimized.sh`:

```bash
#!/bin/bash

# =============================================================================
# Optimized JVM Configuration Script for Apache Ignite
# =============================================================================

# Detect available memory
TOTAL_MEM_KB=$(grep MemTotal /proc/meminfo 2>/dev/null | awk '{print $2}')
if [ -z "$TOTAL_MEM_KB" ]; then
    # macOS fallback
    TOTAL_MEM_KB=$(($(sysctl -n hw.memsize 2>/dev/null) / 1024))
fi

# Calculate heap size (60% of total memory)
HEAP_SIZE_KB=$((TOTAL_MEM_KB * 60 / 100))
HEAP_SIZE_GB=$((HEAP_SIZE_KB / 1024 / 1024))

# Calculate off-heap size (25% of total memory)
OFF_HEAP_KB=$((TOTAL_MEM_KB * 25 / 100))
OFF_HEAP_GB=$((OFF_HEAP_KB / 1024 / 1024))

# Minimum values
[ $HEAP_SIZE_GB -lt 2 ] && HEAP_SIZE_GB=2
[ $OFF_HEAP_GB -lt 1 ] && OFF_HEAP_GB=1

echo "Detected total memory: $((TOTAL_MEM_KB / 1024)) MB"
echo "Calculated heap size: ${HEAP_SIZE_GB}g"
echo "Calculated off-heap size: ${OFF_HEAP_GB}g"

# =============================================================================
# Heap Configuration
# =============================================================================
HEAP_OPTS="-Xms${HEAP_SIZE_GB}g -Xmx${HEAP_SIZE_GB}g"

# =============================================================================
# G1GC Configuration (Recommended for Ignite)
# =============================================================================
GC_OPTS="-XX:+UseG1GC"
GC_OPTS="$GC_OPTS -XX:G1HeapRegionSize=32m"
GC_OPTS="$GC_OPTS -XX:MaxGCPauseMillis=200"
GC_OPTS="$GC_OPTS -XX:InitiatingHeapOccupancyPercent=45"
GC_OPTS="$GC_OPTS -XX:G1ReservePercent=15"
GC_OPTS="$GC_OPTS -XX:+ParallelRefProcEnabled"
GC_OPTS="$GC_OPTS -XX:ParallelGCThreads=$(($(nproc) / 2))"
GC_OPTS="$GC_OPTS -XX:ConcGCThreads=$(($(nproc) / 4))"

# =============================================================================
# Off-Heap Memory Configuration
# =============================================================================
OFF_HEAP_OPTS="-XX:MaxDirectMemorySize=${OFF_HEAP_GB}g"

# =============================================================================
# Performance Optimizations
# =============================================================================
PERF_OPTS="-XX:+AlwaysPreTouch"
PERF_OPTS="$PERF_OPTS -XX:+UseStringDeduplication"
PERF_OPTS="$PERF_OPTS -XX:+DisableExplicitGC"
PERF_OPTS="$PERF_OPTS -XX:+UseNUMA"
PERF_OPTS="$PERF_OPTS -XX:+PerfDisableSharedMem"

# =============================================================================
# GC Logging (Java 11+)
# =============================================================================
GC_LOG_OPTS="-Xlog:gc*,gc+heap=debug,gc+phases=debug:file=gc.log:time,uptime,level,tags:filecount=10,filesize=100M"

# =============================================================================
# Diagnostic Options
# =============================================================================
DIAG_OPTS="-XX:+HeapDumpOnOutOfMemoryError"
DIAG_OPTS="$DIAG_OPTS -XX:HeapDumpPath=./heapdump.hprof"
DIAG_OPTS="$DIAG_OPTS -XX:OnOutOfMemoryError='kill -9 %p'"

# =============================================================================
# JMX Configuration (for monitoring)
# =============================================================================
JMX_OPTS="-Dcom.sun.management.jmxremote"
JMX_OPTS="$JMX_OPTS -Dcom.sun.management.jmxremote.port=49112"
JMX_OPTS="$JMX_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
JMX_OPTS="$JMX_OPTS -Dcom.sun.management.jmxremote.ssl=false"

# =============================================================================
# Combine All Options
# =============================================================================
export JVM_OPTS="$HEAP_OPTS $GC_OPTS $OFF_HEAP_OPTS $PERF_OPTS $GC_LOG_OPTS $DIAG_OPTS"

echo ""
echo "=== Final JVM Options ==="
echo "$JVM_OPTS" | tr ' ' '\n'
echo ""
echo "To use: export JVM_OPTS and start Ignite"
```

### Optional: Memory Configuration

### Exercise 6: Data Regions and Memory Management

Create `Lab11MemoryConfiguration.java`:

```java
package com.example.ignite;

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
 * Demonstrates Ignite memory configuration with multiple data regions,
 * eviction policies, and memory pressure handling.
 */
public class Lab11MemoryConfiguration {

    private static volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Memory Configuration Lab ===\n");

        IgniteConfiguration cfg = createMemoryOptimizedConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started with custom memory configuration\n");

            // Display initial memory state
            displayMemoryRegions(ignite);

            // Create caches in different regions
            IgniteCache<Integer, String> hotCache = createCacheInRegion(ignite, "hotDataCache", "hot-data-region");
            IgniteCache<Integer, byte[]> warmCache = createCacheInRegion(ignite, "warmDataCache", "warm-data-region");
            IgniteCache<Integer, String> persistentCache = createCacheInRegion(ignite, "persistentCache", "persistent-region");

            // Start memory monitoring
            ScheduledExecutorService monitor = startMemoryMonitor(ignite);

            // Simulate workload on different regions
            System.out.println("\n=== Simulating Workload ===");

            // Hot data - frequently accessed, small entries
            System.out.println("\nPopulating hot data region (small, frequent access)...");
            for (int i = 0; i < 10000; i++) {
                hotCache.put(i, "HotValue-" + i);
            }

            // Warm data - larger entries, less frequent
            System.out.println("Populating warm data region (larger, less frequent)...");
            byte[] largeValue = new byte[4096]; // 4KB entries
            for (int i = 0; i < 5000; i++) {
                warmCache.put(i, largeValue);
                if (i % 1000 == 0) {
                    System.out.println("  Inserted " + i + " entries to warm region...");
                }
            }

            // Persistent data
            System.out.println("Populating persistent region...");
            for (int i = 0; i < 1000; i++) {
                persistentCache.put(i, "PersistentValue-" + i);
            }

            // Display final memory state
            System.out.println("\n=== Final Memory State ===");
            displayMemoryRegions(ignite);

            // Demonstrate eviction by filling up warm region
            System.out.println("\n=== Triggering Eviction in Warm Region ===");
            System.out.println("Adding more data to trigger eviction...");

            for (int i = 5000; i < 20000; i++) {
                warmCache.put(i, largeValue);
                if (i % 2000 == 0) {
                    displayRegionMetrics(ignite, "warm-data-region");
                }
            }

            // Cleanup
            monitor.shutdown();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }

    private static IgniteConfiguration createMemoryOptimizedConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("memory-config-node");

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
        warmRegion.setInitialSize(128L * 1024 * 1024);  // 128 MB initial
        warmRegion.setMaxSize(512L * 1024 * 1024);      // 512 MB max
        warmRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        warmRegion.setEvictionThreshold(0.8);           // Start evicting at 80%
        warmRegion.setMetricsEnabled(true);

        // =================================================================
        // Region 3: Persistent Region
        // With persistence enabled
        // =================================================================
        DataRegionConfiguration persistentRegion = new DataRegionConfiguration();
        persistentRegion.setName("persistent-region");
        persistentRegion.setInitialSize(64L * 1024 * 1024);   // 64 MB initial
        persistentRegion.setMaxSize(256L * 1024 * 1024);      // 256 MB max
        persistentRegion.setPersistenceEnabled(true);         // Enable persistence
        persistentRegion.setCheckpointPageBufferSize(32L * 1024 * 1024); // 32 MB
        persistentRegion.setMetricsEnabled(true);

        // Set default region
        storageCfg.setDefaultDataRegionConfiguration(hotRegion);

        // Add additional regions
        storageCfg.setDataRegionConfigurations(warmRegion, persistentRegion);

        // WAL configuration for persistent region
        storageCfg.setWalPath("./ignite/wal");
        storageCfg.setWalArchivePath("./ignite/wal/archive");
        storageCfg.setStoragePath("./ignite/db");
        storageCfg.setWalSegmentSize(64 * 1024 * 1024);       // 64 MB segments

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
            System.out.println("  Max Size: " + formatBytes(metrics.getMaxSize()));
            System.out.println("  Fill Factor: " +
                String.format("%.2f%%", metrics.getPagesFillFactor() * 100));
            System.out.println("  Pages Filled: " + metrics.getTotalAllocatedPages());
            System.out.println("  Eviction Rate: " +
                String.format("%.2f", metrics.getEvictionRate()) + " pages/sec");
            System.out.println("  Large Entries Pages: " + metrics.getLargeEntriesPagesCount());
        });
    }

    private static void displayRegionMetrics(Ignite ignite, String regionName) {
        ignite.dataRegionMetrics().stream()
            .filter(m -> m.getName().equals(regionName))
            .findFirst()
            .ifPresent(metrics -> {
                System.out.println("  [" + regionName + "] " +
                    "Used: " + formatBytes(metrics.getTotalAllocatedSize()) + " / " +
                    formatBytes(metrics.getMaxSize()) + " | " +
                    "Eviction Rate: " + String.format("%.2f", metrics.getEvictionRate()) + " pages/sec");
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

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
```

### Exercise 7: Memory Pressure Handling

Create `Lab11MemoryPressure.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataPageEvictionMode;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates handling memory pressure in Apache Ignite.
 */
public class Lab11MemoryPressure {

    private static final AtomicLong evictedEntries = new AtomicLong(0);
    private static final AtomicLong expiredEntries = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Memory Pressure Handling Lab ===\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started\n");

            // Activate cluster for persistent region
            ignite.cluster().state(org.apache.ignite.cluster.ClusterState.ACTIVE);

            // Register event listener for eviction
            registerEvictionListener(ignite);

            // Create cache with memory pressure handling
            CacheConfiguration<Integer, byte[]> cacheCfg = new CacheConfiguration<>("pressureTestCache");
            cacheCfg.setDataRegionName("pressure-test-region");
            cacheCfg.setCacheMode(CacheMode.PARTITIONED);
            cacheCfg.setBackups(0);
            cacheCfg.setStatisticsEnabled(true);

            // Enable TTL-based expiry
            cacheCfg.setExpiryPolicyFactory(
                CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 30)));
            cacheCfg.setEagerTtl(true);

            IgniteCache<Integer, byte[]> cache = ignite.getOrCreateCache(cacheCfg);

            // Simulate memory pressure
            System.out.println("=== Simulating Memory Pressure ===\n");

            byte[] data = new byte[10 * 1024]; // 10KB per entry
            int entryCount = 0;

            // Keep inserting until we see eviction
            System.out.println("Inserting data until eviction occurs...\n");

            for (int round = 0; round < 10; round++) {
                System.out.println("Round " + (round + 1) + ":");

                // Insert entries
                for (int i = 0; i < 1000; i++) {
                    int key = round * 1000 + i;
                    cache.put(key, data);
                    entryCount++;
                }

                // Display metrics
                displayPressureMetrics(ignite, cache, entryCount);

                // Small delay to allow eviction to process
                Thread.sleep(1000);
            }

            // Summary
            System.out.println("\n=== Memory Pressure Summary ===");
            System.out.println("Total entries inserted: " + entryCount);
            System.out.println("Current cache size: " + cache.size());
            System.out.println("Evicted entries: " + evictedEntries.get());
            System.out.println("Expired entries: " + expiredEntries.get());

            // Best practices
            displayBestPractices();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("memory-pressure-node");

        // Enable events for monitoring
        cfg.setIncludeEventTypes(
            EventType.EVT_CACHE_ENTRY_EVICTED,
            EventType.EVT_CACHE_OBJECT_EXPIRED
        );

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Create a limited region to demonstrate pressure
        DataRegionConfiguration testRegion = new DataRegionConfiguration();
        testRegion.setName("pressure-test-region");
        testRegion.setInitialSize(32L * 1024 * 1024);   // 32 MB
        testRegion.setMaxSize(64L * 1024 * 1024);       // 64 MB (small to trigger eviction)
        testRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        testRegion.setEvictionThreshold(0.7);           // Start evicting at 70%
        testRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(testRegion);
        cfg.setDataStorageConfiguration(storageCfg);

        return cfg;
    }

    private static void registerEvictionListener(Ignite ignite) {
        IgnitePredicate<org.apache.ignite.events.CacheEvent> evictionListener = event -> {
            if (event.type() == EventType.EVT_CACHE_ENTRY_EVICTED) {
                evictedEntries.incrementAndGet();
            } else if (event.type() == EventType.EVT_CACHE_OBJECT_EXPIRED) {
                expiredEntries.incrementAndGet();
            }
            return true;
        };

        ignite.events().localListen(evictionListener,
            EventType.EVT_CACHE_ENTRY_EVICTED,
            EventType.EVT_CACHE_OBJECT_EXPIRED);

        System.out.println("Eviction listener registered\n");
    }

    private static void displayPressureMetrics(Ignite ignite, IgniteCache<?, ?> cache, int inserted) {
        ignite.dataRegionMetrics().stream()
            .filter(m -> m.getName().equals("pressure-test-region"))
            .findFirst()
            .ifPresent(metrics -> {
                double fillPercent = (double) metrics.getTotalAllocatedSize() / metrics.getMaxSize() * 100;
                System.out.println("  Inserted: " + inserted +
                    " | Cache Size: " + cache.size() +
                    " | Region Fill: " + String.format("%.1f%%", fillPercent) +
                    " | Eviction Rate: " + String.format("%.2f", metrics.getEvictionRate()) + "/s" +
                    " | Evicted: " + evictedEntries.get());
            });
    }

    private static void displayBestPractices() {
        System.out.println("\n=== Memory Pressure Best Practices ===");

        System.out.println("\n1. CONFIGURE APPROPRIATE REGION SIZES:");
        System.out.println("   - Size regions based on expected data volume");
        System.out.println("   - Leave headroom for growth (20-30%)");
        System.out.println("   - Monitor usage patterns over time");

        System.out.println("\n2. USE EVICTION WISELY:");
        System.out.println("   - RANDOM_LRU for general use cases");
        System.out.println("   - RANDOM_2_LRU for better accuracy (slight overhead)");
        System.out.println("   - Set evictionThreshold based on workload (0.7-0.9)");

        System.out.println("\n3. IMPLEMENT TTL POLICIES:");
        System.out.println("   - Use expiry policies for temporary data");
        System.out.println("   - Enable eagerTtl for proactive cleanup");
        System.out.println("   - Consider sliding expiry for session data");

        System.out.println("\n4. MONITOR AND ALERT:");
        System.out.println("   - Enable metrics on data regions");
        System.out.println("   - Set up alerts for high fill factors");
        System.out.println("   - Track eviction rates over time");

        System.out.println("\n5. HANDLE OUTOFMEMORY:");
        System.out.println("   - Configure -XX:+HeapDumpOnOutOfMemoryError");
        System.out.println("   - Implement circuit breakers");
        System.out.println("   - Have a scaling strategy ready");
    }
}
```

### Optional: Query Performance Tuning

### Exercise 8: SQL Query Optimization

Create `Lab11QueryPerformance.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.SqlConfiguration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 * Demonstrates SQL query performance tuning in Apache Ignite.
 */
public class Lab11QueryPerformance {

    private static final int DATA_SIZE = 100000;
    private static final Random random = new Random(42);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Query Performance Tuning Lab ===\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started\n");

            // Create caches with and without indexes
            IgniteCache<Long, Order> indexedCache = createIndexedCache(ignite);
            IgniteCache<Long, Order> nonIndexedCache = createNonIndexedCache(ignite);

            // Populate data
            System.out.println("Populating " + DATA_SIZE + " orders...");
            populateData(indexedCache, nonIndexedCache);
            System.out.println("Data populated\n");

            // Compare query performance
            System.out.println("=== Query Performance Comparison ===\n");

            // Test 1: Point lookup by customer
            System.out.println("--- Test 1: Filter by Customer ID ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT * FROM Order WHERE customerId = ?",
                new Object[]{1000L});

            // Test 2: Range query on amount
            System.out.println("\n--- Test 2: Range Query on Amount ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT * FROM Order WHERE amount BETWEEN ? AND ?",
                new Object[]{5000.0, 10000.0});

            // Test 3: Aggregation
            System.out.println("\n--- Test 3: Aggregation Query ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT customerId, SUM(amount), COUNT(*) FROM Order GROUP BY customerId HAVING COUNT(*) > ?",
                new Object[]{5});

            // Test 4: Sorting
            System.out.println("\n--- Test 4: Sorting Query ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT * FROM Order WHERE status = ? ORDER BY orderDate DESC LIMIT ?",
                new Object[]{"COMPLETED", 100});

            // Demonstrate query parallelism
            System.out.println("\n=== Query Parallelism ===");
            testQueryParallelism(indexedCache);

            // Demonstrate lazy query execution
            System.out.println("\n=== Lazy Query Execution ===");
            testLazyExecution(indexedCache);

            // Show query plan
            System.out.println("\n=== Query Plan Analysis ===");
            analyzeQueryPlan(indexedCache);

            // Display best practices
            displayQueryOptimizationTips();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("query-tuning-node");

        // SQL configuration
        SqlConfiguration sqlCfg = new SqlConfiguration();
        sqlCfg.setQueryEnginesConfiguration(
            new org.apache.ignite.indexing.IndexingQueryEngineConfiguration()
        );

        cfg.setSqlConfiguration(sqlCfg);

        return cfg;
    }

    private static IgniteCache<Long, Order> createIndexedCache(Ignite ignite) {
        CacheConfiguration<Long, Order> cfg = new CacheConfiguration<>("indexedOrders");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(0);
        cfg.setStatisticsEnabled(true);
        cfg.setSqlSchema("PUBLIC");

        // Define query entity with indexes
        QueryEntity entity = new QueryEntity(Long.class.getName(), Order.class.getName());
        entity.setTableName("Order");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("orderId", Long.class.getName());
        fields.put("customerId", Long.class.getName());
        fields.put("amount", Double.class.getName());
        fields.put("status", String.class.getName());
        fields.put("orderDate", Long.class.getName());
        entity.setFields(fields);

        entity.setKeyFieldName("orderId");

        // Create indexes
        QueryIndex customerIdx = new QueryIndex("customerId", QueryIndexType.SORTED);
        customerIdx.setName("idx_customer");

        QueryIndex amountIdx = new QueryIndex("amount", QueryIndexType.SORTED);
        amountIdx.setName("idx_amount");

        QueryIndex statusDateIdx = new QueryIndex(
            Arrays.asList("status", "orderDate"), QueryIndexType.SORTED);
        statusDateIdx.setName("idx_status_date");

        entity.setIndexes(Arrays.asList(customerIdx, amountIdx, statusDateIdx));

        cfg.setQueryEntities(Collections.singletonList(entity));

        return ignite.getOrCreateCache(cfg);
    }

    private static IgniteCache<Long, Order> createNonIndexedCache(Ignite ignite) {
        CacheConfiguration<Long, Order> cfg = new CacheConfiguration<>("nonIndexedOrders");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(0);
        cfg.setStatisticsEnabled(true);
        cfg.setSqlSchema("PUBLIC");

        // Define query entity WITHOUT indexes
        QueryEntity entity = new QueryEntity(Long.class.getName(), Order.class.getName());
        entity.setTableName("OrderNoIdx");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("orderId", Long.class.getName());
        fields.put("customerId", Long.class.getName());
        fields.put("amount", Double.class.getName());
        fields.put("status", String.class.getName());
        fields.put("orderDate", Long.class.getName());
        entity.setFields(fields);

        entity.setKeyFieldName("orderId");
        // No indexes!

        cfg.setQueryEntities(Collections.singletonList(entity));

        return ignite.getOrCreateCache(cfg);
    }

    private static void populateData(IgniteCache<Long, Order> indexed,
                                      IgniteCache<Long, Order> nonIndexed) {
        String[] statuses = {"PENDING", "PROCESSING", "COMPLETED", "CANCELLED"};

        for (long i = 0; i < DATA_SIZE; i++) {
            Order order = new Order();
            order.orderId = i;
            order.customerId = random.nextInt(10000);
            order.amount = 100.0 + random.nextDouble() * 10000;
            order.status = statuses[random.nextInt(statuses.length)];
            order.orderDate = System.currentTimeMillis() - random.nextInt(365 * 24 * 60 * 60 * 1000);

            indexed.put(i, order);
            nonIndexed.put(i, order);
        }
    }

    private static void compareQueryPerformance(
            IgniteCache<Long, Order> indexed,
            IgniteCache<Long, Order> nonIndexed,
            String sql, Object[] params) {

        // Replace table name for non-indexed
        String sqlNonIndexed = sql.replace("Order", "OrderNoIdx");

        // Warm up
        for (int i = 0; i < 3; i++) {
            executeQuery(indexed, sql, params);
            executeQuery(nonIndexed, sqlNonIndexed, params);
        }

        // Benchmark indexed
        long startTime = System.nanoTime();
        int indexedCount = 0;
        for (int i = 0; i < 10; i++) {
            indexedCount = executeQuery(indexed, sql, params);
        }
        long indexedTime = (System.nanoTime() - startTime) / 10;

        // Benchmark non-indexed
        startTime = System.nanoTime();
        int nonIndexedCount = 0;
        for (int i = 0; i < 10; i++) {
            nonIndexedCount = executeQuery(nonIndexed, sqlNonIndexed, params);
        }
        long nonIndexedTime = (System.nanoTime() - startTime) / 10;

        System.out.println("Results: " + indexedCount + " rows");
        System.out.println("Indexed:     " + String.format("%,d", indexedTime / 1000) + " us");
        System.out.println("Non-Indexed: " + String.format("%,d", nonIndexedTime / 1000) + " us");
        System.out.println("Speedup:     " + String.format("%.2fx", (double) nonIndexedTime / indexedTime));
    }

    private static int executeQuery(IgniteCache<?, ?> cache, String sql, Object[] params) {
        SqlFieldsQuery query = new SqlFieldsQuery(sql);
        if (params != null && params.length > 0) {
            query.setArgs(params);
        }

        int count = 0;
        try (FieldsQueryCursor<List<?>> cursor = cache.query(query)) {
            for (List<?> row : cursor) {
                count++;
            }
        }
        return count;
    }

    private static void testQueryParallelism(IgniteCache<Long, Order> cache) {
        String sql = "SELECT customerId, SUM(amount), AVG(amount), COUNT(*) " +
                    "FROM Order GROUP BY customerId";

        System.out.println("Testing query with different parallelism levels...\n");

        for (int parallelism : Arrays.asList(1, 2, 4, 8)) {
            SqlFieldsQuery query = new SqlFieldsQuery(sql);
            query.setPageSize(1000);

            // Warm up
            for (int i = 0; i < 3; i++) {
                try (FieldsQueryCursor<List<?>> cursor = cache.query(query)) {
                    cursor.getAll();
                }
            }

            // Benchmark
            long startTime = System.nanoTime();
            int rowCount = 0;
            for (int i = 0; i < 5; i++) {
                try (FieldsQueryCursor<List<?>> cursor = cache.query(query)) {
                    rowCount = cursor.getAll().size();
                }
            }
            long avgTime = (System.nanoTime() - startTime) / 5;

            System.out.println("Parallelism " + parallelism + ": " +
                String.format("%,d", avgTime / 1000000) + " ms (" + rowCount + " rows)");
        }
    }

    private static void testLazyExecution(IgniteCache<Long, Order> cache) {
        String sql = "SELECT * FROM Order WHERE amount > 5000 ORDER BY amount DESC";

        System.out.println("Comparing lazy vs eager execution...\n");

        // Lazy execution (streaming results)
        SqlFieldsQuery lazyQuery = new SqlFieldsQuery(sql);
        lazyQuery.setLazy(true);
        lazyQuery.setPageSize(100);

        long startTime = System.nanoTime();
        int lazyCount = 0;
        try (FieldsQueryCursor<List<?>> cursor = cache.query(lazyQuery)) {
            for (List<?> row : cursor) {
                lazyCount++;
                if (lazyCount >= 100) break; // Only need first 100
            }
        }
        long lazyTime = System.nanoTime() - startTime;
        System.out.println("Lazy (first 100):  " + String.format("%,d", lazyTime / 1000) + " us");

        // Eager execution (load all then take first 100)
        SqlFieldsQuery eagerQuery = new SqlFieldsQuery(sql);
        eagerQuery.setLazy(false);

        startTime = System.nanoTime();
        int eagerCount = 0;
        try (FieldsQueryCursor<List<?>> cursor = cache.query(eagerQuery)) {
            List<List<?>> all = cursor.getAll();
            for (int i = 0; i < Math.min(100, all.size()); i++) {
                eagerCount++;
            }
        }
        long eagerTime = System.nanoTime() - startTime;
        System.out.println("Eager (all->100):  " + String.format("%,d", eagerTime / 1000) + " us");
        System.out.println("Lazy advantage:    " + String.format("%.2fx", (double) eagerTime / lazyTime));
    }

    private static void analyzeQueryPlan(IgniteCache<Long, Order> cache) {
        String[] queries = {
            "SELECT * FROM Order WHERE customerId = 1000",
            "SELECT * FROM Order WHERE amount BETWEEN 5000 AND 10000",
            "SELECT * FROM Order WHERE status = 'COMPLETED' ORDER BY orderDate DESC"
        };

        for (String sql : queries) {
            System.out.println("\nQuery: " + sql);
            System.out.println("Plan:");

            SqlFieldsQuery explainQuery = new SqlFieldsQuery("EXPLAIN " + sql);
            try (FieldsQueryCursor<List<?>> cursor = cache.query(explainQuery)) {
                for (List<?> row : cursor) {
                    System.out.println("  " + row.get(0));
                }
            }
        }
    }

    private static void displayQueryOptimizationTips() {
        System.out.println("\n=== Query Optimization Best Practices ===");

        System.out.println("\n1. INDEX STRATEGY:");
        System.out.println("   - Create indexes on frequently filtered columns");
        System.out.println("   - Use composite indexes for multi-column filters");
        System.out.println("   - Consider index order (leftmost prefix rule)");
        System.out.println("   - Avoid over-indexing (impacts write performance)");

        System.out.println("\n2. QUERY OPTIMIZATION:");
        System.out.println("   - Use EXPLAIN to analyze query plans");
        System.out.println("   - Avoid SELECT * - specify needed columns");
        System.out.println("   - Use LIMIT for pagination");
        System.out.println("   - Prefer batch operations over single-row queries");

        System.out.println("\n3. PARALLELISM:");
        System.out.println("   - Default parallelism is query.parallelism (usually 1)");
        System.out.println("   - Increase for CPU-intensive aggregations");
        System.out.println("   - Be mindful of resource consumption");

        System.out.println("\n4. LAZY EXECUTION:");
        System.out.println("   - Use setLazy(true) for large result sets");
        System.out.println("   - Streams results instead of loading all");
        System.out.println("   - Essential for LIMIT queries");

        System.out.println("\n5. PAGE SIZE:");
        System.out.println("   - Default is 1024");
        System.out.println("   - Increase for large result sets");
        System.out.println("   - Decrease for memory-constrained environments");

        System.out.println("\n6. COLOCATION:");
        System.out.println("   - Design for data colocation to avoid distributed joins");
        System.out.println("   - Use affinity keys for related data");
        System.out.println("   - Only enable distributed joins when necessary");
    }

    // Order class
    public static class Order implements Serializable {
        public long orderId;
        public long customerId;
        public double amount;
        public String status;
        public long orderDate;
    }
}
```

### Optional: Benchmarking Lab

### Exercise 9: Comprehensive Performance Test Suite

Create `Lab11BenchmarkingSuite.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.transactions.Transaction;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Comprehensive benchmarking suite for Apache Ignite.
 * Measures throughput, latency, and generates reports.
 */
public class Lab11BenchmarkingSuite {

    // Benchmark configuration
    private static final int WARMUP_SECONDS = 5;
    private static final int TEST_SECONDS = 10;
    private static final int[] THREAD_COUNTS = {1, 2, 4, 8, 16};
    private static final int ENTRY_COUNT = 100000;
    private static final int VALUE_SIZE_BYTES = 1024; // 1KB

    // Results storage
    private static final Map<String, List<BenchmarkResult>> allResults = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Apache Ignite Benchmarking Suite ===\n");
        System.out.println("Configuration:");
        System.out.println("  Warmup: " + WARMUP_SECONDS + "s");
        System.out.println("  Test Duration: " + TEST_SECONDS + "s");
        System.out.println("  Thread Counts: " + java.util.Arrays.toString(THREAD_COUNTS));
        System.out.println("  Entry Count: " + ENTRY_COUNT);
        System.out.println("  Value Size: " + VALUE_SIZE_BYTES + " bytes\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("benchmark-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started\n");

            // Run all benchmarks
            runPutBenchmark(ignite);
            runGetBenchmark(ignite);
            runPutAllBenchmark(ignite);
            runMixedWorkloadBenchmark(ignite);
            runTransactionalBenchmark(ignite);

            // Generate report
            generateReport();

            System.out.println("\nAll benchmarks completed!");
            System.out.println("Report saved to: benchmark_report.txt");

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }

    private static void runPutBenchmark(Ignite ignite) {
        System.out.println("=== PUT Benchmark ===\n");

        CacheConfiguration<Integer, byte[]> cfg = new CacheConfiguration<>("putBenchCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cfg.setBackups(1);

        IgniteCache<Integer, byte[]> cache = ignite.getOrCreateCache(cfg);
        byte[] value = new byte[VALUE_SIZE_BYTES];
        new Random().nextBytes(value);

        List<BenchmarkResult> results = new ArrayList<>();

        for (int threads : THREAD_COUNTS) {
            BenchmarkResult result = runBenchmark("PUT", threads, () -> {
                Random rnd = new Random();
                cache.put(rnd.nextInt(ENTRY_COUNT), value);
            });
            results.add(result);
            printResult(result);
        }

        allResults.put("PUT", results);
        ignite.destroyCache("putBenchCache");
    }

    private static void runGetBenchmark(Ignite ignite) {
        System.out.println("\n=== GET Benchmark ===\n");

        CacheConfiguration<Integer, byte[]> cfg = new CacheConfiguration<>("getBenchCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cfg.setBackups(1);

        IgniteCache<Integer, byte[]> cache = ignite.getOrCreateCache(cfg);

        // Pre-populate
        byte[] value = new byte[VALUE_SIZE_BYTES];
        System.out.println("Pre-populating cache...");
        for (int i = 0; i < ENTRY_COUNT; i++) {
            cache.put(i, value);
        }
        System.out.println("Cache populated with " + ENTRY_COUNT + " entries\n");

        List<BenchmarkResult> results = new ArrayList<>();

        for (int threads : THREAD_COUNTS) {
            BenchmarkResult result = runBenchmark("GET", threads, () -> {
                Random rnd = new Random();
                cache.get(rnd.nextInt(ENTRY_COUNT));
            });
            results.add(result);
            printResult(result);
        }

        allResults.put("GET", results);
        ignite.destroyCache("getBenchCache");
    }

    private static void runPutAllBenchmark(Ignite ignite) {
        System.out.println("\n=== PUT_ALL Benchmark (Batch Size: 100) ===\n");

        CacheConfiguration<Integer, byte[]> cfg = new CacheConfiguration<>("putAllBenchCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cfg.setBackups(1);

        IgniteCache<Integer, byte[]> cache = ignite.getOrCreateCache(cfg);
        byte[] value = new byte[VALUE_SIZE_BYTES];

        List<BenchmarkResult> results = new ArrayList<>();

        for (int threads : THREAD_COUNTS) {
            BenchmarkResult result = runBenchmark("PUT_ALL", threads, () -> {
                Random rnd = new Random();
                Map<Integer, byte[]> batch = new HashMap<>();
                for (int i = 0; i < 100; i++) {
                    batch.put(rnd.nextInt(ENTRY_COUNT), value);
                }
                cache.putAll(batch);
            });
            // Adjust ops count (each call = 100 operations)
            result.throughput *= 100;
            results.add(result);
            printResult(result);
        }

        allResults.put("PUT_ALL", results);
        ignite.destroyCache("putAllBenchCache");
    }

    private static void runMixedWorkloadBenchmark(Ignite ignite) {
        System.out.println("\n=== Mixed Workload (80% Read, 20% Write) ===\n");

        CacheConfiguration<Integer, byte[]> cfg = new CacheConfiguration<>("mixedBenchCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cfg.setBackups(1);

        IgniteCache<Integer, byte[]> cache = ignite.getOrCreateCache(cfg);
        byte[] value = new byte[VALUE_SIZE_BYTES];

        // Pre-populate
        for (int i = 0; i < ENTRY_COUNT; i++) {
            cache.put(i, value);
        }

        List<BenchmarkResult> results = new ArrayList<>();

        for (int threads : THREAD_COUNTS) {
            BenchmarkResult result = runBenchmark("MIXED_80R_20W", threads, () -> {
                Random rnd = new Random();
                if (rnd.nextInt(100) < 80) {
                    cache.get(rnd.nextInt(ENTRY_COUNT));
                } else {
                    cache.put(rnd.nextInt(ENTRY_COUNT), value);
                }
            });
            results.add(result);
            printResult(result);
        }

        allResults.put("MIXED_80R_20W", results);
        ignite.destroyCache("mixedBenchCache");
    }

    private static void runTransactionalBenchmark(Ignite ignite) {
        System.out.println("\n=== Transactional PUT Benchmark ===\n");

        CacheConfiguration<Integer, byte[]> cfg = new CacheConfiguration<>("txBenchCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setBackups(1);

        IgniteCache<Integer, byte[]> cache = ignite.getOrCreateCache(cfg);
        byte[] value = new byte[VALUE_SIZE_BYTES];

        List<BenchmarkResult> results = new ArrayList<>();

        for (int threads : THREAD_COUNTS) {
            BenchmarkResult result = runBenchmark("TX_PUT", threads, () -> {
                Random rnd = new Random();
                try (Transaction tx = ignite.transactions().txStart()) {
                    cache.put(rnd.nextInt(ENTRY_COUNT), value);
                    tx.commit();
                }
            });
            results.add(result);
            printResult(result);
        }

        allResults.put("TX_PUT", results);
        ignite.destroyCache("txBenchCache");
    }

    private static BenchmarkResult runBenchmark(String name, int threads, Runnable operation) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        LongAdder operationCount = new LongAdder();
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        AtomicLong errors = new AtomicLong(0);

        // Warmup
        CountDownLatch warmupLatch = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                while (warmupLatch.getCount() > 0) {
                    try {
                        operation.run();
                    } catch (Exception e) {
                        // Ignore during warmup
                    }
                }
            });
        }

        try {
            Thread.sleep(WARMUP_SECONDS * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        warmupLatch.countDown();

        // Actual test
        CountDownLatch testLatch = new CountDownLatch(1);
        long testStart = System.currentTimeMillis();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                while (testLatch.getCount() > 0) {
                    long opStart = System.nanoTime();
                    try {
                        operation.run();
                        long opTime = System.nanoTime() - opStart;
                        operationCount.increment();
                        // Sample latencies (1 in 100)
                        if (operationCount.sum() % 100 == 0) {
                            latencies.add(opTime / 1000); // Convert to microseconds
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
        }

        try {
            Thread.sleep(TEST_SECONDS * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        testLatch.countDown();
        long testEnd = System.currentTimeMillis();

        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Calculate results
        long totalOps = operationCount.sum();
        double durationSeconds = (testEnd - testStart) / 1000.0;
        double throughput = totalOps / durationSeconds;

        // Calculate latency percentiles
        Collections.sort(latencies);
        long p50 = latencies.isEmpty() ? 0 : latencies.get(latencies.size() / 2);
        long p95 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.99));

        return new BenchmarkResult(name, threads, throughput, p50, p95, p99, errors.get());
    }

    private static void printResult(BenchmarkResult result) {
        System.out.printf("  Threads: %2d | Throughput: %,10.0f ops/s | " +
            "Latency (us) p50: %,6d p95: %,6d p99: %,6d | Errors: %d%n",
            result.threads, result.throughput,
            result.p50Latency, result.p95Latency, result.p99Latency, result.errors);
    }

    private static void generateReport() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try (PrintWriter writer = new PrintWriter(new FileWriter("benchmark_report.txt"))) {
            writer.println("=".repeat(80));
            writer.println("APACHE IGNITE BENCHMARK REPORT");
            writer.println("Generated: " + timestamp);
            writer.println("=".repeat(80));
            writer.println();

            writer.println("CONFIGURATION:");
            writer.println("-".repeat(40));
            writer.printf("  Warmup Duration:   %d seconds%n", WARMUP_SECONDS);
            writer.printf("  Test Duration:     %d seconds%n", TEST_SECONDS);
            writer.printf("  Entry Count:       %,d%n", ENTRY_COUNT);
            writer.printf("  Value Size:        %,d bytes%n", VALUE_SIZE_BYTES);
            writer.printf("  Thread Counts:     %s%n", java.util.Arrays.toString(THREAD_COUNTS));
            writer.println();

            for (Map.Entry<String, List<BenchmarkResult>> entry : allResults.entrySet()) {
                writer.println("BENCHMARK: " + entry.getKey());
                writer.println("-".repeat(40));
                writer.printf("%-8s %15s %12s %12s %12s %8s%n",
                    "Threads", "Throughput", "P50 (us)", "P95 (us)", "P99 (us)", "Errors");

                for (BenchmarkResult result : entry.getValue()) {
                    writer.printf("%-8d %,15.0f %,12d %,12d %,12d %,8d%n",
                        result.threads, result.throughput,
                        result.p50Latency, result.p95Latency, result.p99Latency, result.errors);
                }
                writer.println();
            }

            writer.println("SUMMARY:");
            writer.println("-".repeat(40));
            for (Map.Entry<String, List<BenchmarkResult>> entry : allResults.entrySet()) {
                BenchmarkResult best = entry.getValue().stream()
                    .max((a, b) -> Double.compare(a.throughput, b.throughput))
                    .orElse(null);
                if (best != null) {
                    writer.printf("  %s: Best throughput %.0f ops/s @ %d threads%n",
                        entry.getKey(), best.throughput, best.threads);
                }
            }

            writer.println();
            writer.println("=".repeat(80));

            System.out.println("\n=== Report Summary ===");
            for (Map.Entry<String, List<BenchmarkResult>> entry : allResults.entrySet()) {
                BenchmarkResult best = entry.getValue().stream()
                    .max((a, b) -> Double.compare(a.throughput, b.throughput))
                    .orElse(null);
                if (best != null) {
                    System.out.printf("%s: Best %.0f ops/s @ %d threads%n",
                        entry.getKey(), best.throughput, best.threads);
                }
            }

        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
    }

    static class BenchmarkResult {
        String name;
        int threads;
        double throughput;
        long p50Latency;
        long p95Latency;
        long p99Latency;
        long errors;

        BenchmarkResult(String name, int threads, double throughput,
                       long p50, long p95, long p99, long errors) {
            this.name = name;
            this.threads = threads;
            this.throughput = throughput;
            this.p50Latency = p50;
            this.p95Latency = p95;
            this.p99Latency = p99;
            this.errors = errors;
        }
    }
}
```

### Optional: Challenge Exercises

### Challenge 1: Performance Dashboard

Create `Lab11PerformanceDashboard.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time performance dashboard for Apache Ignite monitoring.
 */
public class Lab11PerformanceDashboard {

    private static final AtomicLong totalOperations = new AtomicLong(0);
    private static final AtomicLong lastOperations = new AtomicLong(0);
    private static volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Apache Ignite Performance Dashboard ===\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started\n");

            // Create test cache
            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("dashboardCache");
            cacheCfg.setStatisticsEnabled(true);
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Start workload generator
            ScheduledExecutorService workloadExecutor = startWorkloadGenerator(cache);

            // Start dashboard refresh
            ScheduledExecutorService dashboardExecutor = startDashboard(ignite, cache);

            System.out.println("Dashboard running. Press Enter to stop...\n");
            Thread.sleep(1000); // Let dashboard start
            System.in.read();

            running = false;
            workloadExecutor.shutdown();
            dashboardExecutor.shutdown();
        }
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("dashboard-node");
        cfg.setMetricsLogFrequency(0); // Disable default metrics logging

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setMetricsEnabled(true);
        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        cfg.setDataStorageConfiguration(storageCfg);

        return cfg;
    }

    private static ScheduledExecutorService startWorkloadGenerator(IgniteCache<Integer, String> cache) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        Random random = new Random();

        for (int i = 0; i < 4; i++) {
            executor.scheduleAtFixedRate(() -> {
                if (!running) return;
                try {
                    int key = random.nextInt(10000);
                    if (random.nextBoolean()) {
                        cache.put(key, "Value-" + key);
                    } else {
                        cache.get(key);
                    }
                    totalOperations.incrementAndGet();
                } catch (Exception e) {
                    // Ignore
                }
            }, 0, 1, TimeUnit.MILLISECONDS);
        }

        return executor;
    }

    private static ScheduledExecutorService startDashboard(Ignite ignite, IgniteCache<Integer, String> cache) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            if (!running) return;
            printDashboard(ignite, cache);
        }, 0, 2, TimeUnit.SECONDS);

        return executor;
    }

    private static void printDashboard(Ignite ignite, IgniteCache<Integer, String> cache) {
        // Clear screen (works on most terminals)
        System.out.print("\033[H\033[2J");
        System.out.flush();

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

        System.out.println("+" + "-".repeat(78) + "+");
        System.out.println("|" + centerText("APACHE IGNITE PERFORMANCE DASHBOARD", 78) + "|");
        System.out.println("|" + centerText("Last Updated: " + timestamp, 78) + "|");
        System.out.println("+" + "-".repeat(78) + "+");

        // Operations per second
        long currentOps = totalOperations.get();
        long opsPerSecond = (currentOps - lastOperations.get()) / 2;
        lastOperations.set(currentOps);

        System.out.println("|" + centerText("THROUGHPUT", 78) + "|");
        System.out.println("|  Operations/sec: " + padRight(String.format("%,d", opsPerSecond), 58) + "|");
        System.out.println("|  Total Operations: " + padRight(String.format("%,d", currentOps), 56) + "|");
        System.out.println("+" + "-".repeat(78) + "+");

        // Cache metrics
        CacheMetrics metrics = cache.metrics();
        System.out.println("|" + centerText("CACHE METRICS: " + cache.getName(), 78) + "|");
        System.out.println("|  Size: " + padRight(String.valueOf(cache.size()), 68) + "|");
        System.out.println("|  Gets: " + padRight(String.format("%,d", metrics.getCacheGets()), 68) + "|");
        System.out.println("|  Puts: " + padRight(String.format("%,d", metrics.getCachePuts()), 68) + "|");
        System.out.println("|  Hit Rate: " + padRight(String.format("%.2f%%", metrics.getCacheHitPercentage()), 64) + "|");
        System.out.println("|  Avg Get Time: " + padRight(String.format("%.3f ms", metrics.getAverageGetTime()), 60) + "|");
        System.out.println("|  Avg Put Time: " + padRight(String.format("%.3f ms", metrics.getAveragePutTime()), 60) + "|");
        System.out.println("+" + "-".repeat(78) + "+");

        // Cluster metrics
        ClusterMetrics clusterMetrics = ignite.cluster().metrics();
        System.out.println("|" + centerText("CLUSTER METRICS", 78) + "|");
        System.out.println("|  Nodes: " + padRight(String.valueOf(ignite.cluster().nodes().size()), 67) + "|");
        System.out.println("|  CPUs: " + padRight(String.valueOf(clusterMetrics.getTotalCpus()), 68) + "|");
        System.out.println("|  CPU Load: " +
            padRight(String.format("%.1f%%", clusterMetrics.getCurrentCpuLoad() * 100), 64) + "|");
        System.out.println("+" + "-".repeat(78) + "+");

        // Memory metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        System.out.println("|" + centerText("MEMORY", 78) + "|");
        System.out.println("|  Heap Used: " +
            padRight(formatBytes(heapUsage.getUsed()) + " / " + formatBytes(heapUsage.getMax()), 63) + "|");
        System.out.println("|  Heap %: " +
            padRight(String.format("%.1f%%", (double) heapUsage.getUsed() / heapUsage.getMax() * 100), 66) + "|");

        // Data regions
        ignite.dataRegionMetrics().forEach(m -> {
            System.out.println("|  Region '" + m.getName() + "': " +
                padRight(formatBytes(m.getTotalAllocatedSize()) + " / " + formatBytes(m.getMaxSize()),
                    60 - m.getName().length()) + "|");
        });
        System.out.println("+" + "-".repeat(78) + "+");

        // GC metrics
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        System.out.println("|" + centerText("GARBAGE COLLECTION", 78) + "|");
        for (GarbageCollectorMXBean gc : gcBeans) {
            System.out.println("|  " + gc.getName() + ": " +
                padRight(gc.getCollectionCount() + " collections, " + gc.getCollectionTime() + " ms",
                    73 - gc.getName().length()) + "|");
        }
        System.out.println("+" + "-".repeat(78) + "+");

        System.out.println("\nPress Enter to stop dashboard...");
    }

    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - padding - text.length());
    }

    private static String padRight(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
```

### Challenge 2: Auto-Tuning Utility

Create `Lab11AutoTuner.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Auto-tuning utility that analyzes workload and recommends configurations.
 */
public class Lab11AutoTuner {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Ignite Auto-Tuning Utility ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("auto-tuner-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started\n");

            // Create test cache
            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("tuningCache");
            cacheCfg.setStatisticsEnabled(true);
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Analyze system
            System.out.println("=== System Analysis ===\n");
            SystemProfile profile = analyzeSystem();
            printSystemProfile(profile);

            // Generate workload for analysis
            System.out.println("\n=== Workload Analysis ===\n");
            System.out.println("Generating sample workload...");
            WorkloadProfile workload = analyzeWorkload(cache);
            printWorkloadProfile(workload);

            // Generate recommendations
            System.out.println("\n=== Auto-Tuning Recommendations ===\n");
            generateRecommendations(profile, workload);

            // Generate configuration
            System.out.println("\n=== Generated Configuration ===\n");
            generateConfiguration(profile, workload);

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }

    private static SystemProfile analyzeSystem() {
        SystemProfile profile = new SystemProfile();

        Runtime runtime = Runtime.getRuntime();
        profile.availableProcessors = runtime.availableProcessors();
        profile.maxHeapMemory = runtime.maxMemory();
        profile.totalSystemMemory = getTotalSystemMemory();
        profile.javaVersion = System.getProperty("java.version");
        profile.osName = System.getProperty("os.name");

        return profile;
    }

    private static long getTotalSystemMemory() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return os.getTotalMemorySize();
        } catch (Exception e) {
            return Runtime.getRuntime().maxMemory() * 2; // Estimate
        }
    }

    private static void printSystemProfile(SystemProfile profile) {
        System.out.println("System Profile:");
        System.out.println("  OS: " + profile.osName);
        System.out.println("  Java Version: " + profile.javaVersion);
        System.out.println("  CPU Cores: " + profile.availableProcessors);
        System.out.println("  Total System Memory: " + formatBytes(profile.totalSystemMemory));
        System.out.println("  Max JVM Heap: " + formatBytes(profile.maxHeapMemory));
    }

    private static WorkloadProfile analyzeWorkload(IgniteCache<Integer, String> cache) {
        WorkloadProfile workload = new WorkloadProfile();
        Random random = new Random();

        // Generate mixed workload
        long startTime = System.currentTimeMillis();
        int reads = 0, writes = 0;

        for (int i = 0; i < 10000; i++) {
            int key = random.nextInt(5000);
            if (random.nextInt(100) < 70) { // 70% reads
                cache.get(key);
                reads++;
            } else {
                cache.put(key, "Value-" + key);
                writes++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        CacheMetrics metrics = cache.metrics();

        workload.readWriteRatio = (double) reads / writes;
        workload.operationsPerSecond = 10000.0 / duration * 1000;
        workload.avgGetTime = metrics.getAverageGetTime();
        workload.avgPutTime = metrics.getAveragePutTime();
        workload.hitRatio = metrics.getCacheHitPercentage();
        workload.entryCount = cache.size();

        return workload;
    }

    private static void printWorkloadProfile(WorkloadProfile workload) {
        System.out.println("Workload Profile:");
        System.out.printf("  Read/Write Ratio: %.2f:1%n", workload.readWriteRatio);
        System.out.printf("  Operations/sec: %.0f%n", workload.operationsPerSecond);
        System.out.printf("  Avg GET Time: %.3f ms%n", workload.avgGetTime);
        System.out.printf("  Avg PUT Time: %.3f ms%n", workload.avgPutTime);
        System.out.printf("  Cache Hit Ratio: %.2f%%%n", workload.hitRatio);
        System.out.println("  Entry Count: " + workload.entryCount);
    }

    private static void generateRecommendations(SystemProfile system, WorkloadProfile workload) {
        System.out.println("Based on analysis, here are the recommendations:\n");

        // Heap sizing
        long recommendedHeap = (long) (system.totalSystemMemory * 0.6);
        System.out.println("1. HEAP CONFIGURATION:");
        System.out.println("   Current Max Heap: " + formatBytes(system.maxHeapMemory));
        System.out.println("   Recommended Heap: " + formatBytes(recommendedHeap));
        System.out.println("   Flags: -Xms" + (recommendedHeap / (1024*1024*1024)) +
            "g -Xmx" + (recommendedHeap / (1024*1024*1024)) + "g");

        // Off-heap
        long recommendedOffHeap = (long) (system.totalSystemMemory * 0.25);
        System.out.println("\n2. OFF-HEAP CONFIGURATION:");
        System.out.println("   Recommended Off-Heap: " + formatBytes(recommendedOffHeap));
        System.out.println("   Flag: -XX:MaxDirectMemorySize=" +
            (recommendedOffHeap / (1024*1024*1024)) + "g");

        // Thread pools
        int publicPool = system.availableProcessors * 2;
        int systemPool = system.availableProcessors;
        System.out.println("\n3. THREAD POOL CONFIGURATION:");
        System.out.println("   Public Thread Pool: " + publicPool);
        System.out.println("   System Thread Pool: " + systemPool);

        // Cache configuration based on workload
        System.out.println("\n4. CACHE CONFIGURATION:");
        if (workload.readWriteRatio > 5) {
            System.out.println("   Workload Type: Read-Heavy");
            System.out.println("   Recommendation: Use ATOMIC atomicity mode");
            System.out.println("   Recommendation: Consider read-through caching");
        } else if (workload.readWriteRatio < 1) {
            System.out.println("   Workload Type: Write-Heavy");
            System.out.println("   Recommendation: Use write-behind for persistence");
            System.out.println("   Recommendation: Consider larger batch sizes");
        } else {
            System.out.println("   Workload Type: Balanced");
            System.out.println("   Recommendation: Monitor and adjust based on peaks");
        }

        // GC configuration
        System.out.println("\n5. GARBAGE COLLECTION:");
        System.out.println("   Recommended GC: G1GC");
        System.out.println("   Flags: -XX:+UseG1GC -XX:MaxGCPauseMillis=200");
        if (recommendedHeap > 8L * 1024 * 1024 * 1024) {
            System.out.println("   Large heap detected - consider ZGC (Java 15+)");
        }

        // Backups
        System.out.println("\n6. BACKUP CONFIGURATION:");
        System.out.println("   Recommendation: 1 backup for fault tolerance");
        System.out.println("   Note: Each backup doubles memory usage");
    }

    private static void generateConfiguration(SystemProfile system, WorkloadProfile workload) {
        long heapGB = (long) (system.totalSystemMemory * 0.6 / (1024*1024*1024));
        long offHeapGB = (long) (system.totalSystemMemory * 0.25 / (1024*1024*1024));
        int processors = system.availableProcessors;

        System.out.println("# JVM Options (add to JAVA_OPTS or ignite.sh)");
        System.out.println("# ============================================");
        System.out.println();
        System.out.println("# Heap Configuration");
        System.out.println("-Xms" + heapGB + "g");
        System.out.println("-Xmx" + heapGB + "g");
        System.out.println();
        System.out.println("# G1GC Configuration");
        System.out.println("-XX:+UseG1GC");
        System.out.println("-XX:G1HeapRegionSize=32m");
        System.out.println("-XX:MaxGCPauseMillis=200");
        System.out.println("-XX:InitiatingHeapOccupancyPercent=45");
        System.out.println("-XX:+ParallelRefProcEnabled");
        System.out.println();
        System.out.println("# Off-Heap");
        System.out.println("-XX:MaxDirectMemorySize=" + offHeapGB + "g");
        System.out.println();
        System.out.println("# Performance");
        System.out.println("-XX:+AlwaysPreTouch");
        System.out.println("-XX:+UseStringDeduplication");
        System.out.println("-XX:+DisableExplicitGC");
        System.out.println();
        System.out.println("# Diagnostics");
        System.out.println("-XX:+HeapDumpOnOutOfMemoryError");
        System.out.println("-XX:HeapDumpPath=./heapdump.hprof");
        System.out.println("-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=10,filesize=100M");
        System.out.println();
        System.out.println("# Ignite XML Configuration Snippet");
        System.out.println("# =================================");
        System.out.println("<bean class=\"org.apache.ignite.configuration.IgniteConfiguration\">");
        System.out.println("  <property name=\"publicThreadPoolSize\" value=\"" + (processors * 2) + "\"/>");
        System.out.println("  <property name=\"systemThreadPoolSize\" value=\"" + processors + "\"/>");
        System.out.println("  <property name=\"dataStorageConfiguration\">");
        System.out.println("    <bean class=\"org.apache.ignite.configuration.DataStorageConfiguration\">");
        System.out.println("      <property name=\"defaultDataRegionConfiguration\">");
        System.out.println("        <bean class=\"org.apache.ignite.configuration.DataRegionConfiguration\">");
        System.out.println("          <property name=\"initialSize\" value=\"#{" + (offHeapGB * 512) + "L * 1024 * 1024}\"/>");
        System.out.println("          <property name=\"maxSize\" value=\"#{" + offHeapGB + "L * 1024 * 1024 * 1024}\"/>");
        System.out.println("          <property name=\"metricsEnabled\" value=\"true\"/>");
        System.out.println("        </bean>");
        System.out.println("      </property>");
        System.out.println("    </bean>");
        System.out.println("  </property>");
        System.out.println("</bean>");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    static class SystemProfile {
        int availableProcessors;
        long maxHeapMemory;
        long totalSystemMemory;
        String javaVersion;
        String osName;
    }

    static class WorkloadProfile {
        double readWriteRatio;
        double operationsPerSecond;
        double avgGetTime;
        double avgPutTime;
        double hitRatio;
        int entryCount;
    }
}
```

### Challenge 3: Capacity Planning Tool

Create `Lab11CapacityPlanner.java`:

```java
package com.example.ignite;

import java.util.Scanner;

/**
 * Capacity planning tool for Apache Ignite deployments.
 */
public class Lab11CapacityPlanner {

    public static void main(String[] args) {
        System.out.println("=== Apache Ignite Capacity Planning Tool ===\n");

        Scanner scanner = new Scanner(System.in);

        try {
            // Gather requirements
            System.out.println("Please provide the following information:\n");

            System.out.print("Expected data volume (GB): ");
            double dataVolumeGB = Double.parseDouble(scanner.nextLine());

            System.out.print("Average entry size (bytes): ");
            int entrySizeBytes = Integer.parseInt(scanner.nextLine());

            System.out.print("Number of backups (0-2): ");
            int backups = Integer.parseInt(scanner.nextLine());

            System.out.print("Expected operations per second: ");
            int expectedOps = Integer.parseInt(scanner.nextLine());

            System.out.print("Read/Write ratio (e.g., 4 for 80% reads): ");
            double readWriteRatio = Double.parseDouble(scanner.nextLine());

            System.out.print("Required availability (99.9, 99.99, etc.): ");
            double availability = Double.parseDouble(scanner.nextLine());

            System.out.print("Enable persistence? (yes/no): ");
            boolean persistence = scanner.nextLine().toLowerCase().startsWith("y");

            // Calculate requirements
            System.out.println("\n" + "=".repeat(60));
            System.out.println("CAPACITY PLANNING RESULTS");
            System.out.println("=".repeat(60) + "\n");

            calculateCapacityPlan(dataVolumeGB, entrySizeBytes, backups,
                expectedOps, readWriteRatio, availability, persistence);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void calculateCapacityPlan(
            double dataVolumeGB, int entrySizeBytes, int backups,
            int expectedOps, double readWriteRatio, double availability,
            boolean persistence) {

        // Calculate entry count
        long totalEntries = (long) (dataVolumeGB * 1024 * 1024 * 1024 / entrySizeBytes);

        System.out.println("INPUT SUMMARY:");
        System.out.println("-".repeat(40));
        System.out.printf("  Data Volume: %.2f GB%n", dataVolumeGB);
        System.out.printf("  Entry Size: %d bytes%n", entrySizeBytes);
        System.out.printf("  Total Entries: %,d%n", totalEntries);
        System.out.printf("  Backups: %d%n", backups);
        System.out.printf("  Expected Ops/sec: %,d%n", expectedOps);
        System.out.printf("  Read/Write Ratio: %.1f:1%n", readWriteRatio);
        System.out.printf("  Availability Target: %.2f%%%n", availability);
        System.out.printf("  Persistence: %s%n", persistence ? "Yes" : "No");

        // Memory calculations
        System.out.println("\nMEMORY REQUIREMENTS:");
        System.out.println("-".repeat(40));

        // Data memory with overhead (Ignite adds ~200 bytes per entry overhead)
        int overheadPerEntry = 200;
        double rawDataGB = dataVolumeGB;
        double overheadGB = (double) totalEntries * overheadPerEntry / (1024 * 1024 * 1024);
        double totalDataGB = rawDataGB + overheadGB;
        double replicatedDataGB = totalDataGB * (1 + backups);

        System.out.printf("  Raw Data: %.2f GB%n", rawDataGB);
        System.out.printf("  Ignite Overhead (~200 bytes/entry): %.2f GB%n", overheadGB);
        System.out.printf("  Total Per Partition: %.2f GB%n", totalDataGB);
        System.out.printf("  With %d Backup(s): %.2f GB%n", backups, replicatedDataGB);

        // Node calculations
        System.out.println("\nCLUSTER SIZING:");
        System.out.println("-".repeat(40));

        // Determine minimum nodes for availability
        int minNodesForAvailability;
        if (availability >= 99.99) {
            minNodesForAvailability = Math.max(5, backups + 2);
        } else if (availability >= 99.9) {
            minNodesForAvailability = Math.max(3, backups + 1);
        } else {
            minNodesForAvailability = Math.max(2, backups + 1);
        }

        System.out.printf("  Minimum Nodes (for %.2f%% availability): %d%n",
            availability, minNodesForAvailability);

        // Calculate memory per node (aim for 70% utilization)
        double memoryPerNodeGB = replicatedDataGB / minNodesForAvailability / 0.7;

        // Round up to common sizes
        int[] commonSizes = {8, 16, 32, 64, 128, 256};
        int recommendedNodeMemory = 8;
        for (int size : commonSizes) {
            if (size >= memoryPerNodeGB) {
                recommendedNodeMemory = size;
                break;
            }
        }

        // Recalculate actual nodes needed
        int actualNodes = (int) Math.ceil(replicatedDataGB / (recommendedNodeMemory * 0.7));
        actualNodes = Math.max(actualNodes, minNodesForAvailability);

        System.out.printf("  Recommended Node Memory: %d GB%n", recommendedNodeMemory);
        System.out.printf("  Recommended Node Count: %d%n", actualNodes);

        // CPU calculations
        System.out.println("\nCPU REQUIREMENTS:");
        System.out.println("-".repeat(40));

        // Estimate based on ops/sec (rough: 10K ops/sec per core)
        int estimatedCoresTotal = Math.max(actualNodes * 2, expectedOps / 10000);
        int coresPerNode = (int) Math.ceil((double) estimatedCoresTotal / actualNodes);
        coresPerNode = Math.max(4, ((coresPerNode + 3) / 4) * 4); // Round to 4

        System.out.printf("  Estimated Total Cores: %d%n", estimatedCoresTotal);
        System.out.printf("  Recommended Cores per Node: %d%n", coresPerNode);

        // Storage calculations (for persistence)
        if (persistence) {
            System.out.println("\nSTORAGE REQUIREMENTS:");
            System.out.println("-".repeat(40));

            double walSpaceGB = replicatedDataGB * 0.5; // WAL typically 50% of data
            double totalStorageGB = replicatedDataGB + walSpaceGB;
            double storagePerNodeGB = totalStorageGB / actualNodes * 1.3; // 30% headroom

            System.out.printf("  Data Storage: %.2f GB%n", replicatedDataGB);
            System.out.printf("  WAL Storage: %.2f GB%n", walSpaceGB);
            System.out.printf("  Total Storage: %.2f GB%n", totalStorageGB);
            System.out.printf("  Storage per Node (with 30%% headroom): %.2f GB%n", storagePerNodeGB);
            System.out.println("  Recommended: SSD or NVMe for best performance");
        }

        // JVM configuration
        System.out.println("\nJVM CONFIGURATION (per node):");
        System.out.println("-".repeat(40));

        int heapGB = (int) (recommendedNodeMemory * 0.4); // 40% for heap
        int offHeapGB = (int) (recommendedNodeMemory * 0.5); // 50% for off-heap

        System.out.printf("  Heap Size: -Xms%dg -Xmx%dg%n", heapGB, heapGB);
        System.out.printf("  Off-Heap: -XX:MaxDirectMemorySize=%dg%n", offHeapGB);
        System.out.println("  GC: -XX:+UseG1GC -XX:MaxGCPauseMillis=200");

        // Final recommendations
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FINAL RECOMMENDATIONS");
        System.out.println("=".repeat(60));

        System.out.println("\nCluster Configuration:");
        System.out.printf("  - %d nodes%n", actualNodes);
        System.out.printf("  - %d GB RAM per node%n", recommendedNodeMemory);
        System.out.printf("  - %d CPU cores per node%n", coresPerNode);
        if (persistence) {
            System.out.printf("  - %.0f GB SSD storage per node%n",
                (replicatedDataGB + replicatedDataGB * 0.5) / actualNodes * 1.3);
        }

        System.out.println("\nNetwork Requirements:");
        System.out.println("  - Minimum 10 Gbps network between nodes");
        System.out.println("  - Low latency (<1ms) recommended");

        System.out.println("\nHigh Availability:");
        System.out.printf("  - %d backup(s) configured%n", backups);
        System.out.printf("  - Can survive %d simultaneous node failure(s)%n", backups);
        System.out.println("  - Consider multi-datacenter for DR");

        System.out.println("\nMonitoring:");
        System.out.println("  - Enable JMX for metrics export");
        System.out.println("  - Set up alerting for:");
        System.out.println("    * Memory usage > 80%");
        System.out.println("    * GC pauses > 500ms");
        System.out.println("    * Operation latency p99 > 100ms");
    }
}
```

