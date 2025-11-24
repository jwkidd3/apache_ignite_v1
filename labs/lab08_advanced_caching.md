# Lab 8: Advanced Caching Patterns

## Duration: 60 minutes

## Objectives
- Implement cache-aside, write-through, and write-behind patterns
- Configure and use near caches for client-side caching
- Set up expiry policies and eviction strategies
- Use cache entry processors for atomic operations
- Implement event handling and continuous queries

## Prerequisites
- Completed Labs 1-7
- Understanding of basic cache operations
- Familiarity with event-driven programming

## Part 1: Caching Patterns Review (Already covered in Lab 5, extended here)

### Exercise 1: Near Cache Configuration

Create `Lab08NearCache.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;

public class Lab08NearCache {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Lab08NearCache <client|server>");
            System.exit(1);
        }

        boolean isClient = args[0].equals("client");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(isClient ? "client-node" : "server-node");
        cfg.setClientMode(isClient);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Near Cache Lab ===");
            System.out.println("Mode: " + (isClient ? "CLIENT" : "SERVER") + "\n");

            if (!isClient) {
                // Server node: Create cache
                CacheConfiguration<Integer, String> cfg1 =
                    new CacheConfiguration<>("dataCache");
                cfg1.setCacheMode(CacheMode.PARTITIONED);
                cfg1.setBackups(1);

                IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg1);

                // Populate data
                System.out.println("Server: Populating cache with 1000 entries...");
                for (int i = 0; i < 1000; i++) {
                    cache.put(i, "Value-" + i);
                }
                System.out.println("Server: Data loaded\n");

            } else {
                // Client node: Create near cache
                NearCacheConfiguration<Integer, String> nearCfg =
                    new NearCacheConfiguration<>();
                nearCfg.setNearEvictionPolicyFactory(
                    () -> new org.apache.ignite.cache.eviction.lru.LruEvictionPolicy<>(100));

                IgniteCache<Integer, String> cache =
                    ignite.getOrCreateNearCache("dataCache", nearCfg);

                System.out.println("Client: Near cache created (max 100 entries)\n");

                // First access - from server
                System.out.println("=== First Access (from server) ===");
                long start = System.currentTimeMillis();
                String value1 = cache.get(100);
                long time1 = System.currentTimeMillis() - start;
                System.out.println("Value: " + value1);
                System.out.println("Time: " + time1 + " ms\n");

                // Second access - from near cache
                System.out.println("=== Second Access (from near cache) ===");
                start = System.currentTimeMillis();
                String value2 = cache.get(100);
                long time2 = System.currentTimeMillis() - start;
                System.out.println("Value: " + value2);
                System.out.println("Time: " + time2 + " ms");
                System.out.println("Speedup: " + (float)time1/time2 + "x faster\n");

                // Access multiple entries
                System.out.println("=== Accessing Multiple Entries ===");
                for (int i = 0; i < 50; i++) {
                    cache.get(i);
                }
                System.out.println("50 entries now in near cache\n");

                // Check near cache size
                System.out.println("Near cache size: " + cache.localSize());

                System.out.println("\n=== Near Cache Benefits ===");
                System.out.println("- Reduced network latency");
                System.out.println("- Lower load on server nodes");
                System.out.println("- Better performance for frequently accessed data");
                System.out.println("- Automatic invalidation on updates");
            }

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Expiry Policies and Eviction (15 minutes)

### Exercise 2: Configure Expiry Policies

Create `Lab08ExpiryPolicies.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import java.util.concurrent.TimeUnit;

public class Lab08ExpiryPolicies {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Expiry Policies Lab ===\n");

            // Cache with Created Expiry Policy
            System.out.println("=== 1. Created Expiry Policy ===");
            System.out.println("Entry expires 5 seconds after creation\n");

            CacheConfiguration<Integer, String> createdCfg =
                new CacheConfiguration<>("createdExpiryCache");
            createdCfg.setCacheMode(CacheMode.PARTITIONED);
            createdCfg.setExpiryPolicyFactory(
                CreatedExpiryPolicy.factoryOf(Duration.FIVE_SECONDS));

            IgniteCache<Integer, String> createdCache =
                ignite.getOrCreateCache(createdCfg);

            createdCache.put(1, "Will expire in 5 seconds");
            System.out.println("Entry created: " + createdCache.get(1));
            System.out.println("Waiting 6 seconds...");
            Thread.sleep(6000);
            System.out.println("After expiry: " + createdCache.get(1) + " (null expected)\n");

            // Cache with Modified Expiry Policy
            System.out.println("=== 2. Modified Expiry Policy ===");
            System.out.println("Entry expires 3 seconds after last update\n");

            CacheConfiguration<Integer, String> modifiedCfg =
                new CacheConfiguration<>("modifiedExpiryCache");
            modifiedCfg.setCacheMode(CacheMode.PARTITIONED);
            modifiedCfg.setExpiryPolicyFactory(
                ModifiedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 3)));

            IgniteCache<Integer, String> modifiedCache =
                ignite.getOrCreateCache(modifiedCfg);

            modifiedCache.put(1, "Initial value");
            System.out.println("Entry created");
            Thread.sleep(2000);

            modifiedCache.put(1, "Updated value");
            System.out.println("Entry updated (timer reset)");
            Thread.sleep(2000);

            System.out.println("After 2 seconds: " + modifiedCache.get(1));
            Thread.sleep(2000);

            System.out.println("After 4 seconds: " + modifiedCache.get(1) + " (null expected)\n");

            // Cache with Touched Expiry Policy
            System.out.println("=== 3. Touched Expiry Policy ===");
            System.out.println("Entry expires 4 seconds after last access\n");

            CacheConfiguration<Integer, String> touchedCfg =
                new CacheConfiguration<>("touchedExpiryCache");
            touchedCfg.setCacheMode(CacheMode.PARTITIONED);
            touchedCfg.setExpiryPolicyFactory(
                TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 4)));

            IgniteCache<Integer, String> touchedCache =
                ignite.getOrCreateCache(touchedCfg);

            touchedCache.put(1, "Touched expiry value");
            System.out.println("Entry created");

            for (int i = 0; i < 3; i++) {
                Thread.sleep(2000);
                String value = touchedCache.get(1);
                System.out.println("After " + ((i+1)*2) + " seconds: " + value + " (read resets timer)");
            }

            Thread.sleep(5000);
            System.out.println("After no access for 5 seconds: " +
                touchedCache.get(1) + " (null expected)\n");

            // Per-entry expiry policy
            System.out.println("=== 4. Per-Entry Expiry Policy ===");
            IgniteCache<Integer, String> dynamicCache =
                ignite.getOrCreateCache("dynamicExpiryCache");

            IgniteCache<Integer, String> cache2Sec =
                dynamicCache.withExpiryPolicy(
                    CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 2)).create());

            IgniteCache<Integer, String> cache5Sec =
                dynamicCache.withExpiryPolicy(
                    CreatedExpiryPolicy.factoryOf(Duration.FIVE_SECONDS).create());

            cache2Sec.put(1, "Expires in 2 seconds");
            cache5Sec.put(2, "Expires in 5 seconds");

            Thread.sleep(3000);

            System.out.println("Entry 1 (2sec TTL): " + dynamicCache.get(1) + " (null)");
            System.out.println("Entry 2 (5sec TTL): " + dynamicCache.get(2) + " (still there)");

            System.out.println("\n=== Expiry Policy Use Cases ===");
            System.out.println("- CreatedExpiryPolicy: Session data, temporary tokens");
            System.out.println("- ModifiedExpiryPolicy: Frequently updated data");
            System.out.println("- TouchedExpiryPolicy: Recently accessed data (LRU-like)");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 3: Eviction Policies

Create `Lab08Eviction.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.CacheConfiguration;

public class Lab08Eviction {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Eviction Policies Lab ===\n");

            // LRU Eviction Policy
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("evictionCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setOnheapCacheEnabled(true);

            LruEvictionPolicy<Integer, String> evictionPolicy = new LruEvictionPolicy<>();
            evictionPolicy.setMaxSize(100);  // Keep only 100 entries in heap

            cfg.setEvictionPolicyFactory(() -> evictionPolicy);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            System.out.println("Cache created with LRU eviction (max 100 entries on-heap)");
            System.out.println("Adding 200 entries...\n");

            // Add 200 entries
            for (int i = 0; i < 200; i++) {
                cache.put(i, "Value-" + i);
            }

            System.out.println("Total cache size: " + cache.size());
            System.out.println("On-heap size: " + cache.localSize());
            System.out.println("(Others moved to off-heap or disk)\n");

            // Access some entries to make them "recent"
            System.out.println("Accessing entries 0-9 to make them recent...");
            for (int i = 0; i < 10; i++) {
                cache.get(i);
            }

            System.out.println("\n=== Eviction Strategy ===");
            System.out.println("LRU: Least Recently Used entries evicted first");
            System.out.println("Keeps frequently accessed data in memory");
            System.out.println("Balances memory usage and performance");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 3: Cache Entry Processors (15 minutes)

### Exercise 4: Atomic Entry Processors

Create `Lab08EntryProcessors.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.HashMap;
import java.util.Map;

public class Lab08EntryProcessors {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Entry Processors Lab ===\n");

            CacheConfiguration<String, Integer> cfg =
                new CacheConfiguration<>("processorCache");
            cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

            // Initialize counters
            cache.put("counter1", 0);
            cache.put("counter2", 0);
            cache.put("counter3", 0);

            System.out.println("=== Scenario 1: Atomic Increment ===");

            // Without entry processor (non-atomic)
            System.out.println("\n1. Without Entry Processor (3 operations):");
            long start = System.currentTimeMillis();

            Integer value = cache.get("counter1");
            value = value + 10;
            cache.put("counter1", value);

            long withoutEP = System.currentTimeMillis() - start;
            System.out.println("   Result: " + cache.get("counter1"));
            System.out.println("   Time: " + withoutEP + " ms");
            System.out.println("   Network round trips: 2 (get + put)");

            // With entry processor (atomic)
            System.out.println("\n2. With Entry Processor (1 operation):");
            start = System.currentTimeMillis();

            cache.invoke("counter2", new IncrementProcessor(10));

            long withEP = System.currentTimeMillis() - start;
            System.out.println("   Result: " + cache.get("counter2"));
            System.out.println("   Time: " + withEP + " ms");
            System.out.println("   Network round trips: 1 (invoke)");
            System.out.println("   Atomic: Yes");

            System.out.println("\n=== Scenario 2: Conditional Update ===");

            cache.put("balance", 100);

            // Conditional update using entry processor
            boolean withdrawn = cache.invoke("balance",
                new WithdrawProcessor(30));

            System.out.println("Withdraw $30: " + (withdrawn ? "Success" : "Failed"));
            System.out.println("New balance: $" + cache.get("balance"));

            // Try to withdraw more than available
            boolean withdrawn2 = cache.invoke("balance",
                new WithdrawProcessor(100));

            System.out.println("\nWithdraw $100: " + (withdrawn2 ? "Success" : "Failed"));
            System.out.println("Balance: $" + cache.get("balance"));

            System.out.println("\n=== Scenario 3: Batch Processing ===");

            Map<String, Integer> counters = new HashMap<>();
            counters.put("metric1", 0);
            counters.put("metric2", 0);
            counters.put("metric3", 0);
            cache.putAll(counters);

            // Increment all counters atomically
            Map<String, IncrementProcessor> processors = new HashMap<>();
            processors.put("metric1", new IncrementProcessor(5));
            processors.put("metric2", new IncrementProcessor(10));
            processors.put("metric3", new IncrementProcessor(15));

            cache.invokeAll(processors.keySet(),
                (entry, arguments) -> {
                    Integer current = entry.getValue();
                    entry.setValue(current + ((IncrementProcessor)arguments[0]).increment);
                    return null;
                }, processors.values().toArray());

            System.out.println("Batch increment results:");
            System.out.println("  metric1: " + cache.get("metric1"));
            System.out.println("  metric2: " + cache.get("metric2"));
            System.out.println("  metric3: " + cache.get("metric3"));

            System.out.println("\n=== Entry Processor Benefits ===");
            System.out.println("✓ Atomic operations");
            System.out.println("✓ Reduced network overhead");
            System.out.println("✓ Server-side processing");
            System.out.println("✓ Better performance for read-modify-write");
            System.out.println("✓ Avoid race conditions");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Entry processor to increment a value
    static class IncrementProcessor implements CacheEntryProcessor<String, Integer, Integer> {
        private int increment;

        public IncrementProcessor(int increment) {
            this.increment = increment;
        }

        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            Integer current = entry.getValue();
            Integer newValue = current + increment;
            entry.setValue(newValue);
            return newValue;
        }
    }

    // Entry processor for conditional update
    static class WithdrawProcessor implements CacheEntryProcessor<String, Integer, Boolean> {
        private int amount;

        public WithdrawProcessor(int amount) {
            this.amount = amount;
        }

        @Override
        public Boolean process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            Integer balance = entry.getValue();

            if (balance >= amount) {
                entry.setValue(balance - amount);
                return true;
            }
            return false;
        }
    }
}
```

## Part 4: Event Handling and Continuous Queries (20 minutes)

### Exercise 5: Cache Events

Create `Lab08CacheEvents.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;

public class Lab08CacheEvents {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Events Lab ===\n");

            // Enable cache events
            ignite.events().enableLocal(
                EventType.EVT_CACHE_OBJECT_PUT,
                EventType.EVT_CACHE_OBJECT_REMOVED,
                EventType.EVT_CACHE_OBJECT_READ
            );

            // Register event listener
            IgnitePredicate<CacheEvent> listener = evt -> {
                System.out.println("\n[EVENT] Type: " + evt.name());
                System.out.println("  Cache: " + evt.cacheName());
                System.out.println("  Key: " + evt.key());
                System.out.println("  Value: " + evt.newValue());
                return true;  // Continue listening
            };

            ignite.events().localListen(listener,
                EventType.EVT_CACHE_OBJECT_PUT,
                EventType.EVT_CACHE_OBJECT_REMOVED,
                EventType.EVT_CACHE_OBJECT_READ);

            System.out.println("Event listener registered\n");

            // Create cache and perform operations
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("eventCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            System.out.println("=== Performing Cache Operations ===");
            System.out.println("(Watch for events below)\n");

            // PUT event
            System.out.println("1. Putting value...");
            cache.put(1, "Hello");
            Thread.sleep(100);

            // READ event
            System.out.println("\n2. Reading value...");
            cache.get(1);
            Thread.sleep(100);

            // UPDATE (PUT) event
            System.out.println("\n3. Updating value...");
            cache.put(1, "Hello Updated");
            Thread.sleep(100);

            // REMOVE event
            System.out.println("\n4. Removing value...");
            cache.remove(1);
            Thread.sleep(100);

            System.out.println("\n=== Event Use Cases ===");
            System.out.println("- Audit logging");
            System.out.println("- Cache statistics");
            System.out.println("- Triggering workflows");
            System.out.println("- Replication to other systems");
            System.out.println("- Monitoring and alerting");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

            // Cleanup
            ignite.events().stopLocalListen(listener);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 6: Continuous Queries

Create `Lab08ContinuousQueries.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;

public class Lab08ContinuousQueries {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Continuous Queries Lab ===\n");

            CacheConfiguration<Integer, Double> cfg =
                new CacheConfiguration<>("stockPrices");

            IgniteCache<Integer, Double> cache = ignite.getOrCreateCache(cfg);

            // Create continuous query for high-value stocks
            ContinuousQuery<Integer, Double> highValueQuery =
                new ContinuousQuery<>();

            highValueQuery.setLocalListener(new CacheEntryUpdatedListener<Integer, Double>() {
                @Override
                public void onUpdated(Iterable<CacheEntryEvent<? extends Integer, ? extends Double>> events)
                        throws CacheEntryListenerException {
                    for (CacheEntryEvent<? extends Integer, ? extends Double> e : events) {
                        System.out.println("[ALERT] Stock " + e.getKey() +
                            " price changed: $" + e.getOldValue() +
                            " -> $" + e.getValue());
                    }
                }
            });

            // Filter: Only notify for stocks > $100
            highValueQuery.setRemoteFilterFactory(() ->
                evt -> evt.getValue() > 100.0);

            System.out.println("Starting continuous query (stocks > $100)...\n");

            try (QueryCursor<javax.cache.Cache.Entry<Integer, Double>> cursor =
                     cache.query(highValueQuery)) {

                // Simulate stock price updates
                System.out.println("=== Simulating Stock Price Updates ===\n");

                System.out.println("1. Setting initial prices...");
                cache.put(1, 50.0);   // Stock 1: $50 (below threshold, no alert)
                cache.put(2, 150.0);  // Stock 2: $150 (above threshold, alert)
                Thread.sleep(500);

                System.out.println("\n2. Updating prices...");
                cache.put(1, 120.0);  // Stock 1: $50 -> $120 (crosses threshold, alert)
                Thread.sleep(500);

                cache.put(2, 160.0);  // Stock 2: $150 -> $160 (stays above, alert)
                Thread.sleep(500);

                System.out.println("\n3. Price drops...");
                cache.put(1, 80.0);   // Stock 1: $120 -> $80 (drops below, no alert)
                Thread.sleep(500);

                System.out.println("\n=== Continuous Query Benefits ===");
                System.out.println("- Real-time notifications");
                System.out.println("- Server-side filtering");
                System.out.println("- Low latency");
                System.out.println("- Reduced network traffic");
                System.out.println("- Event-driven architecture");

                System.out.println("\n=== Use Cases ===");
                System.out.println("- Real-time dashboards");
                System.out.println("- Alert systems");
                System.out.println("- Cache invalidation");
                System.out.println("- Data synchronization");
                System.out.println("- Streaming analytics");

                System.out.println("\nPress Enter to exit...");
                System.in.read();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Verification Steps

### Checklist
- [ ] Near cache reduces latency for client nodes
- [ ] Expiry policies automatically remove old entries
- [ ] Eviction policies manage on-heap memory
- [ ] Entry processors perform atomic operations
- [ ] Cache events captured correctly
- [ ] Continuous queries deliver real-time notifications

## Lab Questions

1. When should you use near caches?
2. What's the difference between eviction and expiry?
3. Why use entry processors instead of get-update-put?
4. When are continuous queries appropriate?

## Answers

1. Use **near caches** for:
   - Client nodes frequently accessing same data
   - Read-heavy workloads
   - Tolerable eventual consistency
   - Need to reduce network latency

2. **Eviction**: Removes entries from on-heap to off-heap/disk when memory is full (data still in cache). **Expiry**: Completely removes entries after TTL expires (data gone).

3. **Entry processors**:
   - Atomic operation (no race conditions)
   - Single network call (better performance)
   - Server-side processing (less data transfer)
   - Guaranteed consistency

4. **Continuous queries** for:
   - Real-time notifications needed
   - Event-driven architecture
   - Low-latency requirements
   - Monitoring specific conditions
   - Note: Has overhead, use selectively

## Common Issues

**Issue: Near cache not improving performance**
- Verify client mode enabled
- Check data access patterns (must be frequent)
- Ensure near cache size appropriate

**Issue: Entries not expiring**
- Verify expiry policy configured
- Check system time synchronization
- Ensure background cleanup running

**Issue: Entry processor fails**
- Must be serializable
- Check for exceptions in process()
- Verify cache atomicity mode

## Next Steps

In Lab 9, you will:
- Implement distributed computing with compute grid
- Create compute closures and jobs
- Use affinity-aware computing
- Implement MapReduce operations

## Completion

You have completed Lab 8 when you can:
- Configure and use near caches
- Implement expiry and eviction policies
- Use entry processors for atomic operations
- Handle cache events
- Create continuous queries for real-time notifications
