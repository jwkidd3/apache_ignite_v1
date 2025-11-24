# Module 3: Basic Cache Operations

**Duration:** 60 minutes
**Type:** Presentation

---

## Slide 1: Title
**Basic Cache Operations in Apache Ignite**

Master the fundamentals of data operations

---

## Slide 2: Module Objectives

Learn to:
- Use Cache API fundamentals
- Perform CRUD operations
- Understand cache modes (PARTITIONED, REPLICATED, LOCAL)
- Compare synchronous vs asynchronous operations
- Implement batch operations effectively

---

## Slide 3: Cache API Overview

### Core Interface

```java
IgniteCache<K, V> cache = ignite.cache("myCache");

// Or create if not exists
IgniteCache<K, V> cache = ignite.getOrCreateCache("myCache");

// With configuration
CacheConfiguration<K, V> cfg = new CacheConfiguration<>("myCache");
IgniteCache<K, V> cache = ignite.getOrCreateCache(cfg);
```

**Key Points:**
- Type-safe generic interface
- Similar to java.util.Map
- JCache (JSR 107) compliant
- Thread-safe operations

---

## Slide 4: Basic CRUD Operations

### Create/Update - PUT

```java
// Put single entry
cache.put(key, value);

// Put if absent
boolean added = cache.putIfAbsent(key, value);

// Get and put (returns old value)
V oldValue = cache.getAndPut(key, newValue);
```

### Read - GET

```java
// Get single entry
V value = cache.get(key);

// Check existence
boolean exists = cache.containsKey(key);
```

### Delete - REMOVE

```java
// Remove entry
boolean removed = cache.remove(key);

// Conditional remove
boolean removed = cache.remove(key, value);

// Get and remove
V value = cache.getAndRemove(key);
```

---

## Slide 5: Replace Operations

### Atomic Replace

```java
// Replace (key must exist)
boolean replaced = cache.replace(key, newValue);

// Conditional replace (compare-and-swap)
boolean replaced = cache.replace(key, oldValue, newValue);

// Get and replace
V oldValue = cache.getAndReplace(key, newValue);
```

**Use Cases:**
- Optimistic locking
- Version control
- Atomic updates
- Race condition prevention

**Example:**
```java
// Increment counter safely
while (true) {
    Integer current = cache.get("counter");
    if (cache.replace("counter", current, current + 1)) {
        break; // Success
    }
    // Retry if another thread modified it
}
```

---

## Slide 6: Cache Modes - Overview

### Three Cache Modes

**PARTITIONED (Default)**
- Data distributed across nodes
- Best for scalability
- Configurable backups

**REPLICATED**
- Full copy on every node
- Best for small, read-heavy datasets
- Slower writes

**LOCAL**
- Data only on local node
- No distribution
- Good for node-specific data

---

## Slide 7: PARTITIONED Cache Mode

### Distributed Data

```java
CacheConfiguration<K, V> cfg = new CacheConfiguration<>("partCache");
cfg.setCacheMode(CacheMode.PARTITIONED);
cfg.setBackups(1); // Number of backup copies
```

**Characteristics:**
```
With 3 nodes, 1 backup:
Node 1: Primary [A, B, C]    Backup [G, H, I]
Node 2: Primary [D, E, F]    Backup [A, B, C]
Node 3: Primary [G, H, I]    Backup [D, E, F]

Total Memory Usage = Data Size × (1 + Backups) / Nodes
```

**Benefits:**
- ✅ Scales horizontally
- ✅ Efficient memory use
- ✅ Parallel processing

**Trade-offs:**
- ❌ Network hops for remote data
- ❌ Rebalancing on topology changes

---

## Slide 8: REPLICATED Cache Mode

### Full Copy Everywhere

```java
CacheConfiguration<K, V> cfg = new CacheConfiguration<>("replCache");
cfg.setCacheMode(CacheMode.REPLICATED);
// No backup setting needed - all nodes have all data
```

**Characteristics:**
```
With 3 nodes:
Node 1: [A, B, C, D, E, F, G, H, I]
Node 2: [A, B, C, D, E, F, G, H, I]
Node 3: [A, B, C, D, E, F, G, H, I]

Total Memory Usage = Data Size × Number of Nodes
```

**Benefits:**
- ✅ No network hops (local reads)
- ✅ Fast reads
- ✅ High availability

**Trade-offs:**
- ❌ High memory usage
- ❌ Slower writes (update all nodes)
- ❌ Doesn't scale with data size

**Best For:**
- Reference data
- Configuration
- Small datasets (< 10 GB)
- Read-heavy workloads

---

## Slide 9: LOCAL Cache Mode

### Node-Specific Data

```java
CacheConfiguration<K, V> cfg = new CacheConfiguration<>("localCache");
cfg.setCacheMode(CacheMode.LOCAL);
```

**Characteristics:**
```
With 3 nodes:
Node 1: [A, B, C]
Node 2: [D, E, F]
Node 3: [G, H, I]

No sharing, no backups
```

**Benefits:**
- ✅ Simple
- ✅ No network overhead
- ✅ Fast operations

**Trade-offs:**
- ❌ No distribution
- ❌ No fault tolerance
- ❌ Limited scalability

**Best For:**
- Session data
- Thread-local caches
- Node-specific computations
- Temporary data

---

## Slide 10: Choosing Cache Mode

### Decision Matrix

| Criteria | PARTITIONED | REPLICATED | LOCAL |
|----------|-------------|------------|-------|
| **Data Size** | Large | Small | Any |
| **Read Pattern** | Any | High | Any |
| **Write Pattern** | High | Low | Any |
| **Memory Efficiency** | ✅ Best | ❌ Worst | ✅ Good |
| **Read Speed** | ⚠️ Network | ✅ Local | ✅ Local |
| **Scalability** | ✅ Scales | ❌ Limited | ❌ No |
| **Fault Tolerance** | ✅ Yes | ✅ Yes | ❌ No |

**Rule of Thumb:**
- User data, transactions → PARTITIONED
- Reference data, lookups → REPLICATED
- Temporary, node-specific → LOCAL

---

## Slide 11: Synchronous Operations

### Default Behavior

```java
// Synchronous put (waits for completion)
cache.put(key, value); // Blocks until acknowledged

// Synchronous get
V value = cache.get(key); // Blocks until value retrieved
```

**Characteristics:**
- ⏱️ Blocking calls
- ✅ Simple programming model
- ✅ Immediate error handling
- ❌ Lower throughput
- ❌ Can't do other work while waiting

**When to Use:**
- Single operations
- Need immediate confirmation
- Sequential logic
- Error handling critical

---

## Slide 12: Asynchronous Operations

### Non-Blocking Pattern

```java
IgniteCache<K, V> asyncCache = cache.withAsync();

// Async put
asyncCache.put(key, value);
IgniteFuture<Void> future = asyncCache.future();

// Do other work...
processOtherData();

// Wait for completion when needed
future.get();
```

**With Callbacks:**
```java
asyncCache.get(key);
IgniteFuture<V> future = asyncCache.future();

future.listen(f -> {
    V value = f.get();
    System.out.println("Value retrieved: " + value);
});
```

**Benefits:**
- ✅ Non-blocking
- ✅ Higher throughput
- ✅ Parallel operations
- ✅ Better resource utilization

**When to Use:**
- Multiple independent operations
- High-throughput scenarios
- Can do useful work while waiting

---

## Slide 13: Async Performance

### Comparison

**Scenario:** Update 1000 cache entries

**Synchronous:**
```java
long start = System.currentTimeMillis();
for (int i = 0; i < 1000; i++) {
    cache.put(i, "Value-" + i); // Wait for each
}
long time = System.currentTimeMillis() - start;
// Time: ~500ms (sequential network calls)
```

**Asynchronous:**
```java
long start = System.currentTimeMillis();
IgniteCache<K, V> asyncCache = cache.withAsync();
List<IgniteFuture> futures = new ArrayList<>();

for (int i = 0; i < 1000; i++) {
    asyncCache.put(i, "Value-" + i);
    futures.add(asyncCache.future());
}
futures.forEach(f -> f.get()); // Wait for all
long time = System.currentTimeMillis() - start;
// Time: ~100ms (parallel network calls)
```

**Speedup: 5x faster!**

---

## Slide 14: Batch Operations

### putAll / getAll

**Batch Put:**
```java
Map<K, V> batch = new HashMap<>();
for (int i = 0; i < 1000; i++) {
    batch.put(i, "Value-" + i);
}
cache.putAll(batch); // Single network call
```

**Batch Get:**
```java
Set<K> keys = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
Map<K, V> values = cache.getAll(keys);
```

**Batch Remove:**
```java
Set<K> keys = new HashSet<>(Arrays.asList(1, 2, 3));
cache.removeAll(keys);
```

**Performance Gain:**
- 10-100x faster than individual operations
- Reduced network round trips
- Lower protocol overhead
- Efficient serialization

---

## Slide 15: Batch Size Optimization

### Finding the Sweet Spot

**Too Small:**
```java
// Batch size: 10
// Result: Many network calls, not optimal
```

**Optimal:**
```java
// Batch size: 500-1000
// Result: Good balance
```

**Too Large:**
```java
// Batch size: 100,000
// Result: Memory pressure, timeout risk
```

**Best Practices:**
- Batch size: 500-1000 entries
- Monitor network utilization
- Adjust based on entry size
- Consider memory constraints

**Example Pattern:**
```java
List<Entry> allData = loadData();
int batchSize = 500;

for (int i = 0; i < allData.size(); i += batchSize) {
    Map<K, V> batch = new HashMap<>();
    for (int j = i; j < Math.min(i + batchSize, allData.size()); j++) {
        batch.put(allData.get(j).key, allData.get(j).value);
    }
    cache.putAll(batch);
}
```

---

## Slide 16: Atomic Operations

### Thread-Safe Operations

**Built-in Atomicity:**
```java
// These are atomic (no race conditions)
cache.putIfAbsent(key, value);
cache.replace(key, oldValue, newValue);
cache.remove(key, value);
cache.getAndPut(key, value);
cache.getAndRemove(key);
cache.getAndReplace(key, value);
```

**Why Atomic Matters:**
```java
// NON-ATOMIC (Bad - race condition)
if (!cache.containsKey(key)) {
    cache.put(key, value); // Another thread may have added it
}

// ATOMIC (Good - safe)
cache.putIfAbsent(key, value);
```

**Use Cases:**
- Counters
- Flags
- Resource allocation
- Distributed locks

---

## Slide 17: Cache Iteration

### Scanning Cache Entries

**forEach:**
```java
cache.forEach(entry -> {
    System.out.println(entry.getKey() + " = " + entry.getValue());
});
```

**Iterator:**
```java
for (Cache.Entry<K, V> entry : cache) {
    K key = entry.getKey();
    V value = entry.getValue();
}
```

**Scan Query:**
```java
ScanQuery<K, V> scan = new ScanQuery<>((k, v) -> v.startsWith("A"));

try (QueryCursor<Cache.Entry<K, V>> cursor = cache.query(scan)) {
    for (Cache.Entry<K, V> entry : cursor) {
        // Process entries
    }
}
```

**Caution:**
- Iteration can be expensive on large datasets
- Consider using SQL queries for filtering
- Use scan queries for distributed filtering

---

## Slide 18: Cache Size and Metrics

### Monitoring Cache

**Size Operations:**
```java
// Local size (on this node)
int localSize = cache.localSize();

// Total size (across cluster)
int totalSize = cache.size();

// Size with mode
int primarySize = cache.localSize(CachePeekMode.PRIMARY);
int backupSize = cache.localSize(CachePeekMode.BACKUP);
```

**Cache Metrics:**
```java
CacheMetrics metrics = cache.metrics();

System.out.println("Gets: " + metrics.getCacheGets());
System.out.println("Puts: " + metrics.getCachePuts());
System.out.println("Hits: " + metrics.getCacheHits());
System.out.println("Misses: " + metrics.getCacheMisses());
System.out.println("Hit %: " + metrics.getCacheHitPercentage());
```

---

## Slide 19: Clear vs Destroy

### Cache Management

**Clear (Empty Cache):**
```java
cache.clear(); // Removes all entries, keeps cache structure
```
- Cache still exists
- Can add new entries
- Configuration preserved

**Remove All:**
```java
cache.removeAll(); // Same as clear but with events
```

**Destroy (Delete Cache):**
```java
ignite.destroyCache("myCache"); // Completely removes cache
```
- Cache no longer exists
- Must recreate to use
- Frees all resources

**Use Cases:**
- Clear: Reset data for new test
- RemoveAll: Clear with event notifications
- Destroy: Remove cache entirely

---

## Slide 20: Error Handling

### Exception Management

**Common Exceptions:**
```java
try {
    cache.put(key, value);
} catch (CacheException e) {
    // Generic cache operation error
} catch (TransactionException e) {
    // Transaction-specific error
} catch (ClusterTopologyException e) {
    // Cluster state changed during operation
}
```

**Timeout Handling:**
```java
IgniteCache<K, V> cacheWithTimeout = cache.withExpiryPolicy(
    new CreatedExpiryPolicy(new Duration(TimeUnit.SECONDS, 10))
);
```

**Retry Pattern:**
```java
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
    try {
        cache.put(key, value);
        break; // Success
    } catch (ClusterTopologyException e) {
        if (i == maxRetries - 1) throw e;
        Thread.sleep(100); // Wait and retry
    }
}
```

---

## Slide 21: Performance Best Practices

### Optimization Tips

**1. Use Batch Operations**
```java
// ❌ Slow
for (Entry e : entries) cache.put(e.key, e.value);

// ✅ Fast
cache.putAll(entriesMap);
```

**2. Use Async for Parallel Operations**
```java
// ❌ Sequential
for (K key : keys) cache.get(key);

// ✅ Parallel
IgniteCache async = cache.withAsync();
List<Future> futures = new ArrayList<>();
for (K key : keys) {
    async.get(key);
    futures.add(async.future());
}
```

**3. Choose Right Cache Mode**
- Large data → PARTITIONED
- Small, read-heavy → REPLICATED

**4. Configure Backups Appropriately**
- 1 backup usually sufficient
- More backups = more memory

**5. Enable Statistics**
```java
cfg.setStatisticsEnabled(true); // Monitor performance
```

---

## Slide 22: Cache Configuration Summary

### Key Settings

```java
CacheConfiguration<K, V> cfg = new CacheConfiguration<>();

// Basic settings
cfg.setName("myCache");
cfg.setCacheMode(CacheMode.PARTITIONED);
cfg.setBackups(1);

// Atomicity
cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

// Write synchronization
cfg.setWriteSynchronizationMode(
    CacheWriteSynchronizationMode.FULL_SYNC);

// Statistics
cfg.setStatisticsEnabled(true);

// Rebalancing
cfg.setRebalanceMode(CacheRebalanceMode.ASYNC);
```

---

## Slide 23: Common Patterns

### Real-World Usage

**Pattern 1: Caching Database Results**
```java
V value = cache.get(key);
if (value == null) {
    value = database.query(key);
    cache.put(key, value);
}
return value;
```

**Pattern 2: Distributed Counter**
```java
cache.putIfAbsent("counter", 0);
while (true) {
    Integer current = cache.get("counter");
    if (cache.replace("counter", current, current + 1)) {
        break;
    }
}
```

**Pattern 3: Session Storage**
```java
// Store session
cache.put(sessionId, sessionData);

// With expiry
IgniteCache withExpiry = cache.withExpiryPolicy(
    new CreatedExpiryPolicy(Duration.THIRTY_MINUTES));
withExpiry.put(sessionId, sessionData);
```

---

## Slide 24: Troubleshooting

### Common Issues

**Issue 1: Slow Operations**
- ✓ Check network latency
- ✓ Review cache mode choice
- ✓ Use batch operations
- ✓ Enable async operations

**Issue 2: High Memory Usage**
- ✓ Review backup count
- ✓ Check cache mode (REPLICATED?)
- ✓ Implement eviction policies
- ✓ Monitor data growth

**Issue 3: Data Not Found**
- ✓ Check cache name
- ✓ Verify key type matches
- ✓ Check if data was actually written
- ✓ Review eviction settings

**Issue 4: CacheException**
- ✓ Check cluster topology
- ✓ Verify node is connected
- ✓ Review logs for details
- ✓ Check network connectivity

---

## Slide 25: Key Takeaways

### Remember These Points

1. **Cache modes matter**
   - PARTITIONED: scalable, distributed
   - REPLICATED: fast reads, high memory
   - LOCAL: node-specific, simple

2. **Batch operations are faster**
   - putAll/getAll reduce network calls
   - 10-100x performance improvement

3. **Use async for parallelism**
   - Non-blocking operations
   - Better throughput

4. **Atomic operations prevent races**
   - putIfAbsent, replace, etc.
   - Thread-safe by default

5. **Monitor with metrics**
   - Hit ratios
   - Operation times
   - Cache sizes

---

## Slide 26: Questions?

### Discussion Topics

- Cache mode selection for your use case
- Batch size optimization
- Async vs sync trade-offs
- Performance expectations

**Next:** Lab 3 - Basic Cache Operations

---

## Instructor Notes

### Timing Guide
- Slides 1-6: 15 minutes (basics and CRUD)
- Slides 7-10: 15 minutes (cache modes)
- Slides 11-15: 15 minutes (sync/async/batch)
- Slides 16-26: 15 minutes (advanced topics and wrap-up)

### Key Points to Emphasize
1. Difference between cache modes (Slides 7-10)
2. Batch operations performance (Slide 14)
3. Atomic operations importance (Slide 16)
4. Best practices (Slide 21)

### Demo Opportunities
- Show PUT/GET operations
- Compare sync vs async performance
- Demonstrate batch operations speedup
- Show cache metrics

### Common Questions
1. "Which cache mode should I use?"
2. "How much faster are batch operations?"
3. "When should I use async?"
4. "How do I handle concurrent updates?"
5. "Can I change cache mode later?"
