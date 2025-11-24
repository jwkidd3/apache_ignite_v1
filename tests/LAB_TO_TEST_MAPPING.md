# Lab Exercise to Test Mapping

This document maps each lab exercise to the specific tests that validate it.

## Lab 1: Environment Setup

### Exercise 1: Start First Ignite Node (15 min)

**Lab Instructions:**
```java
IgniteConfiguration cfg = new IgniteConfiguration();
cfg.setIgniteInstanceName("first-ignite-node");
Ignite ignite = Ignition.start(cfg);
```

**Validated by:**
- ✅ `testNodeStartup()` - Verifies node starts
- ✅ `testNodeConfiguration()` - Verifies configuration
- ✅ `testLocalNodeId()` - Verifies node has ID
- ✅ `testClusterMetrics()` - Verifies metrics available

**Run:** `mvn test -Dtest=Lab01EnvironmentSetupTest#testNodeStartup`

---

### Exercise 2: Create Cache (15 min)

**Lab Instructions:**
```java
CacheConfiguration<Integer, String> cfg =
    new CacheConfiguration<>("myCache");
IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);
```

**Validated by:**
- ✅ `testCacheCreation()` - Verifies cache created
- ✅ `testCacheDestroy()` - Verifies cache cleanup

**Run:** `mvn test -Dtest=Lab01EnvironmentSetupTest#testCacheCreation`

---

### Exercise 3: Basic Put/Get Operations (20 min)

**Lab Instructions:**
```java
cache.put(1, "Hello Ignite");
String value = cache.get(1);
System.out.println("Value: " + value);
```

**Validated by:**
- ✅ `testPutOperation()` - Verifies put stores data
- ✅ `testGetOperation()` - Verifies get retrieves data
- ✅ `testGetNonExistentKey()` - Verifies null for missing keys
- ✅ `testMultiplePuts()` - Verifies multiple operations

**Run:** `mvn test -Dtest=Lab01EnvironmentSetupTest#testPutOperation`

---

### Exercise 4: Remove and Clear Operations (10 min)

**Lab Instructions:**
```java
cache.remove(1);
cache.clear();
```

**Validated by:**
- ✅ `testRemoveOperation()` - Verifies remove deletes data
- ✅ `testClearOperation()` - Verifies clear empties cache

**Run:** `mvn test -Dtest=Lab01EnvironmentSetupTest#testRemoveOperation`

---

### Exercise 5: Advanced Operations (10 min)

**Lab Instructions:**
```java
cache.replace(1, "newValue");
cache.putIfAbsent(2, "value2");
boolean contains = cache.containsKey(1);
```

**Validated by:**
- ✅ `testReplaceOperation()` - Verifies replace updates
- ✅ `testReplaceNonExistent()` - Verifies replace fails on missing
- ✅ `testPutIfAbsent()` - Verifies putIfAbsent logic
- ✅ `testContainsKey()` - Verifies existence check

**Run:** `mvn test -Dtest=Lab01EnvironmentSetupTest#testReplaceOperation`

---

## Lab 2: Multi-Node Cluster

### Exercise 1: Static IP Discovery (15 min)

**Lab Instructions:**
```java
TcpDiscoverySpi spi = new TcpDiscoverySpi();
TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
```

**Validated by:**
- ✅ `testSecondNodeJoin()` - Verifies second node joins
- ✅ `testClusterTopology()` - Verifies topology visible

**Run:** `mvn test -Dtest=Lab02MultiNodeClusterTest#testSecondNodeJoin`

---

### Exercise 2: Multi-Node Cluster (20 min)

**Lab Instructions:**
- Start Node 1
- Start Node 2
- Start Node 3
- Verify cluster formation

**Validated by:**
- ✅ `testThreeNodeCluster()` - Verifies 3-node cluster
- ✅ `testClusterTopology()` - Verifies all nodes visible
- ✅ `testClusterMetricsAcrossNodes()` - Verifies consistent metrics

**Run:** `mvn test -Dtest=Lab02MultiNodeClusterTest#testThreeNodeCluster`

---

### Exercise 3: Data Replication (20 min)

**Lab Instructions:**
```java
CacheConfiguration<Integer, String> cfg =
    new CacheConfiguration<>("replicatedCache");
cfg.setBackups(1);
cache.put(1, "data");
// Access from Node 2
IgniteCache<Integer, String> cache2 = node2.cache("replicatedCache");
String value = cache2.get(1);  // Should see "data"
```

**Validated by:**
- ✅ `testDataReplication()` - Verifies backups work
- ✅ `testCacheAccessFromMultipleNodes()` - Verifies cache accessible
- ✅ `testDataConsistency()` - Verifies all nodes see same data
- ✅ `testDataDistribution()` - Verifies distribution

**Run:** `mvn test -Dtest=Lab02MultiNodeClusterTest#testDataReplication`

---

### Exercise 4: Node Failure (15 min)

**Lab Instructions:**
- Stop one node
- Verify cluster continues
- Check data still accessible

**Validated by:**
- ✅ `testNodeLeave()` - Verifies node leave detected
- ✅ `testNodeReconnection()` - Verifies reconnection works
- ✅ `testClusterStability()` - Verifies stability under load

**Run:** `mvn test -Dtest=Lab02MultiNodeClusterTest#testNodeLeave`

---

## Lab 3: Basic Cache Operations

### Exercise 1: Cache Modes (20 min)

**Lab Instructions:**
```java
// PARTITIONED
CacheConfiguration<K, V> partCfg = new CacheConfiguration<>("partCache");
partCfg.setCacheMode(CacheMode.PARTITIONED);

// REPLICATED
CacheConfiguration<K, V> replCfg = new CacheConfiguration<>("replCache");
replCfg.setCacheMode(CacheMode.REPLICATED);

// LOCAL
CacheConfiguration<K, V> localCfg = new CacheConfiguration<>("localCache");
localCfg.setCacheMode(CacheMode.LOCAL);
```

**Validated by:**
- ✅ `testCacheModesCreation()` - Tests all modes (parameterized test)
- ✅ `testPartitionedCache()` - Verifies PARTITIONED mode
- ✅ `testReplicatedCache()` - Verifies REPLICATED mode
- ✅ `testLocalCache()` - Verifies LOCAL mode

**Run:** `mvn test -Dtest=Lab03BasicCacheOperationsTest#testCacheModesCreation`

---

### Exercise 2: Atomic Operations (15 min)

**Lab Instructions:**
```java
CacheConfiguration<K, V> cfg = new CacheConfiguration<>("atomicCache");
cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

Integer oldValue = cache.getAndPut(1, 200);
boolean replaced = cache.replace(1, 200, 300);
```

**Validated by:**
- ✅ `testAtomicOperations()` - Verifies atomic mode
- ✅ `testGetAndRemove()` - Verifies getAndRemove
- ✅ `testReplaceIfEquals()` - Verifies conditional replace
- ✅ `testRemoveIfEquals()` - Verifies conditional remove

**Run:** `mvn test -Dtest=Lab03BasicCacheOperationsTest#testAtomicOperations`

---

### Exercise 3: Async Operations (20 min)

**Lab Instructions:**
```java
IgniteCache<Integer, String> asyncCache = cache.withAsync();
asyncCache.put(1, "value1");
IgniteFuture<Void> future = asyncCache.future();
future.get();
```

**Validated by:**
- ✅ `testAsyncPut()` - Verifies async put
- ✅ `testAsyncGet()` - Verifies async get

**Run:** `mvn test -Dtest=Lab03BasicCacheOperationsTest#testAsyncPut`

---

### Exercise 4: Batch Operations (25 min)

**Lab Instructions:**
```java
// Individual operations
for (int i = 0; i < 100; i++) {
    cache.put(i, "value" + i);  // Slow
}

// Batch operation
Map<Integer, String> batch = new HashMap<>();
for (int i = 0; i < 100; i++) {
    batch.put(i, "value" + i);
}
cache.putAll(batch);  // Fast!
```

**Validated by:**
- ✅ `testPutAll()` - Verifies putAll works
- ✅ `testGetAll()` - Verifies getAll works
- ✅ `testRemoveAll()` - Verifies removeAll works
- ✅ `testBatchPerformance()` - Verifies batch is faster

**Run:** `mvn test -Dtest=Lab03BasicCacheOperationsTest#testBatchPerformance`

---

## Lab 4: Configuration and Deployment

### Exercise 1: Programmatic Configuration (20 min)

**Lab Instructions:**
```java
IgniteConfiguration cfg = new IgniteConfiguration();
cfg.setIgniteInstanceName("configured-node");
cfg.setClientMode(false);

CacheConfiguration<K, V> cacheCfg = new CacheConfiguration<>("myCache");
cacheCfg.setBackups(1);
cacheCfg.setStatisticsEnabled(true);
```

**Validated by:**
- ✅ `testProgrammaticConfiguration()` - Verifies Ignite config
- ✅ `testCacheConfiguration()` - Verifies cache config
- ✅ `testMultipleCacheConfigs()` - Verifies multiple caches
- ✅ `testCacheName()` - Verifies naming

**Run:** `mvn test -Dtest=Lab04ConfigurationDeploymentTest#testProgrammaticConfiguration`

---

### Exercise 2: Cache Statistics (20 min)

**Lab Instructions:**
```java
CacheConfiguration<K, V> cfg = new CacheConfiguration<>("statsCache");
cfg.setStatisticsEnabled(true);

cache.put(1, "value1");
cache.get(1);  // Hit
cache.get(999);  // Miss

CacheMetrics metrics = cache.metrics();
System.out.println("Hits: " + metrics.getCacheHits());
System.out.println("Misses: " + metrics.getCacheMisses());
```

**Validated by:**
- ✅ `testCacheMetrics()` - Verifies metrics collection
- ✅ `testCacheHitMissStats()` - Verifies hit/miss tracking
- ✅ `testCacheSizeMetrics()` - Verifies size metrics
- ✅ `testCacheTimingMetrics()` - Verifies timing metrics

**Run:** `mvn test -Dtest=Lab04ConfigurationDeploymentTest#testCacheMetrics`

---

### Exercise 3: Cluster Metrics (15 min)

**Lab Instructions:**
```java
ClusterMetrics metrics = ignite.cluster().metrics();
System.out.println("CPUs: " + metrics.getTotalCpus());
System.out.println("Nodes: " + metrics.getTotalNodes());
System.out.println("CPU Load: " + metrics.getCurrentCpuLoad());
```

**Validated by:**
- ✅ `testClusterMetrics()` - Verifies cluster metrics
- ✅ `testLocalNodeMetrics()` - Verifies node metrics
- ✅ `testNodeAttributes()` - Verifies attributes

**Run:** `mvn test -Dtest=Lab04ConfigurationDeploymentTest#testClusterMetrics`

---

### Exercise 4: Data Regions (15 min)

**Lab Instructions:**
```java
DataStorageConfiguration storageCfg = new DataStorageConfiguration();
DataRegionConfiguration regionCfg = new DataRegionConfiguration();
regionCfg.setName("myRegion");
regionCfg.setMaxSize(100L * 1024 * 1024);  // 100 MB
```

**Validated by:**
- ✅ `testDataRegionMetrics()` - Verifies region metrics

**Run:** `mvn test -Dtest=Lab04ConfigurationDeploymentTest#testDataRegionMetrics`

---

## Lab 5: Data Modeling and Persistence

### Exercise 1: Affinity Keys (20 min)

**Lab Instructions:**
```java
class PersonKey {
    private int personId;

    @AffinityKeyMapped
    private int companyId;  // This ensures colocation by company
}
```

**Validated by:**
- ✅ `testAffinityKeyAnnotation()` - Verifies annotation works
- ✅ `testDataColocation()` - Verifies same company colocates
- ✅ `testAffinityMapping()` - Verifies partition mapping

**Run:** `mvn test -Dtest=Lab05DataModelingPersistenceTest#testAffinityKeyAnnotation`

---

### Exercise 2: Custom Keys (15 min)

**Lab Instructions:**
```java
CacheConfiguration<PersonKey, Person> cfg =
    new CacheConfiguration<>("personCache");

PersonKey key = new PersonKey(1, 100);
Person person = new Person("John", 30);
cache.put(key, person);
```

**Validated by:**
- ✅ `testCustomKeyClass()` - Verifies custom keys work
- ✅ `testComplexKeyOperations()` - Verifies operations with custom keys

**Run:** `mvn test -Dtest=Lab05DataModelingPersistenceTest#testCustomKeyClass`

---

### Exercise 3: Persistence Configuration (15 min)

**Lab Instructions:**
```java
DataStorageConfiguration storageCfg = new DataStorageConfiguration();
DataRegionConfiguration regionCfg = new DataRegionConfiguration();
regionCfg.setPersistenceEnabled(true);
regionCfg.setMaxSize(200L * 1024 * 1024);
```

**Validated by:**
- ✅ `testPersistenceConfiguration()` - Verifies config
- ✅ `testDataRegionConfig()` - Verifies region settings

**Run:** `mvn test -Dtest=Lab05DataModelingPersistenceTest#testPersistenceConfiguration`

---

## Quick Validation Commands

### Validate Single Exercise
```bash
# Example: Validate Lab 1, Exercise 3 (Put/Get operations)
mvn test -Dtest=Lab01EnvironmentSetupTest#testPutOperation
mvn test -Dtest=Lab01EnvironmentSetupTest#testGetOperation
```

### Validate Entire Lab
```bash
# Example: Validate all of Lab 1
mvn test -Dtest=Lab01EnvironmentSetupTest
```

### Validate Multiple Labs
```bash
# Example: Validate Labs 1 and 2
mvn test -Dtest=Lab01*,Lab02*
```

### Validate Everything
```bash
mvn test
```

---

## Mapping Summary

| Lab | Exercises | Total Tests | Command |
|-----|-----------|-------------|---------|
| Lab 1 | 5 exercises | 16 tests | `mvn test -Dtest=Lab01*` |
| Lab 2 | 4 exercises | 14 tests | `mvn test -Dtest=Lab02*` |
| Lab 3 | 4 exercises | 23 tests | `mvn test -Dtest=Lab03*` |
| Lab 4 | 4 exercises | 20 tests | `mvn test -Dtest=Lab04*` |
| Lab 5 | 3 exercises | 8 tests | `mvn test -Dtest=Lab05*` |

---

## Usage Tips

1. **After each exercise**: Run the specific tests for that exercise
2. **After each lab**: Run all tests for that lab
3. **Before final submission**: Run all tests

This ensures you catch issues early and validate progressively!
