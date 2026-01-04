package com.example.ignite.tests;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteEvents;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.cache.Cache;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 8: Advanced Caching Patterns
 * Coverage: Expiry policies, eviction policies, cache entry processors, continuous queries
 */
@DisplayName("Lab 08: Advanced Caching Patterns Tests")
public class Lab08AdvancedCachingTest extends BaseIgniteTest {

    // ==================== EXPIRY POLICY TESTS ====================

    @Test
    @DisplayName("Test CreatedExpiryPolicy configuration")
    public void testCreatedExpiryPolicyConfiguration() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 5)));

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "Test Value");

        // Verify entry exists immediately after creation
        assertThat(cache.get(1)).isEqualTo("Test Value");
    }

    @Test
    @DisplayName("Test CreatedExpiryPolicy expiration")
    public void testCreatedExpiryPolicyExpiration() throws InterruptedException {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 1)));

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "Will expire soon");
        assertThat(cache.get(1)).isEqualTo("Will expire soon");

        // Wait for expiration
        Thread.sleep(1500);

        // Entry should be expired
        assertThat(cache.get(1)).isNull();
    }

    @Test
    @DisplayName("Test ModifiedExpiryPolicy configuration")
    public void testModifiedExpiryPolicyConfiguration() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 3)));

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "Initial value");
        assertThat(cache.get(1)).isEqualTo("Initial value");
    }

    @Test
    @DisplayName("Test ModifiedExpiryPolicy timer reset on update")
    public void testModifiedExpiryPolicyTimerReset() throws InterruptedException {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setExpiryPolicyFactory(ModifiedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 2)));

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "Initial value");
        Thread.sleep(1500);

        // Update the entry - this should reset the timer
        cache.put(1, "Updated value");
        Thread.sleep(1500);

        // Entry should still exist because we updated it
        assertThat(cache.get(1)).isEqualTo("Updated value");
    }

    @Test
    @DisplayName("Test TouchedExpiryPolicy configuration")
    public void testTouchedExpiryPolicyConfiguration() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 4)));

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "Touched expiry value");
        assertThat(cache.get(1)).isEqualTo("Touched expiry value");
    }

    @Test
    @DisplayName("Test per-entry expiry policy using withExpiryPolicy")
    public void testPerEntryExpiryPolicy() throws InterruptedException {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        // Create cache views with different expiry policies
        IgniteCache<Integer, String> cache2Sec = cache.withExpiryPolicy(
                CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 1)).create());

        IgniteCache<Integer, String> cache5Sec = cache.withExpiryPolicy(
                CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 5)).create());

        cache2Sec.put(1, "Expires in 1 second");
        cache5Sec.put(2, "Expires in 5 seconds");

        Thread.sleep(1500);

        // Entry 1 should be expired, entry 2 should still exist
        assertThat(cache.get(1)).isNull();
        assertThat(cache.get(2)).isEqualTo("Expires in 5 seconds");
    }

    @Test
    @DisplayName("Test Duration constants for expiry policies")
    public void testDurationConstants() {
        // Test that Duration constants are correctly configured
        assertThat(Duration.ZERO.getDurationAmount()).isEqualTo(0);
        assertThat(Duration.ONE_MINUTE.getDurationAmount()).isEqualTo(1);
        assertThat(Duration.ONE_MINUTE.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);

        // Test custom duration (FIVE_SECONDS doesn't exist in all versions)
        Duration fiveSeconds = new Duration(TimeUnit.SECONDS, 5);
        assertThat(fiveSeconds.getDurationAmount()).isEqualTo(5);
        assertThat(fiveSeconds.getTimeUnit()).isEqualTo(TimeUnit.SECONDS);
    }

    // ==================== EVICTION POLICY TESTS ====================

    @Test
    @DisplayName("Test LRU eviction policy configuration")
    public void testLruEvictionPolicyConfiguration() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setOnheapCacheEnabled(true);

        LruEvictionPolicy<Integer, String> evictionPolicy = new LruEvictionPolicy<>();
        evictionPolicy.setMaxSize(100);

        cfg.setEvictionPolicyFactory(() -> evictionPolicy);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Add entries
        for (int i = 0; i < 50; i++) {
            cache.put(i, "Value-" + i);
        }

        // Verify entries can be retrieved
        assertThat(cache.get(0)).isEqualTo("Value-0");
        assertThat(cache.get(49)).isEqualTo("Value-49");
    }

    @Test
    @DisplayName("Test eviction policy with max size")
    public void testEvictionPolicyMaxSize() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setOnheapCacheEnabled(true);

        LruEvictionPolicy<Integer, String> evictionPolicy = new LruEvictionPolicy<>();
        evictionPolicy.setMaxSize(50);

        cfg.setEvictionPolicyFactory(() -> evictionPolicy);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Add more entries than max size
        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }

        // All entries should still be in the cache (eviction moves to off-heap, not deletion)
        assertThat(cache.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("Test on-heap cache enabled for eviction")
    public void testOnheapCacheEnabledForEviction() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setOnheapCacheEnabled(true);

        LruEvictionPolicy<Integer, String> evictionPolicy = new LruEvictionPolicy<>();
        evictionPolicy.setMaxSize(10);
        evictionPolicy.setMaxMemorySize(0); // Disable memory-based eviction

        cfg.setEvictionPolicyFactory(() -> evictionPolicy);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        for (int i = 0; i < 20; i++) {
            cache.put(i, "Value-" + i);
        }

        // Cache should contain all entries
        assertThat(cache.size()).isEqualTo(20);
    }

    // ==================== CACHE ENTRY PROCESSOR TESTS ====================

    @Test
    @DisplayName("Test atomic increment with entry processor")
    public void testAtomicIncrementEntryProcessor() {
        CacheConfiguration<String, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put("counter", 0);

        // Invoke entry processor to increment
        Integer result = cache.invoke("counter", new IncrementProcessor(10));

        assertThat(result).isEqualTo(10);
        assertThat(cache.get("counter")).isEqualTo(10);
    }

    @Test
    @DisplayName("Test multiple increments with entry processor")
    public void testMultipleIncrementsEntryProcessor() {
        CacheConfiguration<String, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put("counter", 0);

        // Perform multiple increments
        cache.invoke("counter", new IncrementProcessor(5));
        cache.invoke("counter", new IncrementProcessor(10));
        cache.invoke("counter", new IncrementProcessor(15));

        assertThat(cache.get("counter")).isEqualTo(30);
    }

    @Test
    @DisplayName("Test conditional update with entry processor")
    public void testConditionalUpdateEntryProcessor() {
        CacheConfiguration<String, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put("balance", 100);

        // Successful withdrawal
        Boolean withdrawn = cache.invoke("balance", new WithdrawProcessor(30));
        assertThat(withdrawn).isTrue();
        assertThat(cache.get("balance")).isEqualTo(70);

        // Failed withdrawal (insufficient funds)
        Boolean withdrawn2 = cache.invoke("balance", new WithdrawProcessor(100));
        assertThat(withdrawn2).isFalse();
        assertThat(cache.get("balance")).isEqualTo(70);
    }

    @Test
    @DisplayName("Test entry processor with non-existent entry")
    public void testEntryProcessorNonExistentEntry() {
        CacheConfiguration<String, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

        // Invoke on non-existent entry using CreateIfAbsentProcessor
        Integer result = cache.invoke("newCounter", new CreateIfAbsentProcessor(100));

        assertThat(result).isEqualTo(100);
        assertThat(cache.get("newCounter")).isEqualTo(100);
    }

    @Test
    @DisplayName("Test entry processor returns old value")
    public void testEntryProcessorReturnsOldValue() {
        CacheConfiguration<String, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<String, String> cache = ignite.getOrCreateCache(cfg);

        cache.put("key", "oldValue");

        // Entry processor that returns old value and sets new value
        String oldValue = cache.invoke("key", new ReplaceAndReturnOldProcessor("newValue"));

        assertThat(oldValue).isEqualTo("oldValue");
        assertThat(cache.get("key")).isEqualTo("newValue");
    }

    // ==================== CONTINUOUS QUERY TESTS ====================

    @Test
    @DisplayName("Test continuous query receives updates")
    public void testContinuousQueryReceivesUpdates() throws InterruptedException {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        List<String> receivedEvents = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        ContinuousQuery<Integer, String> query = new ContinuousQuery<>();
        query.setLocalListener(events -> {
            for (var event : events) {
                receivedEvents.add("Key=" + event.getKey() + ",Value=" + event.getValue());
                latch.countDown();
            }
        });

        try (QueryCursor<Cache.Entry<Integer, String>> cursor = cache.query(query)) {
            cache.put(1, "Value1");
            cache.put(2, "Value2");
            cache.put(3, "Value3");

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(receivedEvents).hasSize(3);
        }
    }

    @Test
    @DisplayName("Test continuous query with remote filter")
    public void testContinuousQueryWithRemoteFilter() throws InterruptedException {
        CacheConfiguration<Integer, Double> cfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, Double> cache = ignite.getOrCreateCache(cfg);

        AtomicInteger eventCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        ContinuousQuery<Integer, Double> query = new ContinuousQuery<>();
        query.setLocalListener(events -> {
            for (var event : events) {
                eventCount.incrementAndGet();
                latch.countDown();
            }
        });

        // Filter: Only notify for values > 100
        query.setRemoteFilterFactory(() -> event -> event.getValue() > 100.0);

        try (QueryCursor<Cache.Entry<Integer, Double>> cursor = cache.query(query)) {
            cache.put(1, 50.0);   // Below threshold - no notification
            cache.put(2, 150.0);  // Above threshold - notification
            cache.put(3, 75.0);   // Below threshold - no notification
            cache.put(4, 200.0);  // Above threshold - notification

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(eventCount.get()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Test continuous query receives update events")
    public void testContinuousQueryUpdateEvents() throws InterruptedException {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        List<String> oldValues = Collections.synchronizedList(new ArrayList<>());
        List<String> newValues = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(2);

        ContinuousQuery<Integer, String> query = new ContinuousQuery<>();
        query.setLocalListener(events -> {
            for (var event : events) {
                if (event.getOldValue() != null) {
                    oldValues.add(event.getOldValue());
                }
                newValues.add(event.getValue());
                latch.countDown();
            }
        });

        try (QueryCursor<Cache.Entry<Integer, String>> cursor = cache.query(query)) {
            cache.put(1, "Initial");
            Thread.sleep(100);
            cache.put(1, "Updated");

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(newValues).contains("Initial", "Updated");
        }
    }

    @Test
    @DisplayName("Test continuous query with initial query")
    public void testContinuousQueryWithInitialQuery() throws InterruptedException {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Pre-populate cache
        cache.put(1, "Existing1");
        cache.put(2, "Existing2");

        List<String> existingEntries = new ArrayList<>();
        AtomicInteger updateCount = new AtomicInteger(0);

        ContinuousQuery<Integer, String> query = new ContinuousQuery<>();
        query.setLocalListener(events -> {
            for (var event : events) {
                updateCount.incrementAndGet();
            }
        });

        // Set initial query to get existing entries
        query.setInitialQuery(new org.apache.ignite.cache.query.ScanQuery<>());

        try (QueryCursor<Cache.Entry<Integer, String>> cursor = cache.query(query)) {
            // Collect initial results
            for (Cache.Entry<Integer, String> entry : cursor) {
                existingEntries.add(entry.getValue());
            }

            assertThat(existingEntries).hasSize(2);
            assertThat(existingEntries).contains("Existing1", "Existing2");
        }
    }

    @Test
    @DisplayName("Test continuous query handles remove events")
    public void testContinuousQueryRemoveEvents() throws InterruptedException {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        List<String> eventTypes = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(2);

        ContinuousQuery<Integer, String> query = new ContinuousQuery<>();
        query.setLocalListener(events -> {
            for (var event : events) {
                eventTypes.add(event.getEventType().name());
                latch.countDown();
            }
        });

        try (QueryCursor<Cache.Entry<Integer, String>> cursor = cache.query(query)) {
            cache.put(1, "ToBeRemoved");
            Thread.sleep(100);
            cache.remove(1);

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(eventTypes).contains("CREATED", "REMOVED");
        }
    }

    // ==================== NEAR CACHE TESTS ====================

    @Test
    @DisplayName("Test near cache configuration")
    public void testNearCacheConfiguration() {
        String cacheName = getTestCacheName() + "-near-config";

        NearCacheConfiguration<Integer, String> nearCfg = new NearCacheConfiguration<>();
        nearCfg.setNearEvictionPolicyFactory(() -> {
            LruEvictionPolicy<Integer, String> policy = new LruEvictionPolicy<>();
            policy.setMaxSize(100);
            return policy;
        });

        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);
        cfg.setNearConfiguration(nearCfg);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "Test Value");
        assertThat(cache.get(1)).isEqualTo("Test Value");
    }

    @Test
    @DisplayName("Test near cache stores frequently accessed data")
    public void testNearCacheFrequentAccess() {
        String cacheName = getTestCacheName() + "-near-frequent";

        NearCacheConfiguration<Integer, String> nearCfg = new NearCacheConfiguration<>();
        nearCfg.setNearEvictionPolicyFactory(() -> {
            LruEvictionPolicy<Integer, String> policy = new LruEvictionPolicy<>();
            policy.setMaxSize(50);
            return policy;
        });

        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setNearConfiguration(nearCfg);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Populate cache
        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }

        // Access some entries multiple times
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 20; i++) {
                cache.get(i);
            }
        }

        // Verify entries are accessible
        assertThat(cache.get(0)).isEqualTo("Value-0");
        assertThat(cache.get(19)).isEqualTo("Value-19");
    }

    // ==================== INVOKE ALL TESTS ====================

    @Test
    @DisplayName("Test invokeAll for batch entry processing")
    public void testInvokeAllBatchProcessor() {
        CacheConfiguration<String, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

        // Initialize multiple counters
        cache.put("counter1", 10);
        cache.put("counter2", 20);
        cache.put("counter3", 30);
        cache.put("counter4", 40);
        cache.put("counter5", 50);

        // Create set of keys to process
        Set<String> keys = new HashSet<>();
        keys.add("counter1");
        keys.add("counter2");
        keys.add("counter3");
        keys.add("counter4");
        keys.add("counter5");

        // Invoke batch increment on all keys
        Map<String, javax.cache.processor.EntryProcessorResult<Integer>> results =
                cache.invokeAll(keys, new IncrementProcessor(100));

        // Verify all results
        assertThat(results).hasSize(5);
        assertThat(results.get("counter1").get()).isEqualTo(110);  // 10 + 100
        assertThat(results.get("counter2").get()).isEqualTo(120);  // 20 + 100
        assertThat(results.get("counter3").get()).isEqualTo(130);  // 30 + 100
        assertThat(results.get("counter4").get()).isEqualTo(140);  // 40 + 100
        assertThat(results.get("counter5").get()).isEqualTo(150);  // 50 + 100

        // Verify cache values are updated
        assertThat(cache.get("counter1")).isEqualTo(110);
        assertThat(cache.get("counter2")).isEqualTo(120);
        assertThat(cache.get("counter3")).isEqualTo(130);
        assertThat(cache.get("counter4")).isEqualTo(140);
        assertThat(cache.get("counter5")).isEqualTo(150);
    }

    // ==================== CACHE EVENTS TESTS ====================

    @Test
    @DisplayName("Test enabling cache events with ignite.events().enableLocal()")
    public void testCacheEventsEnabled() {
        // Get the events API
        IgniteEvents events = ignite.events();
        assertThat(events).isNotNull();

        // Enable local cache events
        events.enableLocal(
                EventType.EVT_CACHE_OBJECT_PUT,
                EventType.EVT_CACHE_OBJECT_READ,
                EventType.EVT_CACHE_OBJECT_REMOVED
        );

        // Create a cache
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Perform cache operations - events are now enabled
        cache.put(1, "test");
        cache.get(1);
        cache.remove(1);

        // Verify events can be queried (local events only in single-node setup)
        // The fact that no exception is thrown confirms events are enabled
        assertThat(cache.containsKey(1)).isFalse();
    }

    @Test
    @DisplayName("Test cache event listener registration and event capture")
    public void testCacheEventListener() throws InterruptedException {
        // Create a cache with events enabled
        String cacheName = getTestCacheName();
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Enable local events
        ignite.events().enableLocal(
                EventType.EVT_CACHE_OBJECT_PUT,
                EventType.EVT_CACHE_OBJECT_READ,
                EventType.EVT_CACHE_OBJECT_REMOVED
        );

        List<String> capturedEvents = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(3);

        // Create and register event listener that filters by cache name
        IgnitePredicate<CacheEvent> listener = event -> {
            // Only process events from our specific cache
            if (cacheName.equals(event.cacheName())) {
                String eventInfo = "EventType=" + event.type() + ",Key=" + event.key();
                capturedEvents.add(eventInfo);
                latch.countDown();
            }
            return true; // Continue listening
        };

        // Register listener for cache events
        ignite.events().localListen(listener,
                EventType.EVT_CACHE_OBJECT_PUT,
                EventType.EVT_CACHE_OBJECT_READ,
                EventType.EVT_CACHE_OBJECT_REMOVED);

        try {
            // Perform cache operations that trigger events
            cache.put(1, "Value1");  // PUT event
            cache.get(1);            // READ event
            cache.remove(1);         // REMOVED event

            // Wait for events to be captured
            boolean completed = latch.await(5, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(capturedEvents).hasSize(3);

            // Verify event types were captured
            assertThat(capturedEvents).anyMatch(e -> e.contains("EventType=" + EventType.EVT_CACHE_OBJECT_PUT));
            assertThat(capturedEvents).anyMatch(e -> e.contains("EventType=" + EventType.EVT_CACHE_OBJECT_READ));
            assertThat(capturedEvents).anyMatch(e -> e.contains("EventType=" + EventType.EVT_CACHE_OBJECT_REMOVED));
        } finally {
            // Unregister listener
            ignite.events().stopLocalListen(listener);
        }
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Entry processor to increment a value atomically
     */
    static class IncrementProcessor implements CacheEntryProcessor<String, Integer, Integer>, Serializable {
        private final int increment;

        public IncrementProcessor(int increment) {
            this.increment = increment;
        }

        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            Integer current = entry.getValue();
            Integer newValue = (current != null ? current : 0) + increment;
            entry.setValue(newValue);
            return newValue;
        }
    }

    /**
     * Entry processor for conditional withdrawal
     */
    static class WithdrawProcessor implements CacheEntryProcessor<String, Integer, Boolean>, Serializable {
        private final int amount;

        public WithdrawProcessor(int amount) {
            this.amount = amount;
        }

        @Override
        public Boolean process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            Integer balance = entry.getValue();

            if (balance != null && balance >= amount) {
                entry.setValue(balance - amount);
                return true;
            }
            return false;
        }
    }

    /**
     * Entry processor that creates entry if absent
     */
    static class CreateIfAbsentProcessor implements CacheEntryProcessor<String, Integer, Integer>, Serializable {
        private final int defaultValue;

        public CreateIfAbsentProcessor(int defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            if (!entry.exists()) {
                entry.setValue(defaultValue);
                return defaultValue;
            }
            return entry.getValue();
        }
    }

    /**
     * Entry processor that replaces value and returns old value
     */
    static class ReplaceAndReturnOldProcessor implements CacheEntryProcessor<String, String, String>, Serializable {
        private final String newValue;

        public ReplaceAndReturnOldProcessor(String newValue) {
            this.newValue = newValue;
        }

        @Override
        public String process(MutableEntry<String, String> entry, Object... args)
                throws EntryProcessorException {
            String oldValue = entry.getValue();
            entry.setValue(newValue);
            return oldValue;
        }
    }
}
