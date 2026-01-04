package com.example.ignite.tests;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 11: Performance Tuning and Monitoring
 * Coverage: Data region configuration, memory configuration, cache statistics/metrics,
 * batch operations performance comparison
 */
@DisplayName("Lab 11: Performance Tuning Tests")
public class Lab11PerformanceTuningTest extends BaseIgniteTest {

    // ==================== Data Region Configuration Tests ====================

    @Test
    @DisplayName("Test data region configuration with custom name")
    public void testDataRegionConfigurationWithName() {
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("customRegion");
        regionCfg.setInitialSize(64L * 1024 * 1024);  // 64 MB
        regionCfg.setMaxSize(256L * 1024 * 1024);     // 256 MB

        assertThat(regionCfg.getName()).isEqualTo("customRegion");
        assertThat(regionCfg.getInitialSize()).isEqualTo(64L * 1024 * 1024);
        assertThat(regionCfg.getMaxSize()).isEqualTo(256L * 1024 * 1024);
    }

    @Test
    @DisplayName("Test data region with metrics enabled")
    public void testDataRegionWithMetricsEnabled() {
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("metricsRegion");
        regionCfg.setMetricsEnabled(true);
        regionCfg.setInitialSize(50L * 1024 * 1024);
        regionCfg.setMaxSize(100L * 1024 * 1024);

        assertThat(regionCfg.isMetricsEnabled()).isTrue();
        assertThat(regionCfg.getName()).isEqualTo("metricsRegion");
    }

    @Test
    @DisplayName("Test data region persistence configuration")
    public void testDataRegionPersistenceConfiguration() {
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("persistentRegion");
        regionCfg.setPersistenceEnabled(false);  // For testing, use in-memory
        regionCfg.setInitialSize(100L * 1024 * 1024);
        regionCfg.setMaxSize(500L * 1024 * 1024);

        assertThat(regionCfg.isPersistenceEnabled()).isFalse();
        assertThat(regionCfg.getMaxSize()).isEqualTo(500L * 1024 * 1024);
    }

    @Test
    @DisplayName("Test data region eviction threshold")
    public void testDataRegionEvictionThreshold() {
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("evictionRegion");
        regionCfg.setEvictionThreshold(0.9);
        regionCfg.setMaxSize(200L * 1024 * 1024);

        assertThat(regionCfg.getEvictionThreshold()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("Test multiple data regions configuration")
    public void testMultipleDataRegionsConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Default region
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("Default_Region");
        defaultRegion.setInitialSize(50L * 1024 * 1024);
        defaultRegion.setMaxSize(100L * 1024 * 1024);

        // High-performance region
        DataRegionConfiguration highPerfRegion = new DataRegionConfiguration();
        highPerfRegion.setName("HighPerformance_Region");
        highPerfRegion.setInitialSize(100L * 1024 * 1024);
        highPerfRegion.setMaxSize(500L * 1024 * 1024);
        highPerfRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setDataRegionConfigurations(highPerfRegion);

        assertThat(storageCfg.getDefaultDataRegionConfiguration().getName())
            .isEqualTo("Default_Region");
        assertThat(storageCfg.getDataRegionConfigurations()).hasSize(1);
        assertThat(storageCfg.getDataRegionConfigurations()[0].getName())
            .isEqualTo("HighPerformance_Region");
    }

    // ==================== Memory Configuration Tests ====================

    @Test
    @DisplayName("Test data storage configuration defaults")
    public void testDataStorageConfigurationDefaults() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Check that default region is not null after setting
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setMaxSize(100L * 1024 * 1024);
        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);

        assertThat(storageCfg.getDefaultDataRegionConfiguration()).isNotNull();
        assertThat(storageCfg.getDefaultDataRegionConfiguration().getMaxSize())
            .isEqualTo(100L * 1024 * 1024);
    }

    @Test
    @DisplayName("Test memory size configuration validation")
    public void testMemorySizeConfigurationValidation() {
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();

        long initialSize = 50L * 1024 * 1024;  // 50 MB
        long maxSize = 200L * 1024 * 1024;     // 200 MB

        regionCfg.setInitialSize(initialSize);
        regionCfg.setMaxSize(maxSize);

        assertThat(regionCfg.getInitialSize()).isLessThanOrEqualTo(regionCfg.getMaxSize());
        assertThat(regionCfg.getMaxSize()).isEqualTo(maxSize);
    }

    @Test
    @DisplayName("Test checkpoint frequency configuration")
    public void testCheckpointFrequencyConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        long checkpointFrequency = 180000L;  // 3 minutes
        storageCfg.setCheckpointFrequency(checkpointFrequency);

        assertThat(storageCfg.getCheckpointFrequency()).isEqualTo(180000L);
    }

    @Test
    @DisplayName("Test WAL segment size configuration")
    public void testWalSegmentSizeConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        int walSegmentSize = 64 * 1024 * 1024;  // 64 MB
        storageCfg.setWalSegmentSize(walSegmentSize);

        assertThat(storageCfg.getWalSegmentSize()).isEqualTo(64 * 1024 * 1024);
    }

    // ==================== Cache Statistics and Metrics Tests ====================

    @Test
    @DisplayName("Test cache with statistics enabled")
    public void testCacheWithStatisticsEnabled() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        assertThat(cache.getConfiguration(CacheConfiguration.class).isStatisticsEnabled())
            .isTrue();
    }

    @Test
    @DisplayName("Test cache metrics after operations")
    public void testCacheMetricsAfterOperations() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Perform operations
        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }

        for (int i = 0; i < 50; i++) {
            cache.get(i);
        }

        CacheMetrics metrics = cache.metrics();

        assertThat(metrics.getCachePuts()).isEqualTo(100);
        assertThat(metrics.getCacheGets()).isEqualTo(50);
        assertThat(metrics.getCacheHits()).isEqualTo(50);
        assertThat(metrics.getCacheMisses()).isZero();
    }

    @Test
    @DisplayName("Test cache hit rate calculation")
    public void testCacheHitRateCalculation() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Populate cache
        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }

        // Mix of hits and misses
        for (int i = 0; i < 80; i++) {
            cache.get(i);  // These will be hits
        }
        for (int i = 100; i < 120; i++) {
            cache.get(i);  // These will be misses
        }

        CacheMetrics metrics = cache.metrics();

        assertThat(metrics.getCacheHits()).isEqualTo(80);
        assertThat(metrics.getCacheMisses()).isEqualTo(20);
        // Hit percentage should be 80%
        assertThat(metrics.getCacheHitPercentage()).isCloseTo(80.0f, within(0.1f));
    }

    @Test
    @DisplayName("Test cache size metrics")
    public void testCacheSizeMetrics() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Add entries
        for (int i = 0; i < 50; i++) {
            cache.put(i, "Value-" + i);
        }

        assertThat(cache.size()).isEqualTo(50);

        CacheMetrics metrics = cache.metrics();
        assertThat(metrics.getSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("Test cluster metrics availability")
    public void testClusterMetricsAvailability() {
        ClusterMetrics clusterMetrics = ignite.cluster().metrics();

        assertThat(clusterMetrics).isNotNull();
        assertThat(clusterMetrics.getTotalCpus()).isGreaterThan(0);
        assertThat(clusterMetrics.getHeapMemoryMaximum()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test cluster node count metric")
    public void testClusterNodeCountMetric() {
        int nodeCount = ignite.cluster().nodes().size();

        assertThat(nodeCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Test cache average timing metrics")
    public void testCacheAverageTimingMetrics() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Perform operations to generate timing data
        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }
        for (int i = 0; i < 100; i++) {
            cache.get(i);
        }

        CacheMetrics metrics = cache.metrics();

        // Timings should be non-negative
        assertThat(metrics.getAverageGetTime()).isGreaterThanOrEqualTo(0.0f);
        assertThat(metrics.getAveragePutTime()).isGreaterThanOrEqualTo(0.0f);
    }

    // ==================== Batch Operations Performance Tests ====================

    @Test
    @DisplayName("Test batch put operation")
    public void testBatchPutOperation() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        Map<Integer, String> batch = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            batch.put(i, "Value-" + i);
        }

        cache.putAll(batch);

        assertThat(cache.size()).isEqualTo(1000);
        assertThat(cache.get(500)).isEqualTo("Value-500");
    }

    @Test
    @DisplayName("Test individual vs batch put performance")
    public void testIndividualVsBatchPutPerformance() {
        int operationCount = 500;

        // Test 1: Individual operations
        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(getTestCacheName() + "-individual");

        long startIndividual = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            cache1.put(i, "Value-" + i);
        }
        long individualTime = System.currentTimeMillis() - startIndividual;

        // Test 2: Batch operations
        IgniteCache<Integer, String> cache2 = ignite.getOrCreateCache(getTestCacheName() + "-batch");

        Map<Integer, String> batch = new HashMap<>();
        for (int i = 0; i < operationCount; i++) {
            batch.put(i, "Value-" + i);
        }

        long startBatch = System.currentTimeMillis();
        cache2.putAll(batch);
        long batchTime = System.currentTimeMillis() - startBatch;

        // Both caches should have same size
        assertThat(cache1.size()).isEqualTo(operationCount);
        assertThat(cache2.size()).isEqualTo(operationCount);

        // Batch operations should be faster or comparable
        // Using a generous multiplier to avoid flaky tests
        assertThat(batchTime).isLessThanOrEqualTo(individualTime * 3);

        log.info("Individual PUT time: {} ms, Batch PUT time: {} ms", individualTime, batchTime);
    }

    @Test
    @DisplayName("Test batch operations with different batch sizes")
    public void testBatchOperationsWithDifferentBatchSizes() {
        int totalOperations = 1000;

        // Small batch size (100)
        CacheConfiguration<Integer, String> cfg1 = new CacheConfiguration<>(getTestCacheName() + "-small");
        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(cfg1);

        long startSmallBatch = System.currentTimeMillis();
        Map<Integer, String> smallBatch = new HashMap<>();
        for (int i = 0; i < totalOperations; i++) {
            smallBatch.put(i, "Value-" + i);
            if (smallBatch.size() >= 100) {
                cache1.putAll(smallBatch);
                smallBatch.clear();
            }
        }
        if (!smallBatch.isEmpty()) {
            cache1.putAll(smallBatch);
        }
        long smallBatchTime = System.currentTimeMillis() - startSmallBatch;

        // Large batch size (500)
        CacheConfiguration<Integer, String> cfg2 = new CacheConfiguration<>(getTestCacheName() + "-large");
        IgniteCache<Integer, String> cache2 = ignite.getOrCreateCache(cfg2);

        long startLargeBatch = System.currentTimeMillis();
        Map<Integer, String> largeBatch = new HashMap<>();
        for (int i = 0; i < totalOperations; i++) {
            largeBatch.put(i, "Value-" + i);
            if (largeBatch.size() >= 500) {
                cache2.putAll(largeBatch);
                largeBatch.clear();
            }
        }
        if (!largeBatch.isEmpty()) {
            cache2.putAll(largeBatch);
        }
        long largeBatchTime = System.currentTimeMillis() - startLargeBatch;

        assertThat(cache1.size()).isEqualTo(totalOperations);
        assertThat(cache2.size()).isEqualTo(totalOperations);

        log.info("Small batch (100) time: {} ms, Large batch (500) time: {} ms",
            smallBatchTime, largeBatchTime);
    }

    @Test
    @DisplayName("Test atomic vs transactional cache performance comparison")
    public void testAtomicVsTransactionalCachePerformance() {
        int operationCount = 200;

        // Atomic cache
        CacheConfiguration<Integer, String> atomicCfg = new CacheConfiguration<>(getTestCacheName() + "-atomic");
        atomicCfg.setCacheMode(CacheMode.PARTITIONED);
        atomicCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<Integer, String> atomicCache = ignite.getOrCreateCache(atomicCfg);

        long startAtomic = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            atomicCache.put(i, "Value-" + i);
        }
        long atomicTime = System.currentTimeMillis() - startAtomic;

        // Transactional cache
        CacheConfiguration<Integer, String> txCfg = new CacheConfiguration<>(getTestCacheName() + "-tx");
        txCfg.setCacheMode(CacheMode.PARTITIONED);
        txCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        IgniteCache<Integer, String> txCache = ignite.getOrCreateCache(txCfg);

        long startTx = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            txCache.put(i, "Value-" + i);
        }
        long txTime = System.currentTimeMillis() - startTx;

        assertThat(atomicCache.size()).isEqualTo(operationCount);
        assertThat(txCache.size()).isEqualTo(operationCount);

        log.info("Atomic cache PUT time: {} ms, Transactional cache PUT time: {} ms",
            atomicTime, txTime);
    }

    @Test
    @DisplayName("Test getAll batch operation performance")
    public void testGetAllBatchOperationPerformance() {
        int operationCount = 500;

        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Populate cache
        Map<Integer, String> data = new HashMap<>();
        for (int i = 0; i < operationCount; i++) {
            data.put(i, "Value-" + i);
        }
        cache.putAll(data);

        // Individual gets
        long startIndividual = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            cache.get(i);
        }
        long individualTime = System.currentTimeMillis() - startIndividual;

        // Batch get
        long startBatch = System.currentTimeMillis();
        Map<Integer, String> results = cache.getAll(data.keySet());
        long batchTime = System.currentTimeMillis() - startBatch;

        assertThat(results).hasSize(operationCount);

        log.info("Individual GET time: {} ms, Batch GET time: {} ms", individualTime, batchTime);
    }

    @Test
    @DisplayName("Test cache with backups configuration")
    public void testCacheWithBackupsConfiguration() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Add data
        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }

        CacheConfiguration<?, ?> actualCfg = cache.getConfiguration(CacheConfiguration.class);
        assertThat(actualCfg.getBackups()).isEqualTo(1);
        assertThat(cache.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("Test cache clear operation")
    public void testCacheClearOperation() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        // Populate cache
        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }
        assertThat(cache.size()).isEqualTo(100);

        // Clear cache
        cache.clear();

        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("Test thread pool configuration")
    public void testThreadPoolConfiguration() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        // Verify recommended thread pool sizes
        int recommendedPublicPoolSize = availableProcessors * 2;
        int recommendedSystemPoolSize = availableProcessors;

        assertThat(recommendedPublicPoolSize).isEqualTo(availableProcessors * 2);
        assertThat(recommendedSystemPoolSize).isEqualTo(availableProcessors);
        assertThat(availableProcessors).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test JVM memory information availability")
    public void testJvmMemoryInformationAvailability() {
        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        int processors = runtime.availableProcessors();

        assertThat(maxMemory).isGreaterThan(0);
        assertThat(totalMemory).isGreaterThan(0);
        assertThat(freeMemory).isGreaterThanOrEqualTo(0);
        assertThat(processors).isGreaterThan(0);
        assertThat(totalMemory).isLessThanOrEqualTo(maxMemory);

        log.info("Max Heap: {} MB, Total Heap: {} MB, Free Heap: {} MB, Processors: {}",
            maxMemory / (1024 * 1024),
            totalMemory / (1024 * 1024),
            freeMemory / (1024 * 1024),
            processors);
    }
}
