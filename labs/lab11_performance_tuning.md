# Lab 11: Performance Tuning and Monitoring

## Duration: 45 minutes

## Objectives
- Tune JVM for Ignite workloads
- Optimize memory management and garbage collection
- Monitor performance metrics
- Benchmark and load test strategies
- Identify and fix common performance anti-patterns

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

## Verification Steps

### Checklist
- [ ] JVM parameters configured appropriately
- [ ] Metrics collection enabled and working
- [ ] Benchmarks show expected performance
- [ ] GC logs being generated
- [ ] Memory usage within acceptable limits
- [ ] No obvious anti-patterns in code

### Performance Checklist

```
□ Heap size set to 60-70% of available RAM
□ G1GC enabled
□ GC logs configured
□ Off-heap memory configured
□ Thread pools tuned
□ Cache statistics enabled
□ Indexes created for queries
□ Batch operations used where appropriate
□ Affinity keys configured
□ Monitoring tools in place
```

## Lab Questions

1. What garbage collector is recommended for Ignite?
2. How much heap memory should you allocate?
3. What metrics are most important to monitor?
4. What is the most common performance anti-pattern?

## Answers

1. **G1GC (Garbage First Garbage Collector)** is recommended:
   - Good balance of throughput and latency
   - Handles large heaps well
   - Predictable pause times
   - Alternative: ZGC for ultra-low latency (Java 15+)

2. **Heap memory**:
   - 60-70% of available RAM for Ignite
   - Set -Xms equal to -Xmx (avoid resizing)
   - Leave memory for off-heap and OS
   - Example: 32GB RAM → 20GB heap, 10GB off-heap

3. **Critical metrics**:
   - Cache hit ratio (should be high)
   - Average get/put times
   - GC pause times and frequency
   - CPU utilization
   - Memory usage (heap and off-heap)
   - Query execution times

4. Most common: **Not using batch operations**
   - Single operations in loops cause many network round trips
   - Use putAll/getAll/invokeAll instead
   - Can improve throughput by 10-100x

## Common Issues

**Issue: High GC overhead**
- Increase heap size
- Check for memory leaks
- Review object creation patterns
- Consider off-heap storage

**Issue: Poor cache hit ratio**
- Review cache sizing
- Check expiry policies
- Analyze access patterns
- Consider eviction policies

**Issue: Slow queries**
- Add indexes
- Review query plans (EXPLAIN)
- Check for distributed joins
- Optimize data model

## Next Steps

In Lab 12 (final lab), you will:
- Deploy production-ready clusters
- Configure security (SSL/TLS)
- Set up backup and disaster recovery
- Implement rolling updates
- Deploy on Docker/Kubernetes

## Completion

You have completed Lab 11 when you can:
- Configure JVM appropriately for Ignite
- Monitor key performance metrics
- Benchmark different configurations
- Identify and fix performance anti-patterns
- Optimize Ignite for your workload
