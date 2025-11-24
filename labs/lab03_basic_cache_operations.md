# Lab 3: Basic Cache Operations

## Duration: 30 minutes

## Objectives
- Understand Ignite Cache API fundamentals
- Perform CRUD operations (put, get, remove, replace)
- Work with different cache modes (PARTITIONED, REPLICATED, LOCAL)
- Compare synchronous vs. asynchronous operations
- Implement batch operations

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

            // 3. LOCAL Cache
            CacheConfiguration<Integer, String> localCfg =
                new CacheConfiguration<>("localCache");
            localCfg.setCacheMode(CacheMode.LOCAL);

            IgniteCache<Integer, String> localCache =
                ignite.getOrCreateCache(localCfg);

            // Populate caches (only from node 1)
            if (nodeNumber == 1) {
                System.out.println("Node 1: Populating caches...\n");

                for (int i = 1; i <= 10; i++) {
                    partitionedCache.put(i, "Partitioned-" + i);
                    replicatedCache.put(i, "Replicated-" + i);
                    localCache.put(i, "Local-" + i);
                }
                System.out.println("Added 10 entries to each cache");
            }

            // Wait for data to propagate
            Thread.sleep(1000);

            // Check cache sizes on each node
            System.out.println("\n=== Cache Sizes on Node " + nodeNumber + " ===");
            System.out.println("Partitioned cache size: " + partitionedCache.size());
            System.out.println("Replicated cache size: " + replicatedCache.size());
            System.out.println("Local cache size: " + localCache.size());

            // Check local sizes (data actually stored on this node)
            System.out.println("\n=== Local Cache Sizes (data on this node) ===");
            System.out.println("Partitioned local size: " +
                partitionedCache.localSize());
            System.out.println("Replicated local size: " +
                replicatedCache.localSize());
            System.out.println("Local cache local size: " +
                localCache.localSize());

            // Demonstrate data access
            System.out.println("\n=== Data Access Test ===");
            System.out.println("Partitioned cache key 1: " +
                partitionedCache.get(1));
            System.out.println("Replicated cache key 1: " +
                replicatedCache.get(1));
            System.out.println("Local cache key 1: " +
                localCache.get(1));

            System.out.println("\n=== Cache Mode Characteristics ===");
            System.out.println("PARTITIONED: Data distributed across nodes, " +
                "with configurable backups");
            System.out.println("  - Best for: Large datasets, scalability");
            System.out.println("  - Memory usage: Divided across nodes");

            System.out.println("\nREPLICATED: Full copy of data on every node");
            System.out.println("  - Best for: Small, read-heavy datasets");
            System.out.println("  - Memory usage: Full dataset per node");

            System.out.println("\nLOCAL: Data exists only on this node");
            System.out.println("  - Best for: Node-specific data, no sharing");
            System.out.println("  - Memory usage: Only local data");

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

## Part 3: Asynchronous Operations (10 minutes)

### Exercise 3: Synchronous vs Asynchronous Operations

Create `Lab03AsyncOperations.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;

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

            // Asynchronous operations
            System.out.println("\n2. Asynchronous Operations:");
            IgniteCache<Integer, String> asyncCache = cache.withAsync();
            startTime = System.currentTimeMillis();

            for (int i = 0; i < 1000; i++) {
                asyncCache.put(i, "Value-" + i);
                IgniteFuture<Void> future = asyncCache.future();
                // Can do other work here while operation completes
            }

            // Wait for all operations to complete
            long asyncTime = System.currentTimeMillis() - startTime;
            System.out.println("   Time taken: " + asyncTime + " ms");

            System.out.println("\n3. Performance Comparison:");
            System.out.println("   Synchronous: " + syncTime + " ms");
            System.out.println("   Asynchronous: " + asyncTime + " ms");
            System.out.println("   Speedup: " + (float)syncTime / asyncTime + "x");

            // Async with callback
            System.out.println("\n4. Async Operations with Callback:");
            asyncCache.get(500);
            IgniteFuture<String> getFuture = asyncCache.future();

            getFuture.listen(f -> {
                System.out.println("   Callback: Retrieved value = " + f.get());
            });

            // Wait for callback
            Thread.sleep(100);

            // Batch async operations
            System.out.println("\n5. Batch Async Operations:");
            startTime = System.currentTimeMillis();

            IgniteFuture[] futures = new IgniteFuture[100];
            for (int i = 0; i < 100; i++) {
                asyncCache.put(i + 1000, "Batch-" + i);
                futures[i] = asyncCache.future();
            }

            // Wait for all to complete
            for (IgniteFuture future : futures) {
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

## Verification Steps

### Checklist
- [ ] Basic CRUD operations work correctly
- [ ] All three cache modes (PARTITIONED, REPLICATED, LOCAL) created
- [ ] Understand differences between cache modes
- [ ] Asynchronous operations implemented
- [ ] Batch operations demonstrate performance improvement
- [ ] Can explain when to use each cache mode

### Quick Test

```java
// Verify all operations
cache.put(1, "test");
assert cache.get(1).equals("test");
assert cache.containsKey(1);
cache.remove(1);
assert !cache.containsKey(1);
System.out.println("All operations verified!");
```

## Lab Questions

1. What is the difference between PARTITIONED and REPLICATED cache modes?
2. When should you use asynchronous operations?
3. What is the benefit of using putAll vs multiple put operations?
4. How many backups can a PARTITIONED cache have?

## Answers

1. **PARTITIONED**: Data is distributed across nodes with configurable backups. **REPLICATED**: Full copy on every node. Partitioned scales better for large datasets; replicated is better for read-heavy small datasets.

2. Use **async operations** when:
   - You can do other work while waiting
   - You need to parallelize operations
   - Network latency is high
   - Processing multiple independent requests

3. **putAll** reduces network round trips by sending multiple entries in one request, significantly improving performance for bulk operations.

4. A PARTITIONED cache can have **0 to N-1 backups** (where N is the number of nodes). Each backup stores a complete copy of the partition on a different node.

## Common Issues

**Issue: Cache not found on other nodes**
- Ensure cache configuration is consistent across nodes
- Use getOrCreateCache instead of createCache

**Issue: Local cache empty on node 2**
- Expected behavior - LOCAL caches are node-specific
- Data doesn't replicate to other nodes

**Issue: Async operations slower than sync**
- May occur with very small datasets
- Network overhead not justified for simple operations
- Benefits appear with larger workloads

## Next Steps

In Lab 4, you will:
- Configure caches using XML and programmatic methods
- Integrate with Spring Boot
- Set up monitoring and logging
- Learn configuration best practices

## Completion

You have completed Lab 3 when you can:
- Perform all basic CRUD operations
- Create and use different cache modes
- Implement async operations
- Use batch operations effectively
- Explain trade-offs between cache modes
