package com.example.ignite.tests;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 3: Basic Cache Operations
 * Coverage: Cache modes, CRUD operations, async operations, batch operations
 */
@DisplayName("Lab 03: Basic Cache Operations Tests")
public class Lab03BasicCacheOperationsTest extends BaseIgniteTest {

    @ParameterizedTest
    @EnumSource(CacheMode.class)
    @DisplayName("Test cache creation with different modes")
    public void testCacheModesCreation(CacheMode mode) {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(mode);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        assertThat(cache).isNotNull();
        assertThat(cache.getConfiguration(CacheConfiguration.class).getCacheMode()).isEqualTo(mode);
    }

    @Test
    @DisplayName("Test partitioned cache mode")
    public void testPartitionedCache() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");

        assertThat(cache.get(1)).isEqualTo("value1");
        assertThat(cache.getConfiguration(CacheConfiguration.class).getBackups()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test replicated cache mode")
    public void testReplicatedCache() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.REPLICATED);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");

        assertThat(cache.get(1)).isEqualTo("value1");
    }

    @Test
    @DisplayName("Test partitioned cache without backups")
    public void testPartitionedCacheNoBackups() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(0);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");

        assertThat(cache.get(1)).isEqualTo("value1");
        assertThat(cache.getConfiguration(CacheConfiguration.class).getBackups()).isEqualTo(0);
    }

    @Test
    @DisplayName("Test atomic cache operations")
    public void testAtomicOperations() {
        CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);
        Integer oldValue = cache.getAndPut(1, 200);

        assertThat(oldValue).isEqualTo(100);
        assertThat(cache.get(1)).isEqualTo(200);
    }

    @Test
    @DisplayName("Test getAndRemove operation")
    public void testGetAndRemove() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");
        String removed = cache.getAndRemove(1);

        assertThat(removed).isEqualTo("value1");
        assertThat(cache.get(1)).isNull();
    }

    @Test
    @DisplayName("Test replace with value check")
    public void testReplaceIfEquals() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");

        boolean replaced1 = cache.replace(1, "value1", "value2");
        boolean replaced2 = cache.replace(1, "wrongValue", "value3");

        assertThat(replaced1).isTrue();
        assertThat(replaced2).isFalse();
        assertThat(cache.get(1)).isEqualTo("value2");
    }

    @Test
    @DisplayName("Test remove with value check")
    public void testRemoveIfEquals() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");

        boolean removed1 = cache.remove(1, "wrongValue");
        boolean removed2 = cache.remove(1, "value1");

        assertThat(removed1).isFalse();
        assertThat(removed2).isTrue();
        assertThat(cache.get(1)).isNull();
    }

    @Test
    @DisplayName("Test async put operation")
    public void testAsyncPut() throws Exception {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());
        IgniteCache<Integer, String> asyncCache = cache.withAsync();

        asyncCache.put(1, "value1");
        IgniteFuture<Void> future = asyncCache.future();

        future.get(5, TimeUnit.SECONDS);

        assertThat(cache.get(1)).isEqualTo("value1");
    }

    @Test
    @DisplayName("Test async get operation")
    public void testAsyncGet() throws Exception {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");

        IgniteCache<Integer, String> asyncCache = cache.withAsync();
        asyncCache.get(1);
        IgniteFuture<String> future = asyncCache.future();

        String result = future.get(5, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("value1");
    }

    @Test
    @DisplayName("Test putAll batch operation")
    public void testPutAll() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        Map<Integer, String> batch = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            batch.put(i, "value" + i);
        }

        cache.putAll(batch);

        assertThat(cache.size()).isEqualTo(100);
        assertThat(cache.get(50)).isEqualTo("value50");
    }

    @Test
    @DisplayName("Test getAll batch operation")
    public void testGetAll() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        for (int i = 0; i < 10; i++) {
            cache.put(i, "value" + i);
        }

        Set<Integer> keys = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            keys.add(i);
        }

        Map<Integer, String> results = cache.getAll(keys);

        assertThat(results).hasSize(10);
        assertThat(results.get(5)).isEqualTo("value5");
    }

    @Test
    @DisplayName("Test removeAll batch operation")
    public void testRemoveAll() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        for (int i = 0; i < 10; i++) {
            cache.put(i, "value" + i);
        }

        Set<Integer> keysToRemove = new HashSet<>();
        keysToRemove.add(1);
        keysToRemove.add(2);
        keysToRemove.add(3);

        cache.removeAll(keysToRemove);

        assertThat(cache.size()).isEqualTo(7);
        assertThat(cache.get(1)).isNull();
        assertThat(cache.get(4)).isEqualTo("value4");
    }

    @Test
    @DisplayName("Test batch operations are faster than individual operations")
    public void testBatchPerformance() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        // Individual operations
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            cache.put(i, "value" + i);
        }
        long individualTime = System.currentTimeMillis() - start1;

        cache.clear();

        // Batch operation
        Map<Integer, String> batch = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            batch.put(i, "value" + i);
        }

        long start2 = System.currentTimeMillis();
        cache.putAll(batch);
        long batchTime = System.currentTimeMillis() - start2;

        assertThat(cache.size()).isEqualTo(100);
        // Batch should be faster (or at least not significantly slower)
        assertThat(batchTime).isLessThanOrEqualTo(individualTime * 2);
    }

    @Test
    @DisplayName("Test cache size operation")
    public void testCacheSize() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        assertThat(cache.size()).isZero();

        cache.put(1, "value1");
        assertThat(cache.size()).isEqualTo(1);

        cache.put(2, "value2");
        assertThat(cache.size()).isEqualTo(2);

        cache.remove(1);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test cache iteration")
    public void testCacheIteration() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        for (int i = 0; i < 10; i++) {
            cache.put(i, "value" + i);
        }

        int count = 0;
        for (javax.cache.Cache.Entry<Integer, String> entry : cache) {
            assertThat(entry.getKey()).isNotNull();
            assertThat(entry.getValue()).isNotNull();
            count++;
        }

        assertThat(count).isEqualTo(10);
    }

    @Test
    @DisplayName("Test getAndPutIfAbsent operation")
    public void testGetAndPutIfAbsent() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        String result1 = cache.getAndPutIfAbsent(1, "value1");
        String result2 = cache.getAndPutIfAbsent(1, "value2");

        assertThat(result1).isNull();
        assertThat(result2).isEqualTo("value1");
        assertThat(cache.get(1)).isEqualTo("value1");
    }

    @Test
    @DisplayName("Test getAndReplace operation")
    public void testGetAndReplace() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");
        String old = cache.getAndReplace(1, "value2");

        assertThat(old).isEqualTo("value1");
        assertThat(cache.get(1)).isEqualTo("value2");
    }

    @Test
    @DisplayName("Test cache withKeepBinary mode")
    public void testKeepBinary() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        cache.put(1, "value1");

        // withKeepBinary should work without deserialization
        IgniteCache<Integer, Object> binaryCache = cache.withKeepBinary();

        assertThat(binaryCache).isNotNull();
        assertThat(binaryCache.get(1)).isNotNull();
    }

    @Test
    @DisplayName("Test localSize operation")
    public void testLocalSize() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(0);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Initially local size should be zero
        assertThat(cache.localSize()).isZero();

        // Add entries
        for (int i = 0; i < 50; i++) {
            cache.put(i, "value" + i);
        }

        // Local size should reflect entries on this node
        // In single-node setup, all entries are local
        assertThat(cache.localSize()).isEqualTo(50);

        // Test localSize with specific peek modes
        assertThat(cache.localSize(org.apache.ignite.cache.CachePeekMode.PRIMARY)).isGreaterThan(0);

        // Remove some entries
        cache.remove(0);
        cache.remove(1);

        assertThat(cache.localSize()).isEqualTo(48);
    }

    @Test
    @DisplayName("Test async operations with future.listen() callback")
    public void testAsyncWithCallback() throws Exception {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        // Prepare test data
        cache.put(1, "initialValue");

        // Use CountDownLatch to wait for async callback completion
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> callbackResult = new AtomicReference<>();

        // Get async cache and perform operation
        IgniteCache<Integer, String> asyncCache = cache.withAsync();
        asyncCache.get(1);
        IgniteFuture<String> future = asyncCache.future();

        // Register listener callback
        future.listen(f -> {
            try {
                callbackResult.set(f.get());
            } finally {
                latch.countDown();
            }
        });

        // Wait for callback to complete
        boolean completed = latch.await(10, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(callbackResult.get()).isEqualTo("initialValue");

        // Test async put with callback
        CountDownLatch putLatch = new CountDownLatch(1);
        AtomicReference<Boolean> putCompleted = new AtomicReference<>(false);

        asyncCache.put(2, "asyncValue");
        IgniteFuture<Void> putFuture = asyncCache.future();

        putFuture.listen(f -> {
            putCompleted.set(true);
            putLatch.countDown();
        });

        boolean putDone = putLatch.await(10, TimeUnit.SECONDS);

        assertThat(putDone).isTrue();
        assertThat(putCompleted.get()).isTrue();
        assertThat(cache.get(2)).isEqualTo("asyncValue");
    }
}
