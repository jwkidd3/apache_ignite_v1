package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 1: Environment Setup
 * Coverage: Node startup, configuration, basic cache operations
 */
@DisplayName("Lab 01: Environment Setup Tests")
public class Lab01EnvironmentSetupTest extends BaseIgniteTest {

    @Test
    @DisplayName("Test Ignite node starts successfully")
    public void testNodeStartup() {
        assertThat(ignite).isNotNull();
        assertThat(ignite.cluster().localNode()).isNotNull();
        assertThat(ignite.cluster().nodes()).hasSize(1);
    }

    @Test
    @DisplayName("Test node has valid configuration")
    public void testNodeConfiguration() {
        IgniteConfiguration cfg = ignite.configuration();

        assertThat(cfg).isNotNull();
        assertThat(cfg.getIgniteInstanceName()).isEqualTo(testName);
        assertThat(cfg.isClientMode()).isFalse();
    }

    @Test
    @DisplayName("Test basic cache creation")
    public void testCacheCreation() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>("testCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo("testCache");
    }

    @Test
    @DisplayName("Test basic put operation")
    public void testPutOperation() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");

        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.get(1)).isEqualTo("value1");
    }

    @Test
    @DisplayName("Test basic get operation")
    public void testGetOperation() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");
        String result = cache.get(1);

        assertThat(result).isEqualTo("value1");
    }

    @Test
    @DisplayName("Test get on non-existent key returns null")
    public void testGetNonExistentKey() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        String result = cache.get(999);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Test basic remove operation")
    public void testRemoveOperation() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");
        boolean removed = cache.remove(1);

        assertThat(removed).isTrue();
        assertThat(cache.get(1)).isNull();
        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("Test cache clear operation")
    public void testClearOperation() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");
        cache.put(2, "value2");
        cache.put(3, "value3");

        cache.clear();

        assertThat(cache.size()).isZero();
    }

    @Test
    @DisplayName("Test cache destroy")
    public void testCacheDestroy() {
        String cacheName = "destroyCache";
        ignite.getOrCreateCache(cacheName);

        assertThat(ignite.cacheNames()).contains(cacheName);

        ignite.destroyCache(cacheName);

        assertThat(ignite.cacheNames()).doesNotContain(cacheName);
    }

    @Test
    @DisplayName("Test cluster metrics are available")
    public void testClusterMetrics() {
        assertThat(ignite.cluster().metrics()).isNotNull();
        assertThat(ignite.cluster().metrics().getTotalCpus()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test local node ID")
    public void testLocalNodeId() {
        assertThat(ignite.cluster().localNode().id()).isNotNull();
        assertThat(ignite.cluster().localNode().consistentId()).isNotNull();
    }

    @Test
    @DisplayName("Test multiple put operations")
    public void testMultiplePuts() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        for (int i = 0; i < 100; i++) {
            cache.put(i, "value" + i);
        }

        assertThat(cache.size()).isEqualTo(100);
        assertThat(cache.get(50)).isEqualTo("value50");
    }

    @Test
    @DisplayName("Test cache replace operation")
    public void testReplaceOperation() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");
        boolean replaced = cache.replace(1, "value2");

        assertThat(replaced).isTrue();
        assertThat(cache.get(1)).isEqualTo("value2");
    }

    @Test
    @DisplayName("Test replace on non-existent key returns false")
    public void testReplaceNonExistent() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        boolean replaced = cache.replace(999, "value");

        assertThat(replaced).isFalse();
    }

    @Test
    @DisplayName("Test putIfAbsent operation")
    public void testPutIfAbsent() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        boolean put1 = cache.putIfAbsent(1, "value1");
        boolean put2 = cache.putIfAbsent(1, "value2");

        assertThat(put1).isTrue();
        assertThat(put2).isFalse();
        assertThat(cache.get(1)).isEqualTo("value1");
    }

    @Test
    @DisplayName("Test cache contains key")
    public void testContainsKey() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");

        assertThat(cache.containsKey(1)).isTrue();
        assertThat(cache.containsKey(999)).isFalse();
    }

    @Test
    @DisplayName("Test peer class loading can be enabled")
    public void testPeerClassLoadingEnabled() {
        // Verify the current configuration has peer class loading disabled (as set in BaseIgniteTest)
        assertThat(ignite.configuration().isPeerClassLoadingEnabled()).isFalse();

        // Create a new configuration with peer class loading enabled
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setPeerClassLoadingEnabled(true);

        // Verify the configuration setting
        assertThat(cfg.isPeerClassLoadingEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test cluster state is active")
    public void testClusterStateActive() {
        // Verify the cluster state is active
        assertThat(ignite.cluster().state().active()).isTrue();
    }
}
