# Lab 3: Basic Cache Operations

## Duration: 55 minutes

## Objectives
- Understand Ignite Cache API fundamentals
- Perform CRUD operations (put, get, remove, replace)
- Work with different cache modes (PARTITIONED, REPLICATED, LOCAL)
- Implement batch operations for efficiency

## Prerequisites
- Completed Lab 1 and Lab 2
- Understanding of basic Ignite cluster setup

## Part 1: Basic CRUD Operations (10 minutes)

### Exercise 1: Create and Use a Basic Cache

Create `Lab03BasicCacheOps.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

public class Lab03BasicCacheOps {

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cache-ops-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Basic Cache Operations Lab ===\n");

            // Create a cache configuration
            CacheConfiguration<Integer, String> cacheCfg =
                new CacheConfiguration<>("myCache");

            // Get or create cache
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // PUT operation
            System.out.println("1. PUT Operations:");
            cache.put(1, "Hello");
            cache.put(2, "World");
            cache.put(3, "Apache Ignite");
            System.out.println("   Added 3 entries to cache");

            // GET operation
            System.out.println("\n2. GET Operations:");
            String value1 = cache.get(1);
            String value2 = cache.get(2);
            System.out.println("   Key 1: " + value1);
            System.out.println("   Key 2: " + value2);

            // GET non-existent key
            String value99 = cache.get(99);
            System.out.println("   Key 99: " + value99 + " (null expected)");

            // CONTAINS operation
            System.out.println("\n3. CONTAINS Operations:");
            System.out.println("   Contains key 1: " + cache.containsKey(1));
            System.out.println("   Contains key 99: " + cache.containsKey(99));

            // REPLACE operation
            System.out.println("\n4. REPLACE Operations:");
            boolean replaced = cache.replace(1, "Hello", "Hi");
            System.out.println("   Replaced 'Hello' with 'Hi': " + replaced);
            System.out.println("   New value: " + cache.get(1));

            // Try to replace with wrong old value
            replaced = cache.replace(1, "Hello", "Hey");
            System.out.println("   Tried wrong old value: " + replaced + " (false expected)");

            // REMOVE operation
            System.out.println("\n5. REMOVE Operations:");
            boolean removed = cache.remove(3);
            System.out.println("   Removed key 3: " + removed);
            System.out.println("   Key 3 exists: " + cache.containsKey(3));

            // Conditional remove
            cache.put(4, "Test");
            removed = cache.remove(4, "Test");
            System.out.println("   Conditional remove: " + removed);

            // GET AND PUT
            System.out.println("\n6. GET AND PUT Operations:");
            String oldValue = cache.getAndPut(1, "Greetings");
            System.out.println("   Old value: " + oldValue);
            System.out.println("   New value: " + cache.get(1));

            // GET AND REMOVE
            System.out.println("\n7. GET AND REMOVE Operations:");
            oldValue = cache.getAndRemove(1);
            System.out.println("   Removed value: " + oldValue);
            System.out.println("   Key exists: " + cache.containsKey(1));

            // PUT IF ABSENT
            System.out.println("\n8. PUT IF ABSENT Operations:");
            boolean putResult = cache.putIfAbsent(5, "New Value");
            System.out.println("   Put if absent (new key): " + putResult);
            putResult = cache.putIfAbsent(5, "Another Value");
            System.out.println("   Put if absent (existing key): " + putResult);

            // Cache size
            System.out.println("\n9. Cache Statistics:");
            System.out.println("   Cache size: " + cache.size());

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Cache Modes (15 minutes)

### Exercise 2: Compare Different Cache Modes

Create `Lab03CacheModes.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

public class Lab03CacheModes {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Lab03CacheModes <nodeNumber>");
            System.exit(1);
        }

        int nodeNumber = Integer.parseInt(args[0]);

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cache-mode-node-" + nodeNumber);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Cache Modes Lab - Node " + nodeNumber + " ===\n");

            // 1. PARTITIONED Cache
            CacheConfiguration<Integer, String> partitionedCfg =
                new CacheConfiguration<>("partitionedCache");
            partitionedCfg.setCacheMode(CacheMode.PARTITIONED);
            partitionedCfg.setBackups(1); // Number of backup copies

            IgniteCache<Integer, String> partitionedCache =
                ignite.getOrCreateCache(partitionedCfg);

            // 2. REPLICATED Cache
            CacheConfiguration<Integer, String> replicatedCfg =
                new CacheConfiguration<>("replicatedCache");
            replicatedCfg.setCacheMode(CacheMode.REPLICATED);

            IgniteCache<Integer, String> replicatedCache =
                ignite.getOrCreateCache(replicatedCfg);

            // 3. PARTITIONED with no backups (simulates node-local behavior)
            // Note: LOCAL mode was deprecated in Ignite 2.x
            CacheConfiguration<Integer, String> noBackupCfg =
                new CacheConfiguration<>("noBackupCache");
            noBackupCfg.setCacheMode(CacheMode.PARTITIONED);
            noBackupCfg.setBackups(0); // No backups - data only on primary node

            IgniteCache<Integer, String> noBackupCache =
                ignite.getOrCreateCache(noBackupCfg);

            // Populate caches (only from node 1)
            if (nodeNumber == 1) {
                System.out.println("Node 1: Populating caches...\n");

                for (int i = 1; i <= 10; i++) {
                    partitionedCache.put(i, "Partitioned-" + i);
                    replicatedCache.put(i, "Replicated-" + i);
                    noBackupCache.put(i, "NoBackup-" + i);
                }
                System.out.println("Added 10 entries to each cache");
            }

            // Wait for rebalancing to complete
            // When a new node joins, data must be rebalanced across nodes
            // For PARTITIONED with 0 backups, entries physically MOVE between nodes
            System.out.println("\nWaiting for cluster rebalancing...");
            Thread.sleep(5000);
            System.out.println("Rebalancing complete.\n");

            // Check cache sizes (total entries across cluster)
            // Use CachePeekMode.ALL to count all entries
            System.out.println("=== Total Cache Sizes (cluster-wide) ===");
            System.out.println("Partitioned cache size: " +
                partitionedCache.size(org.apache.ignite.cache.CachePeekMode.ALL));
            System.out.println("Replicated cache size: " +
                replicatedCache.size(org.apache.ignite.cache.CachePeekMode.ALL));
            System.out.println("No-backup cache size: " +
                noBackupCache.size(org.apache.ignite.cache.CachePeekMode.ALL));

            // Check local sizes - this is where cache modes differ!
            System.out.println("\n=== Local Cache Sizes (data physically on THIS node) ===");

            // Show primary vs backup breakdown
            System.out.println("Partitioned (1 backup): local=" +
                partitionedCache.localSize() + " (primary=" +
                partitionedCache.localSize(org.apache.ignite.cache.CachePeekMode.PRIMARY) +
                ", backup=" +
                partitionedCache.localSize(org.apache.ignite.cache.CachePeekMode.BACKUP) + ")");

            System.out.println("Replicated: local=" +
                replicatedCache.localSize() + " (should be ALL 10 on every node)");

            System.out.println("No-backup: local=" +
                noBackupCache.localSize() + " (primary only, NO redundancy)");

            // Demonstrate data access
            System.out.println("\n=== Data Access Test ===");
            System.out.println("Partitioned cache key 1: " + partitionedCache.get(1));
            System.out.println("Replicated cache key 1: " + replicatedCache.get(1));
            System.out.println("No-backup cache key 1: " + noBackupCache.get(1));

            // Expected behavior explanation
            int clusterSize = ignite.cluster().forServers().nodes().size();
            System.out.println("\n=== Expected with " + clusterSize + " Node(s) ===");
            if (clusterSize == 1) {
                System.out.println("Single node: ALL entries are local (no distribution)");
            } else {
                System.out.println("PARTITIONED (1 backup): ~10 local (primary + backup)");
                System.out.println("REPLICATED: 10 local (full copy on every node)");
                System.out.println("NO-BACKUP: ~" + (10/clusterSize) + " local (no redundancy!)");
            }

            System.out.println("\n=== Cache Mode Characteristics ===");
            System.out.println("PARTITIONED: Data distributed across nodes");
            System.out.println("  - Best for: Large datasets, horizontal scaling");

            System.out.println("\nREPLICATED: Full copy of ALL data on EVERY node");
            System.out.println("  - Best for: Small, read-heavy reference data");

            System.out.println("\nPARTITIONED (0 backups): No redundancy");
            System.out.println("  - Data lost if node fails!");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Run Instructions:**

1. Start Node 1:
   ```bash
   java Lab03CacheModes 1
   ```

2. Start Node 2 (in another terminal):
   ```bash
   java Lab03CacheModes 2
   ```

3. Observe the differences in local cache sizes

## Part 3: Batch Operations (10 minutes)

### Exercise 3: Synchronous vs Asynchronous Operations

Create `Lab03AsyncOperations.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Lab03AsyncOperations {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Synchronous vs Asynchronous Operations ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("asyncCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Synchronous operations
            System.out.println("1. Synchronous Operations:");
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 1000; i++) {
                cache.put(i, "Value-" + i);
            }

            long syncTime = System.currentTimeMillis() - startTime;
            System.out.println("   Time taken: " + syncTime + " ms");

            // Clear cache
            cache.clear();

            // Asynchronous operations using modern API
            System.out.println("\n2. Asynchronous Operations:");
            startTime = System.currentTimeMillis();

            List<IgniteFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                IgniteFuture<Void> future = cache.putAsync(i, "Value-" + i);
                futures.add(future);
            }

            // Wait for all operations to complete
            for (IgniteFuture<Void> future : futures) {
                future.get();
            }

            long asyncTime = System.currentTimeMillis() - startTime;
            System.out.println("   Time taken: " + asyncTime + " ms");

            System.out.println("\n3. Performance Comparison:");
            System.out.println("   Synchronous: " + syncTime + " ms");
            System.out.println("   Asynchronous: " + asyncTime + " ms");
            if (asyncTime > 0) {
                System.out.println("   Speedup: " + (float)syncTime / asyncTime + "x");
            }

            // Async with callback
            System.out.println("\n4. Async Operations with Callback:");
            IgniteFuture<String> getFuture = cache.getAsync(500);

            getFuture.listen(f -> {
                System.out.println("   Callback: Retrieved value = " + f.get());
            });

            // Wait for callback
            Thread.sleep(100);

            // Batch async operations
            System.out.println("\n5. Batch Async Operations:");
            startTime = System.currentTimeMillis();

            List<IgniteFuture<Void>> batchFutures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                IgniteFuture<Void> future = cache.putAsync(i + 1000, "Batch-" + i);
                batchFutures.add(future);
            }

            // Wait for all to complete
            for (IgniteFuture<Void> future : batchFutures) {
                future.get();
            }

            long batchTime = System.currentTimeMillis() - startTime;
            System.out.println("   Batch operations time: " + batchTime + " ms");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 4: Batch Operations

Create `Lab03BatchOperations.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Lab03BatchOperations {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Batch Operations Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("batchCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Individual puts vs batch put
            System.out.println("1. Performance: Individual vs Batch Operations\n");

            // Individual puts
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                cache.put(i, "Value-" + i);
            }
            long individualTime = System.currentTimeMillis() - startTime;
            System.out.println("   Individual puts (1000): " + individualTime + " ms");

            cache.clear();

            // Batch put (putAll)
            startTime = System.currentTimeMillis();
            Map<Integer, String> batch = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                batch.put(i, "Value-" + i);
            }
            cache.putAll(batch);
            long batchTime = System.currentTimeMillis() - startTime;
            System.out.println("   Batch put (putAll): " + batchTime + " ms");
            System.out.println("   Performance gain: " +
                (float)individualTime / batchTime + "x faster");

            // Batch get (getAll)
            System.out.println("\n2. Batch GET Operations:\n");

            Set<Integer> keys = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                keys.add(i);
            }

            Map<Integer, String> values = cache.getAll(keys);
            System.out.println("   Retrieved " + values.size() + " entries");
            System.out.println("   Sample: " + values.get(0));

            // Batch remove (removeAll)
            System.out.println("\n3. Batch REMOVE Operations:\n");

            Set<Integer> keysToRemove = new HashSet<>();
            for (int i = 0; i < 50; i++) {
                keysToRemove.add(i);
            }

            cache.removeAll(keysToRemove);
            System.out.println("   Removed " + keysToRemove.size() + " entries");
            System.out.println("   Cache size now: " + cache.size());

            // Replace all
            System.out.println("\n4. Replace Operations:\n");

            Map<Integer, String> replacements = new HashMap<>();
            for (int i = 50; i < 100; i++) {
                replacements.put(i, "Updated-" + i);
            }

            cache.putAll(replacements);
            System.out.println("   Updated " + replacements.size() + " entries");

            System.out.println("\n=== Performance Best Practices ===");
            System.out.println("- Use putAll/getAll/removeAll for bulk operations");
            System.out.println("- Batch size: 500-1000 entries for optimal performance");
            System.out.println("- Reduce network round trips");
            System.out.println("- Use async operations for concurrent processing");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## Verification Steps

### Checklist
- [ ] Basic CRUD operations (put, get, remove, replace) completed
- [ ] Different cache modes (PARTITIONED, REPLICATED) tested
- [ ] Understood data distribution in PARTITIONED mode
- [ ] Batch operations (putAll, getAll, removeAll) implemented
- [ ] Asynchronous operations demonstrated

### Common Issues

**Issue: Cache returns null for existing key**
- Ensure you're using the correct cache name
- Verify the key type matches

**Issue: Data not distributed as expected**
- Check cache mode configuration
- Verify backups setting for PARTITIONED mode

## Lab Questions

1. What is the difference between PARTITIONED and REPLICATED cache modes?
2. When would you use putAll() instead of multiple put() calls?
3. What is the default number of backups in PARTITIONED mode?

## Answers

1. **PARTITIONED** distributes data across nodes (each key on one primary + backups), while **REPLICATED** stores all data on all nodes. PARTITIONED scales better for large datasets, REPLICATED provides faster reads.

2. Use **putAll()** when inserting multiple entries to reduce network round trips. It's more efficient than individual puts.

3. The default is **0 backups**. Set backups explicitly for fault tolerance.

## Next Steps

In Lab 4, you will:
- Learn configuration and deployment options
- Work with XML and programmatic configuration
- Configure data regions and persistence

## Completion

You have completed Lab 3 when you can:
- Perform CRUD operations on Ignite caches
- Configure and use different cache modes
- Implement efficient batch operations

---

## Optional Exercises (If Time Permits)

### Optional: Advanced Cache Operations

Create `Lab03AdvancedCacheOps.java` for entry processors and locks:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.concurrent.locks.Lock;

public class Lab03AdvancedCacheOps {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Advanced Cache Operations Lab ===\n");

            CacheConfiguration<String, Integer> cfg =
                new CacheConfiguration<>("advancedCache");
            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

            // 1. getAndPutIfAbsent - atomically get and put if not present
            System.out.println("1. getAndPutIfAbsent Operations:");
            cache.put("counter", 100);

            Integer oldVal = cache.getAndPutIfAbsent("counter", 200);
            System.out.println("   Existing key - old value: " + oldVal);
            System.out.println("   Current value: " + cache.get("counter"));

            oldVal = cache.getAndPutIfAbsent("newCounter", 50);
            System.out.println("   New key - old value: " + oldVal + " (null expected)");
            System.out.println("   New key current value: " + cache.get("newCounter"));

            // 2. getAndReplace - atomically get and replace
            System.out.println("\n2. getAndReplace Operations:");
            oldVal = cache.getAndReplace("counter", 150);
            System.out.println("   Old value: " + oldVal);
            System.out.println("   New value: " + cache.get("counter"));

            oldVal = cache.getAndReplace("nonExistent", 999);
            System.out.println("   Non-existent key result: " + oldVal + " (null expected)");

            // 3. EntryProcessor - invoke operations directly on cache entries
            System.out.println("\n3. EntryProcessor (invoke) Operations:");

            // Increment counter using EntryProcessor
            Integer result = cache.invoke("counter", new IncrementProcessor(), 10);
            System.out.println("   Invoked increment by 10, new value: " + result);

            // Another increment
            result = cache.invoke("counter", new IncrementProcessor(), 5);
            System.out.println("   Invoked increment by 5, new value: " + result);

            // Multiply using EntryProcessor
            result = cache.invoke("counter", new MultiplyProcessor(), 2);
            System.out.println("   Invoked multiply by 2, new value: " + result);

            // 4. Lock operations for explicit locking
            System.out.println("\n4. Lock Operations:");

            Lock lock = cache.lock("counter");
            System.out.println("   Acquiring lock on 'counter'...");

            lock.lock();
            try {
                System.out.println("   Lock acquired!");
                Integer val = cache.get("counter");
                System.out.println("   Current value: " + val);

                // Simulate some work
                Thread.sleep(100);

                cache.put("counter", val + 100);
                System.out.println("   Updated value: " + cache.get("counter"));
            } finally {
                lock.unlock();
                System.out.println("   Lock released!");
            }

            // 5. tryLock with timeout
            System.out.println("\n5. TryLock with Timeout:");

            Lock tryLock = cache.lock("counter");
            boolean acquired = tryLock.tryLock();
            if (acquired) {
                try {
                    System.out.println("   TryLock succeeded!");
                    cache.put("counter", cache.get("counter") + 1);
                } finally {
                    tryLock.unlock();
                }
            } else {
                System.out.println("   TryLock failed - lock held by another thread");
            }

            // 6. invokeAll for batch processing
            System.out.println("\n6. InvokeAll for Batch Processing:");
            cache.put("val1", 10);
            cache.put("val2", 20);
            cache.put("val3", 30);

            java.util.Set<String> keys = new java.util.HashSet<>();
            keys.add("val1");
            keys.add("val2");
            keys.add("val3");

            java.util.Map<String, javax.cache.processor.EntryProcessorResult<Integer>> results =
                cache.invokeAll(keys, new IncrementProcessor(), 5);

            System.out.println("   After invokeAll (increment by 5):");
            for (java.util.Map.Entry<String, javax.cache.processor.EntryProcessorResult<Integer>> entry : results.entrySet()) {
                System.out.println("   " + entry.getKey() + " = " + entry.getValue().get());
            }

            System.out.println("\n=== Advanced Operations Summary ===");
            System.out.println("- getAndPutIfAbsent: Atomic read-and-insert");
            System.out.println("- getAndReplace: Atomic read-and-update");
            System.out.println("- invoke/EntryProcessor: Server-side processing");
            System.out.println("- lock: Explicit distributed locking");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // EntryProcessor to increment a value
    static class IncrementProcessor implements EntryProcessor<String, Integer, Integer> {
        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            int increment = (Integer) args[0];
            int currentValue = entry.exists() ? entry.getValue() : 0;
            int newValue = currentValue + increment;
            entry.setValue(newValue);
            return newValue;
        }
    }

    // EntryProcessor to multiply a value
    static class MultiplyProcessor implements EntryProcessor<String, Integer, Integer> {
        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            int multiplier = (Integer) args[0];
            int currentValue = entry.exists() ? entry.getValue() : 0;
            int newValue = currentValue * multiplier;
            entry.setValue(newValue);
            return newValue;
        }
    }
}
```

### Optional: Cache Iteration and Streaming

### Exercise 6: Cache Iteration and ScanQuery

Create `Lab03CacheIteration.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteBiPredicate;

import javax.cache.Cache;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab03CacheIteration {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Iteration and ScanQuery Lab ===\n");

            CacheConfiguration<Integer, Product> cfg =
                new CacheConfiguration<>("productsCache");
            IgniteCache<Integer, Product> cache = ignite.getOrCreateCache(cfg);

            // Populate cache with sample data
            System.out.println("1. Populating cache with sample products...");
            Map<Integer, Product> products = new HashMap<>();
            products.put(1, new Product("Laptop", "Electronics", 999.99));
            products.put(2, new Product("Phone", "Electronics", 699.99));
            products.put(3, new Product("Tablet", "Electronics", 499.99));
            products.put(4, new Product("Desk", "Furniture", 299.99));
            products.put(5, new Product("Chair", "Furniture", 149.99));
            products.put(6, new Product("Book", "Education", 29.99));
            products.put(7, new Product("Notebook", "Education", 9.99));
            products.put(8, new Product("Monitor", "Electronics", 349.99));
            products.put(9, new Product("Keyboard", "Electronics", 79.99));
            products.put(10, new Product("Mouse", "Electronics", 39.99));

            cache.putAll(products);
            System.out.println("   Added " + products.size() + " products");

            // 2. forEach iteration
            System.out.println("\n2. Using forEach to iterate cache:");
            AtomicInteger count = new AtomicInteger(0);
            cache.forEach(entry -> {
                count.incrementAndGet();
                if (count.get() <= 3) { // Show first 3 only
                    System.out.println("   " + entry.getKey() + ": " + entry.getValue());
                }
            });
            System.out.println("   ... (total " + count.get() + " entries)");

            // 3. ScanQuery - retrieve all entries
            System.out.println("\n3. ScanQuery - All Entries:");
            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(new ScanQuery<>())) {
                int displayed = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    if (displayed++ < 3) {
                        System.out.println("   " + entry.getKey() + ": " + entry.getValue());
                    }
                }
                System.out.println("   ... (query completed)");
            }

            // 4. ScanQuery with filter - find electronics
            System.out.println("\n4. ScanQuery with Filter - Electronics Only:");
            IgniteBiPredicate<Integer, Product> electronicsFilter =
                (key, product) -> "Electronics".equals(product.getCategory());

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(new ScanQuery<>(electronicsFilter))) {
                double totalValue = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    Product p = entry.getValue();
                    System.out.println("   " + p.getName() + " - $" + p.getPrice());
                    totalValue += p.getPrice();
                }
                System.out.println("   Total electronics value: $" + totalValue);
            }

            // 5. ScanQuery with filter - price range
            System.out.println("\n5. ScanQuery - Products under $100:");
            IgniteBiPredicate<Integer, Product> priceFilter =
                (key, product) -> product.getPrice() < 100;

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(new ScanQuery<>(priceFilter))) {
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    Product p = entry.getValue();
                    System.out.println("   " + p.getName() + " - $" + p.getPrice());
                }
            }

            // 6. ScanQuery with local-only flag
            System.out.println("\n6. ScanQuery - Local Entries Only:");
            ScanQuery<Integer, Product> localQuery = new ScanQuery<>();
            localQuery.setLocal(true);

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(localQuery)) {
                int localCount = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    localCount++;
                }
                System.out.println("   Found " + localCount + " entries on local node");
            }

            // 7. ScanQuery with page size
            System.out.println("\n7. ScanQuery with Page Size (pagination):");
            ScanQuery<Integer, Product> pagedQuery = new ScanQuery<>();
            pagedQuery.setPageSize(3); // 3 entries per page

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(pagedQuery)) {
                System.out.println("   Processing with page size 3...");
                int pageCount = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    pageCount++;
                }
                System.out.println("   Processed " + pageCount + " entries");
            }

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Product class for demonstration
    static class Product implements java.io.Serializable {
        private String name;
        private String category;
        private double price;

        public Product(String name, String category, double price) {
            this.name = name;
            this.category = category;
            this.price = price;
        }

        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }

        @Override
        public String toString() {
            return name + " (" + category + ") - $" + price;
        }
    }
}
```

### Exercise 7: DataStreamer for High-Speed Loading

Create `Lab03DataStreamer.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.stream.StreamReceiver;

import java.util.HashMap;
import java.util.Map;

public class Lab03DataStreamer {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== DataStreamer Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("streamCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            int totalRecords = 100000;

            // 1. Compare regular putAll vs DataStreamer
            System.out.println("1. Performance Comparison: putAll vs DataStreamer\n");

            // Method 1: Using putAll (batch)
            cache.clear();
            long startTime = System.currentTimeMillis();

            int batchSize = 1000;
            for (int i = 0; i < totalRecords; i += batchSize) {
                Map<Integer, String> batch = new HashMap<>();
                for (int j = i; j < Math.min(i + batchSize, totalRecords); j++) {
                    batch.put(j, "Value-" + j);
                }
                cache.putAll(batch);
            }

            long putAllTime = System.currentTimeMillis() - startTime;
            System.out.println("   putAll time for " + totalRecords + " records: " +
                putAllTime + " ms");
            System.out.println("   Throughput: " +
                (totalRecords * 1000 / putAllTime) + " records/sec");

            // Method 2: Using DataStreamer
            cache.clear();
            startTime = System.currentTimeMillis();

            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("streamCache")) {

                // Configure streamer for maximum performance
                streamer.perNodeBufferSize(1024);
                streamer.perNodeParallelOperations(8);
                streamer.allowOverwrite(true);

                for (int i = 0; i < totalRecords; i++) {
                    streamer.addData(i, "Value-" + i);
                }
            } // Auto-flush on close

            long streamerTime = System.currentTimeMillis() - startTime;
            System.out.println("\n   DataStreamer time for " + totalRecords + " records: " +
                streamerTime + " ms");
            System.out.println("   Throughput: " +
                (totalRecords * 1000 / streamerTime) + " records/sec");
            System.out.println("   Speedup: " +
                String.format("%.2f", (double)putAllTime / streamerTime) + "x faster");

            // 2. DataStreamer with receiver (transformation)
            System.out.println("\n2. DataStreamer with StreamReceiver (Transform on load):");

            CacheConfiguration<Integer, Integer> numCfg =
                new CacheConfiguration<>("numbersCache");
            IgniteCache<Integer, Integer> numCache = ignite.getOrCreateCache(numCfg);

            try (IgniteDataStreamer<Integer, Integer> streamer =
                    ignite.dataStreamer("numbersCache")) {

                // Set a receiver that doubles the values
                streamer.receiver(StreamReceiver.SINGLE_ENTRY_OVERRIDE);

                for (int i = 0; i < 10; i++) {
                    streamer.addData(i, i);
                }
            }

            System.out.println("   Loaded numbers 0-9 with SINGLE_ENTRY_OVERRIDE receiver");
            System.out.println("   Sample values: ");
            for (int i = 0; i < 5; i++) {
                System.out.println("      Key " + i + " = " + numCache.get(i));
            }

            // 3. DataStreamer options explained
            System.out.println("\n3. DataStreamer Configuration Options:");

            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("streamCache")) {

                // Per-node buffer size
                streamer.perNodeBufferSize(2048);
                System.out.println("   perNodeBufferSize: 2048 entries");
                System.out.println("      - Entries buffered before sending to each node");

                // Parallel operations per node
                streamer.perNodeParallelOperations(16);
                System.out.println("   perNodeParallelOperations: 16");
                System.out.println("      - Concurrent streams per node");

                // Allow overwrite
                streamer.allowOverwrite(true);
                System.out.println("   allowOverwrite: true");
                System.out.println("      - Replace existing entries");

                // Auto-flush frequency
                streamer.autoFlushFrequency(1000);
                System.out.println("   autoFlushFrequency: 1000 ms");
                System.out.println("      - Auto-flush interval");
            }

            // 4. Manual flush demonstration
            System.out.println("\n4. Manual Flush Control:");

            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("streamCache")) {

                streamer.autoFlushFrequency(0); // Disable auto-flush

                for (int i = 0; i < 100; i++) {
                    streamer.addData(i + 200000, "Batch1-" + i);
                }

                System.out.println("   Added 100 entries (not flushed yet)");
                System.out.println("   Cache size before flush: " + cache.size());

                streamer.flush();
                System.out.println("   After manual flush, cache size: " + cache.size());
            }

            System.out.println("\n=== DataStreamer Best Practices ===");
            System.out.println("- Use for bulk loading (10,000+ entries)");
            System.out.println("- Tune perNodeBufferSize based on entry size");
            System.out.println("- Set allowOverwrite based on your use case");
            System.out.println("- Always use try-with-resources for proper cleanup");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Optional: Binary Objects and Keep Binary

### Exercise 8: Working with Binary Objects

Create `Lab03BinaryObjects.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.Cache;
import java.io.Serializable;

public class Lab03BinaryObjects {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Binary Objects and Keep Binary Lab ===\n");

            CacheConfiguration<Integer, Person> cfg =
                new CacheConfiguration<>("personCache");
            IgniteCache<Integer, Person> cache = ignite.getOrCreateCache(cfg);

            // 1. Store regular objects
            System.out.println("1. Storing Regular Objects:");
            cache.put(1, new Person("Alice", 30, "Engineering"));
            cache.put(2, new Person("Bob", 25, "Marketing"));
            cache.put(3, new Person("Charlie", 35, "Engineering"));
            System.out.println("   Stored 3 Person objects");

            // 2. Retrieve as regular objects
            System.out.println("\n2. Retrieve as Regular Objects:");
            Person alice = cache.get(1);
            System.out.println("   Person 1: " + alice);
            System.out.println("   Class: " + alice.getClass().getName());

            // 3. Get cache in "keep binary" mode
            System.out.println("\n3. Using withKeepBinary():");
            IgniteCache<Integer, BinaryObject> binaryCache = cache.withKeepBinary();

            BinaryObject binaryAlice = binaryCache.get(1);
            System.out.println("   Binary Object retrieved");
            System.out.println("   Type: " + binaryAlice.type().typeName());
            System.out.println("   Fields: " + binaryAlice.type().fieldNames());

            // 4. Access fields from BinaryObject
            System.out.println("\n4. Accessing Fields from BinaryObject:");
            String name = binaryAlice.field("name");
            int age = binaryAlice.field("age");
            String dept = binaryAlice.field("department");
            System.out.println("   Name: " + name);
            System.out.println("   Age: " + age);
            System.out.println("   Department: " + dept);

            // 5. Benefits: No deserialization cost
            System.out.println("\n5. Performance Benefit - Field Access Without Deserialization:");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                Person p = cache.get(1);
                String n = p.getName();
            }
            long regularTime = System.currentTimeMillis() - startTime;
            System.out.println("   Regular access (1000 iterations): " + regularTime + " ms");

            startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                BinaryObject bo = binaryCache.get(1);
                String n = bo.field("name");
            }
            long binaryTime = System.currentTimeMillis() - startTime;
            System.out.println("   Binary access (1000 iterations): " + binaryTime + " ms");

            // 6. Create BinaryObject without class definition
            System.out.println("\n6. Creating BinaryObject with Builder (no class needed):");
            BinaryObjectBuilder builder = ignite.binary().builder("Employee");
            builder.setField("id", 100);
            builder.setField("name", "Dynamic Employee");
            builder.setField("salary", 75000.0);
            builder.setField("active", true);

            BinaryObject employee = builder.build();
            binaryCache.put(100, employee);
            System.out.println("   Created and stored Employee BinaryObject");

            BinaryObject retrieved = binaryCache.get(100);
            System.out.println("   Retrieved - Name: " + retrieved.field("name"));
            System.out.println("   Retrieved - Salary: " + retrieved.field("salary"));

            // 7. Modify BinaryObject
            System.out.println("\n7. Modifying BinaryObject:");
            BinaryObject bobBinary = binaryCache.get(2);

            // Create new object from existing with modifications
            BinaryObject modifiedBob = bobBinary.toBuilder()
                .setField("age", 26)  // Birthday!
                .setField("department", "Sales")  // Department change
                .build();

            binaryCache.put(2, modifiedBob);
            System.out.println("   Modified Bob's age and department");

            BinaryObject updatedBob = binaryCache.get(2);
            System.out.println("   New age: " + updatedBob.field("age"));
            System.out.println("   New department: " + updatedBob.field("department"));

            // 8. Query with BinaryObjects
            System.out.println("\n8. ScanQuery with BinaryObjects:");
            try (QueryCursor<Cache.Entry<Integer, BinaryObject>> cursor =
                    binaryCache.query(new ScanQuery<>(
                        (k, v) -> "Engineering".equals(v.field("department"))))) {

                System.out.println("   Engineering employees:");
                for (Cache.Entry<Integer, BinaryObject> entry : cursor) {
                    BinaryObject bo = entry.getValue();
                    System.out.println("   - " + bo.field("name") +
                        " (age: " + bo.field("age") + ")");
                }
            }

            System.out.println("\n=== Binary Objects Benefits ===");
            System.out.println("- No need for class on server side");
            System.out.println("- Field access without full deserialization");
            System.out.println("- Dynamic schema - add fields at runtime");
            System.out.println("- Efficient for partial reads");
            System.out.println("- Cross-platform compatibility");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Person class for demonstration
    static class Person implements Serializable {
        private String name;
        private int age;
        private String department;

        public Person(String name, int age, String department) {
            this.name = name;
            this.age = age;
            this.department = department;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getDepartment() { return department; }

        @Override
        public String toString() {
            return name + " (age: " + age + ", dept: " + department + ")";
        }
    }
}
```

### Optional: Performance Measurement

### Exercise 9: Comprehensive Performance Benchmarking

Create `Lab03PerformanceBenchmark.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Lab03PerformanceBenchmark {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Performance Measurement Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("benchmarkCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Warmup
            System.out.println("Warming up...");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                cache.put(i, "warmup-" + i);
                cache.get(i);
            }
            cache.clear();

            // 1. Individual vs Batch PUT
            System.out.println("\n" + "=".repeat(60));
            System.out.println("1. PUT Performance: Individual vs Batch");
            System.out.println("=".repeat(60));

            // Individual PUTs
            long[] individualLatencies = new long[BENCHMARK_ITERATIONS];
            long startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long opStart = System.nanoTime();
                cache.put(i, "individual-" + i);
                individualLatencies[i] = System.nanoTime() - opStart;
            }
            long individualTotalTime = System.nanoTime() - startTime;

            cache.clear();

            // Batch PUTs
            long[] batchLatencies = new long[BENCHMARK_ITERATIONS / BATCH_SIZE];
            startTime = System.nanoTime();
            int batchIndex = 0;
            for (int i = 0; i < BENCHMARK_ITERATIONS; i += BATCH_SIZE) {
                Map<Integer, String> batch = new HashMap<>();
                for (int j = i; j < i + BATCH_SIZE; j++) {
                    batch.put(j, "batch-" + j);
                }
                long opStart = System.nanoTime();
                cache.putAll(batch);
                batchLatencies[batchIndex++] = System.nanoTime() - opStart;
            }
            long batchTotalTime = System.nanoTime() - startTime;

            printResults("Individual PUT", individualLatencies, individualTotalTime, BENCHMARK_ITERATIONS);
            printResults("Batch PUT (size " + BATCH_SIZE + ")", batchLatencies, batchTotalTime, BENCHMARK_ITERATIONS);

            // 2. Sync vs Async PUT
            System.out.println("\n" + "=".repeat(60));
            System.out.println("2. PUT Performance: Sync vs Async");
            System.out.println("=".repeat(60));

            cache.clear();

            // Sync PUTs
            long[] syncLatencies = new long[BENCHMARK_ITERATIONS];
            startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long opStart = System.nanoTime();
                cache.put(i, "sync-" + i);
                syncLatencies[i] = System.nanoTime() - opStart;
            }
            long syncTotalTime = System.nanoTime() - startTime;

            cache.clear();

            // Async PUTs
            List<IgniteFuture<Void>> futures = new ArrayList<>();
            startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                futures.add(cache.putAsync(i, "async-" + i));
            }
            // Wait for all
            for (IgniteFuture<Void> f : futures) {
                f.get();
            }
            long asyncTotalTime = System.nanoTime() - startTime;

            printResults("Sync PUT", syncLatencies, syncTotalTime, BENCHMARK_ITERATIONS);
            System.out.println("\nAsync PUT:");
            System.out.println("   Total time: " + formatNanos(asyncTotalTime));
            System.out.println("   Throughput: " +
                String.format("%,.0f", BENCHMARK_ITERATIONS * 1_000_000_000.0 / asyncTotalTime) + " ops/sec");

            // 3. GET Performance
            System.out.println("\n" + "=".repeat(60));
            System.out.println("3. GET Performance: Individual vs Batch");
            System.out.println("=".repeat(60));

            // Individual GETs
            long[] getLatencies = new long[BENCHMARK_ITERATIONS];
            startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long opStart = System.nanoTime();
                cache.get(i);
                getLatencies[i] = System.nanoTime() - opStart;
            }
            long getTotalTime = System.nanoTime() - startTime;

            // Batch GETs
            long[] batchGetLatencies = new long[BENCHMARK_ITERATIONS / BATCH_SIZE];
            startTime = System.nanoTime();
            batchIndex = 0;
            for (int i = 0; i < BENCHMARK_ITERATIONS; i += BATCH_SIZE) {
                Set<Integer> keys = new HashSet<>();
                for (int j = i; j < i + BATCH_SIZE; j++) {
                    keys.add(j);
                }
                long opStart = System.nanoTime();
                cache.getAll(keys);
                batchGetLatencies[batchIndex++] = System.nanoTime() - opStart;
            }
            long batchGetTotalTime = System.nanoTime() - startTime;

            printResults("Individual GET", getLatencies, getTotalTime, BENCHMARK_ITERATIONS);
            printResults("Batch GET (size " + BATCH_SIZE + ")", batchGetLatencies, batchGetTotalTime, BENCHMARK_ITERATIONS);

            // 4. DataStreamer Performance
            System.out.println("\n" + "=".repeat(60));
            System.out.println("4. DataStreamer Performance");
            System.out.println("=".repeat(60));

            int streamCount = 10000;
            cache.clear();

            startTime = System.nanoTime();
            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("benchmarkCache")) {
                streamer.perNodeBufferSize(1024);
                for (int i = 0; i < streamCount; i++) {
                    streamer.addData(i, "streamed-" + i);
                }
            }
            long streamerTime = System.nanoTime() - startTime;

            System.out.println("\nDataStreamer (" + streamCount + " entries):");
            System.out.println("   Total time: " + formatNanos(streamerTime));
            System.out.println("   Throughput: " +
                String.format("%,.0f", streamCount * 1_000_000_000.0 / streamerTime) + " ops/sec");

            // Summary
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PERFORMANCE SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("\nKey Findings:");
            System.out.println("- Batch operations reduce network round-trips");
            System.out.println("- Async operations improve throughput for concurrent workloads");
            System.out.println("- DataStreamer is optimal for bulk loading");
            System.out.println("- Latency percentiles help identify outliers");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printResults(String operation, long[] latencies, long totalTime, int operations) {
        Arrays.sort(latencies);

        long min = latencies[0];
        long max = latencies[latencies.length - 1];
        long p50 = latencies[(int)(latencies.length * 0.50)];
        long p90 = latencies[(int)(latencies.length * 0.90)];
        long p99 = latencies[(int)(latencies.length * 0.99)];

        double avg = 0;
        for (long l : latencies) avg += l;
        avg /= latencies.length;

        System.out.println("\n" + operation + ":");
        System.out.println("   Total time: " + formatNanos(totalTime));
        System.out.println("   Throughput: " +
            String.format("%,.0f", operations * 1_000_000_000.0 / totalTime) + " ops/sec");
        System.out.println("   Latency:");
        System.out.println("      Min:  " + formatNanos(min));
        System.out.println("      Avg:  " + formatNanos((long)avg));
        System.out.println("      P50:  " + formatNanos(p50));
        System.out.println("      P90:  " + formatNanos(p90));
        System.out.println("      P99:  " + formatNanos(p99));
        System.out.println("      Max:  " + formatNanos(max));
    }

    private static String formatNanos(long nanos) {
        if (nanos < 1000) {
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.2f us", nanos / 1000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanos / 1_000_000_000.0);
        }
    }
}
```

### Optional: Challenge Exercises

### Exercise 10: Atomic Counter Implementation

Create `Lab03AtomicCounter.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab03AtomicCounter {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Atomic Counter Challenge ===\n");

            // Method 1: Using IgniteAtomicLong (built-in)
            System.out.println("1. Built-in IgniteAtomicLong:");
            IgniteAtomicLong atomicLong = ignite.atomicLong("myCounter", 0, true);

            System.out.println("   Initial value: " + atomicLong.get());
            System.out.println("   After increment: " + atomicLong.incrementAndGet());
            System.out.println("   After addAndGet(10): " + atomicLong.addAndGet(10));
            System.out.println("   After decrementAndGet: " + atomicLong.decrementAndGet());

            // Concurrent increments
            int threads = 10;
            int incrementsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threads);

            atomicLong.getAndSet(0); // Reset

            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        atomicLong.incrementAndGet();
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

            System.out.println("   After " + (threads * incrementsPerThread) +
                " concurrent increments: " + atomicLong.get());

            // Method 2: Using Cache with EntryProcessor
            System.out.println("\n2. Cache-backed Counter with EntryProcessor:");

            CacheConfiguration<String, Long> cfg =
                new CacheConfiguration<>("counterCache");
            IgniteCache<String, Long> cache = ignite.getOrCreateCache(cfg);

            String counterId = "myCounter";
            cache.put(counterId, 0L);

            // Thread-safe increment using EntryProcessor
            Long newValue = cache.invoke(counterId, new AtomicIncrementProcessor(), 1L);
            System.out.println("   After invoke increment: " + newValue);

            newValue = cache.invoke(counterId, new AtomicIncrementProcessor(), 5L);
            System.out.println("   After invoke +5: " + newValue);

            // Concurrent test with cache-based counter
            cache.put(counterId, 0L);
            executor = Executors.newFixedThreadPool(threads);

            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        cache.invoke(counterId, new AtomicIncrementProcessor(), 1L);
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

            System.out.println("   After " + (threads * incrementsPerThread) +
                " concurrent increments: " + cache.get(counterId));

            // Method 3: Compare-and-swap loop
            System.out.println("\n3. Manual CAS (Compare-And-Swap) Loop:");
            cache.put("casCounter", 0L);

            // Increment using CAS
            long result = casIncrement(cache, "casCounter", 1);
            System.out.println("   After CAS increment: " + result);

            result = casIncrement(cache, "casCounter", 10);
            System.out.println("   After CAS +10: " + result);

            System.out.println("\n=== Counter Implementation Comparison ===");
            System.out.println("- IgniteAtomicLong: Simplest, best performance");
            System.out.println("- EntryProcessor: Flexible, supports custom logic");
            System.out.println("- CAS Loop: Educational, shows underlying mechanism");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // EntryProcessor for atomic increment
    static class AtomicIncrementProcessor
            implements EntryProcessor<String, Long, Long> {
        @Override
        public Long process(MutableEntry<String, Long> entry, Object... args)
                throws EntryProcessorException {
            long increment = (Long) args[0];
            long current = entry.exists() ? entry.getValue() : 0L;
            long newValue = current + increment;
            entry.setValue(newValue);
            return newValue;
        }
    }

    // CAS-based increment
    static long casIncrement(IgniteCache<String, Long> cache, String key, long delta) {
        while (true) {
            Long current = cache.get(key);
            if (current == null) current = 0L;
            long newValue = current + delta;

            if (cache.replace(key, current, newValue)) {
                return newValue;
            }
            // Retry if someone else modified the value
        }
    }
}
```

### Exercise 11: Simple Cache Loader

Create `Lab03CacheLoader.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.store.CacheLoadOnlyStoreAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteBiTuple;

import javax.cache.Cache;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import java.io.Serializable;
import java.util.*;

public class Lab03CacheLoader {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Loader Challenge ===\n");

            // 1. Simple read-through CacheLoader
            System.out.println("1. Read-Through CacheLoader:");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("loaderCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setReadThrough(true);
            cfg.setCacheLoaderFactory(FactoryBuilder.factoryOf(SimpleCacheLoader.class));

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // These will trigger the loader
            System.out.println("   Accessing key 1 (not in cache): " + cache.get(1));
            System.out.println("   Accessing key 5 (not in cache): " + cache.get(5));
            System.out.println("   Accessing key 100 (not in cache): " + cache.get(100));

            // After loading
            System.out.println("   Cache size after loads: " + cache.size());

            // 2. Bulk loading from external source
            System.out.println("\n2. Bulk Loading (loadCache):");

            CacheConfiguration<String, Product> productCfg =
                new CacheConfiguration<>("productLoaderCache");
            productCfg.setCacheMode(CacheMode.PARTITIONED);
            productCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(ProductStore.class));
            productCfg.setReadThrough(true);

            IgniteCache<String, Product> productCache = ignite.getOrCreateCache(productCfg);

            // Load all data from "database"
            System.out.println("   Loading all products...");
            productCache.loadCache(null); // null filter = load all

            System.out.println("   Loaded " + productCache.size() + " products");

            // Access loaded data
            Product laptop = productCache.get("LAPTOP-001");
            if (laptop != null) {
                System.out.println("   Sample product: " + laptop);
            }

            // 3. Conditional loading
            System.out.println("\n3. Conditional Loading (with filter):");
            productCache.clear();

            // Load only products over $500
            productCache.loadCache((key, value) -> value.getPrice() > 500);
            System.out.println("   Products over $500: " + productCache.size());

            // 4. Manual loading pattern
            System.out.println("\n4. Manual Loading Pattern:");

            CacheConfiguration<String, String> manualCfg =
                new CacheConfiguration<>("manualLoaderCache");
            IgniteCache<String, String> manualCache = ignite.getOrCreateCache(manualCfg);

            // Simulate loading from external source
            Map<String, String> externalData = simulateExternalDataSource();

            System.out.println("   Loading " + externalData.size() + " entries from external source...");
            long startTime = System.currentTimeMillis();
            manualCache.putAll(externalData);
            long loadTime = System.currentTimeMillis() - startTime;
            System.out.println("   Loaded in " + loadTime + " ms");

            System.out.println("\n=== Cache Loader Patterns ===");
            System.out.println("- Read-through: Load on cache miss");
            System.out.println("- Write-through: Write to store on put");
            System.out.println("- loadCache(): Bulk pre-population");
            System.out.println("- Manual putAll: Simple bulk loading");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simple CacheLoader implementation
    public static class SimpleCacheLoader
            implements CacheLoader<Integer, String>, Serializable {
        @Override
        public String load(Integer key) throws CacheLoaderException {
            // Simulate loading from external source
            System.out.println("   [Loader] Loading key: " + key);
            return "Loaded-Value-" + key;
        }

        @Override
        public Map<Integer, String> loadAll(Iterable<? extends Integer> keys)
                throws CacheLoaderException {
            Map<Integer, String> result = new HashMap<>();
            for (Integer key : keys) {
                result.put(key, load(key));
            }
            return result;
        }
    }

    // Product store with bulk loading
    public static class ProductStore
            extends CacheLoadOnlyStoreAdapter<String, Product, Map.Entry<String, Product>>
            implements Serializable {

        @Override
        protected Iterator<Map.Entry<String, Product>> inputIterator(Object... args) {
            // Simulate database records
            Map<String, Product> products = new LinkedHashMap<>();
            products.put("LAPTOP-001", new Product("LAPTOP-001", "Gaming Laptop", 1299.99));
            products.put("PHONE-001", new Product("PHONE-001", "Smartphone", 799.99));
            products.put("TABLET-001", new Product("TABLET-001", "Tablet Pro", 599.99));
            products.put("WATCH-001", new Product("WATCH-001", "Smart Watch", 299.99));
            products.put("EARBUDS-001", new Product("EARBUDS-001", "Wireless Earbuds", 149.99));

            return products.entrySet().iterator();
        }

        @Override
        protected IgniteBiTuple<String, Product> parse(
                Map.Entry<String, Product> entry, Object... args) {
            return new IgniteBiTuple<>(entry.getKey(), entry.getValue());
        }
    }

    // Product class
    static class Product implements Serializable {
        private String id;
        private String name;
        private double price;

        public Product(String id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }

        @Override
        public String toString() {
            return name + " ($" + price + ")";
        }
    }

    // Simulate external data source
    private static Map<String, String> simulateExternalDataSource() {
        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            data.put("key-" + i, "value-" + i);
        }
        return data;
    }
}
```

### Exercise 12: TTL-Based Expiration

Create `Lab03ManualTTL.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class Lab03ManualTTL {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== TTL-Based Expiration Challenge ===\n");

            // 1. Built-in TTL using ExpiryPolicy
            System.out.println("1. Built-in ExpiryPolicy (5 seconds TTL):");

            CacheConfiguration<String, String> cfg =
                new CacheConfiguration<>("ttlCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setExpiryPolicyFactory(
                CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 5)));

            IgniteCache<String, String> cache = ignite.getOrCreateCache(cfg);

            cache.put("key1", "This will expire in 5 seconds");
            System.out.println("   Put key1, value: " + cache.get("key1"));
            System.out.println("   Waiting 3 seconds...");
            Thread.sleep(3000);
            System.out.println("   After 3s, key1: " + cache.get("key1"));
            System.out.println("   Waiting 3 more seconds...");
            Thread.sleep(3000);
            System.out.println("   After 6s, key1: " + cache.get("key1") + " (should be null)");

            // 2. Per-entry TTL using withExpiryPolicy
            System.out.println("\n2. Per-Entry ExpiryPolicy:");

            CacheConfiguration<String, String> cfg2 =
                new CacheConfiguration<>("perEntryTTLCache");
            IgniteCache<String, String> cache2 = ignite.getOrCreateCache(cfg2);

            // Entry with 2-second TTL
            cache2.withExpiryPolicy(
                CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 2)).create())
                .put("short", "Short-lived entry");

            // Entry with 10-second TTL
            cache2.withExpiryPolicy(
                CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 10)).create())
                .put("long", "Long-lived entry");

            // Entry with no expiration
            cache2.put("permanent", "Permanent entry");

            System.out.println("   Added entries with different TTLs");
            System.out.println("   short (2s): " + cache2.get("short"));
            System.out.println("   long (10s): " + cache2.get("long"));
            System.out.println("   permanent: " + cache2.get("permanent"));

            System.out.println("   Waiting 3 seconds...");
            Thread.sleep(3000);

            System.out.println("   After 3s:");
            System.out.println("   short: " + cache2.get("short") + " (should be null)");
            System.out.println("   long: " + cache2.get("long"));
            System.out.println("   permanent: " + cache2.get("permanent"));

            // 3. Manual TTL implementation
            System.out.println("\n3. Manual TTL Implementation:");

            CacheConfiguration<String, TimedEntry> manualCfg =
                new CacheConfiguration<>("manualTTLCache");
            IgniteCache<String, TimedEntry> manualCache = ignite.getOrCreateCache(manualCfg);

            // Store with timestamp
            long ttlMs = 4000; // 4 seconds
            manualCache.put("manual1", new TimedEntry("Value 1", ttlMs));
            manualCache.put("manual2", new TimedEntry("Value 2", ttlMs));

            System.out.println("   Added entries with 4s manual TTL");

            // Get with expiration check
            System.out.println("   manual1: " + getWithTTLCheck(manualCache, "manual1"));
            System.out.println("   manual2: " + getWithTTLCheck(manualCache, "manual2"));

            System.out.println("   Waiting 5 seconds...");
            Thread.sleep(5000);

            System.out.println("   After 5s:");
            System.out.println("   manual1: " + getWithTTLCheck(manualCache, "manual1") + " (expired)");
            System.out.println("   manual2: " + getWithTTLCheck(manualCache, "manual2") + " (expired)");

            // 4. Sliding TTL (reset on access)
            System.out.println("\n4. Sliding TTL (extends on access):");

            CacheConfiguration<String, SlidingEntry> slidingCfg =
                new CacheConfiguration<>("slidingTTLCache");
            IgniteCache<String, SlidingEntry> slidingCache = ignite.getOrCreateCache(slidingCfg);

            long slidingTTL = 3000; // 3 seconds
            slidingCache.put("sliding", new SlidingEntry("Sliding value", slidingTTL));

            System.out.println("   Added entry with 3s sliding TTL");

            for (int i = 0; i < 5; i++) {
                Thread.sleep(2000);
                String value = getWithSlidingTTL(slidingCache, "sliding");
                if (value != null) {
                    System.out.println("   Access #" + (i + 1) + " (after 2s): " + value + " (TTL extended)");
                } else {
                    System.out.println("   Access #" + (i + 1) + ": null (expired)");
                    break;
                }
            }

            // Don't access for 4 seconds
            System.out.println("   Waiting 4s without access...");
            Thread.sleep(4000);
            System.out.println("   After 4s without access: " +
                getWithSlidingTTL(slidingCache, "sliding") + " (should be null)");

            System.out.println("\n=== TTL Implementation Patterns ===");
            System.out.println("- ExpiryPolicy: Built-in, efficient, server-side");
            System.out.println("- Per-entry TTL: Different TTL per entry");
            System.out.println("- Manual TTL: Full control, application-managed");
            System.out.println("- Sliding TTL: Extends expiration on access");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Entry with fixed TTL
    static class TimedEntry implements Serializable {
        private String value;
        private long createdAt;
        private long ttlMs;

        public TimedEntry(String value, long ttlMs) {
            this.value = value;
            this.createdAt = System.currentTimeMillis();
            this.ttlMs = ttlMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > (createdAt + ttlMs);
        }

        public String getValue() { return value; }
    }

    // Entry with sliding TTL
    static class SlidingEntry implements Serializable {
        private String value;
        private long lastAccessedAt;
        private long ttlMs;

        public SlidingEntry(String value, long ttlMs) {
            this.value = value;
            this.lastAccessedAt = System.currentTimeMillis();
            this.ttlMs = ttlMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > (lastAccessedAt + ttlMs);
        }

        public void touch() {
            this.lastAccessedAt = System.currentTimeMillis();
        }

        public String getValue() { return value; }
    }

    // Get with TTL check (removes if expired)
    static String getWithTTLCheck(IgniteCache<String, TimedEntry> cache, String key) {
        TimedEntry entry = cache.get(key);
        if (entry == null) return null;

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    // Get with sliding TTL (extends TTL on access)
    static String getWithSlidingTTL(IgniteCache<String, SlidingEntry> cache, String key) {
        SlidingEntry entry = cache.get(key);
        if (entry == null) return null;

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        // Extend TTL
        entry.touch();
        cache.put(key, entry);
        return entry.getValue();
    }
}
```

