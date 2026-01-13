# Near Cache Example

Demonstrates Near Cache for client-side caching in Apache Ignite.

## What is Near Cache?

Near cache is a local cache that resides on the client node. It stores frequently accessed data locally, eliminating network round-trips for repeated reads.

## Files

- **NearCacheServer.java** - Server that creates and populates the data cache
- **NearCacheClient.java** - Client demonstrating near cache performance benefits

## Usage

1. Build the project:
   ```bash
   mvn compile
   ```

2. Start the server:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ignite.NearCacheServer"
   ```

3. In another terminal, run the client:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ignite.NearCacheClient"
   ```

## Key Concepts

### Creating Near Cache

```java
// Configure near cache with LRU eviction
NearCacheConfiguration<Integer, String> nearCfg = new NearCacheConfiguration<>();
nearCfg.setNearEvictionPolicyFactory(new LruEvictionPolicyFactory<>(50));

// Get cache with near cache enabled
IgniteCache<Integer, String> cache = ignite.getOrCreateNearCache("dataCache", nearCfg);
```

### Performance Benefits

| Access Type | Network Round-Trip | Latency |
|-------------|-------------------|---------|
| Without near cache | Every read | Higher |
| With near cache | First read only | Much lower |

### Eviction Policies

- **LRU** (Least Recently Used) - Evicts least recently accessed entries
- **FIFO** (First In First Out) - Evicts oldest entries first
- **Sorted** - Evicts entries based on custom comparator

### Automatic Invalidation

Near cache entries are automatically invalidated when:
- The entry is updated on any node
- The entry is removed from the cache
- The cache is cleared

## When to Use Near Cache

**Good for:**
- Read-heavy workloads
- Frequently accessed reference data
- Data that changes infrequently

**Avoid for:**
- Write-heavy workloads
- Data that changes frequently
- Large datasets (memory constraints)
