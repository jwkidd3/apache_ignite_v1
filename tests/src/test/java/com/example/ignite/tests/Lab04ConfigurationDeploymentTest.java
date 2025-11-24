package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 4: Configuration and Deployment
 * Coverage: Configuration methods, metrics, monitoring
 */
@DisplayName("Lab 04: Configuration and Deployment Tests")
public class Lab04ConfigurationDeploymentTest extends BaseIgniteTest {

    @Test
    @DisplayName("Test programmatic configuration")
    public void testProgrammaticConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("programmatic-node");

        assertThat(cfg.getIgniteInstanceName()).isEqualTo("programmatic-node");
    }

    @Test
    @DisplayName("Test cache configuration")
    public void testCacheConfiguration() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        CacheConfiguration<Integer, String> actualCfg = cache.getConfiguration(CacheConfiguration.class);

        assertThat(actualCfg.isStatisticsEnabled()).isTrue();
        assertThat(actualCfg.getBackups()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test cache metrics enabled")
    public void testCacheMetrics() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");
        cache.get(1);
        cache.get(999); // miss

        CacheMetrics metrics = cache.metrics();

        assertThat(metrics.getCacheGets()).isGreaterThan(0);
        assertThat(metrics.getCachePuts()).isGreaterThan(0);
        assertThat(metrics.getCacheHits()).isGreaterThan(0);
        assertThat(metrics.getCacheMisses()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test cache hit and miss statistics")
    public void testCacheHitMissStats() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");

        // Hit
        cache.get(1);
        // Miss
        cache.get(999);

        CacheMetrics metrics = cache.metrics();

        assertThat(metrics.getCacheHits()).isEqualTo(1);
        assertThat(metrics.getCacheMisses()).isEqualTo(1);
        assertThat(metrics.getCacheHitPercentage()).isEqualTo(50.0f);
    }

    @Test
    @DisplayName("Test cluster metrics")
    public void testClusterMetrics() {
        ClusterMetrics metrics = ignite.cluster().metrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalCpus()).isGreaterThan(0);
        assertThat(metrics.getTotalNodes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test local node metrics")
    public void testLocalNodeMetrics() {
        ClusterMetrics metrics = ignite.cluster().localNode().metrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getCurrentCpuLoad()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getHeapMemoryUsed()).isGreaterThan(0);
        assertThat(metrics.getHeapMemoryMaximum()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test cache size metrics")
    public void testCacheSizeMetrics() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        for (int i = 0; i < 100; i++) {
            cache.put(i, "value" + i);
        }

        assertThat(cache.size()).isEqualTo(100);
        assertThat(cache.metrics().getCacheSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Test cache timing metrics")
    public void testCacheTimingMetrics() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");
        cache.get(1);

        CacheMetrics metrics = cache.metrics();

        assertThat(metrics.getAverageGetTime()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getAveragePutTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Test multiple cache configurations")
    public void testMultipleCacheConfigs() {
        CacheConfiguration<Integer, String> cfg1 = new CacheConfiguration<>("cache1");
        cfg1.setBackups(1);

        CacheConfiguration<Integer, String> cfg2 = new CacheConfiguration<>("cache2");
        cfg2.setBackups(2);

        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(cfg1);
        IgniteCache<Integer, String> cache2 = ignite.getOrCreateCache(cfg2);

        assertThat(cache1.getConfiguration(CacheConfiguration.class).getBackups()).isEqualTo(1);
        assertThat(cache2.getConfiguration(CacheConfiguration.class).getBackups()).isEqualTo(2);
    }

    @Test
    @DisplayName("Test cache name configuration")
    public void testCacheName() {
        String cacheName = "myCustomCache";
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        assertThat(cache.getName()).isEqualTo(cacheName);
    }

    @Test
    @DisplayName("Test getOrCreateCache idempotency")
    public void testGetOrCreateIdempotency() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());

        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(cfg);
        IgniteCache<Integer, String> cache2 = ignite.getOrCreateCache(cfg);

        cache1.put(1, "value1");

        assertThat(cache2.get(1)).isEqualTo("value1");
        assertThat(cache1.getName()).isEqualTo(cache2.getName());
    }

    @Test
    @DisplayName("Test cache metrics disabled by default")
    public void testMetricsDisabledByDefault() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        // Don't enable statistics

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        assertThat(cache.getConfiguration(CacheConfiguration.class).isStatisticsEnabled()).isFalse();
    }

    @Test
    @DisplayName("Test cluster-wide cache names")
    public void testClusterCacheNames() {
        ignite.getOrCreateCache("cache1");
        ignite.getOrCreateCache("cache2");
        ignite.getOrCreateCache("cache3");

        assertThat(ignite.cacheNames()).contains("cache1", "cache2", "cache3");
    }

    @Test
    @DisplayName("Test node attributes")
    public void testNodeAttributes() {
        assertThat(ignite.cluster().localNode().attributes()).isNotEmpty();
        assertThat(ignite.cluster().localNode().attribute("org.apache.ignite.lang.IgniteProductVersion")).isNotNull();
    }

    @Test
    @DisplayName("Test ignite instance name")
    public void testIgniteInstanceName() {
        assertThat(ignite.name()).isEqualTo(testName);
    }

    @Test
    @DisplayName("Test configuration retrieval")
    public void testConfigRetrieval() {
        IgniteConfiguration cfg = ignite.configuration();

        assertThat(cfg).isNotNull();
        assertThat(cfg.getIgniteInstanceName()).isEqualTo(testName);
    }

    @Test
    @DisplayName("Test cache configuration after creation")
    public void testCacheConfigAfterCreation() {
        CacheConfiguration<Integer, String> originalCfg = new CacheConfiguration<>(getTestCacheName());
        originalCfg.setBackups(2);
        originalCfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(originalCfg);

        CacheConfiguration<Integer, String> retrievedCfg = cache.getConfiguration(CacheConfiguration.class);

        assertThat(retrievedCfg.getBackups()).isEqualTo(2);
        assertThat(retrievedCfg.isStatisticsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test data region metrics")
    public void testDataRegionMetrics() {
        assertThat(ignite.dataRegionMetrics()).isNotEmpty();

        ignite.dataRegionMetrics().forEach(metrics -> {
            assertThat(metrics.getName()).isNotNull();
            assertThat(metrics.getTotalAllocatedSize()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    @DisplayName("Test cache evictions metric")
    public void testCacheEvictionsMetric() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        CacheMetrics metrics = cache.metrics();

        assertThat(metrics.getCacheEvictions()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Test cache removals metric")
    public void testCacheRemovalsMetric() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");
        cache.remove(1);

        CacheMetrics metrics = cache.metrics();

        assertThat(metrics.getCacheRemovals()).isGreaterThan(0);
    }
}
