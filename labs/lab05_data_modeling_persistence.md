# Lab 5: Data Modeling and Persistence

## Duration: 45 minutes

## Objectives
- Learn data modeling best practices for Ignite
- Implement affinity keys for data colocation
- Configure native persistence layer
- Implement write-through, write-behind, and read-through patterns
- Integrate with external databases

## Prerequisites
- Completed Labs 1-4
- Understanding of cache operations
- JDBC knowledge (for database integration)

## Part 1: Data Modeling with Affinity Keys (15 minutes)

### Exercise 1: Basic Data Model

Create domain classes:

```java
package com.example.ignite.model;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import java.io.Serializable;

public class Customer implements Serializable {
    private Integer customerId;
    private String name;
    private String email;
    private String city;

    public Customer() {}

    public Customer(Integer customerId, String name, String email, String city) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.city = city;
    }

    // Getters and setters
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
```

```java
package com.example.ignite.model;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import java.io.Serializable;

public class Order implements Serializable {
    private Integer orderId;

    @AffinityKeyMapped
    private Integer customerId;  // Affinity key - ensures colocation with Customer

    private String product;
    private Double amount;

    public Order() {}

    public Order(Integer orderId, Integer customerId, String product, Double amount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.product = product;
        this.amount = amount;
    }

    // Getters and setters
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", product='" + product + '\'' +
                ", amount=" + amount +
                '}';
    }
}
```

### Exercise 2: Implement Affinity Colocation

Create `Lab05AffinityKeys.java`:

```java
package com.example.ignite;

import com.example.ignite.model.Customer;
import com.example.ignite.model.Order;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;

public class Lab05AffinityKeys {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Affinity Keys and Colocation Lab ===\n");

            // Create caches
            CacheConfiguration<Integer, Customer> customerCfg =
                new CacheConfiguration<>("customers");
            customerCfg.setBackups(1);

            CacheConfiguration<Integer, Order> orderCfg =
                new CacheConfiguration<>("orders");
            orderCfg.setBackups(1);

            IgniteCache<Integer, Customer> customerCache =
                ignite.getOrCreateCache(customerCfg);
            IgniteCache<Integer, Order> orderCache =
                ignite.getOrCreateCache(orderCfg);

            // Create test data
            Customer customer1 = new Customer(1, "John Doe",
                "john@example.com", "New York");
            Customer customer2 = new Customer(2, "Jane Smith",
                "jane@example.com", "Los Angeles");

            Order order1 = new Order(101, 1, "Laptop", 1200.00);
            Order order2 = new Order(102, 1, "Mouse", 25.00);
            Order order3 = new Order(103, 2, "Keyboard", 75.00);

            // Store data
            customerCache.put(1, customer1);
            customerCache.put(2, customer2);
            orderCache.put(101, order1);
            orderCache.put(102, order2);
            orderCache.put(103, order3);

            // Check affinity - verify colocation
            Affinity<Integer> customerAffinity = ignite.affinity("customers");
            Affinity<Integer> orderAffinity = ignite.affinity("orders");

            System.out.println("=== Affinity Verification ===");

            // Customer 1 and their orders
            ClusterNode customer1Node = customerAffinity.mapKeyToNode(1);
            ClusterNode order1Node = orderAffinity.mapKeyToNode(101);
            ClusterNode order2Node = orderAffinity.mapKeyToNode(102);

            System.out.println("Customer 1 on node: " +
                customer1Node.id().toString().substring(0, 8) + "...");
            System.out.println("Order 101 on node: " +
                order1Node.id().toString().substring(0, 8) + "...");
            System.out.println("Order 102 on node: " +
                order2Node.id().toString().substring(0, 8) + "...");

            System.out.println("\nColocated (Customer 1 with Orders): " +
                (customer1Node.equals(order1Node) && customer1Node.equals(order2Node)));

            // Customer 2 and their orders
            ClusterNode customer2Node = customerAffinity.mapKeyToNode(2);
            ClusterNode order3Node = orderAffinity.mapKeyToNode(103);

            System.out.println("\nCustomer 2 on node: " +
                customer2Node.id().toString().substring(0, 8) + "...");
            System.out.println("Order 103 on node: " +
                order3Node.id().toString().substring(0, 8) + "...");
            System.out.println("Colocated (Customer 2 with Order): " +
                customer2Node.equals(order3Node));

            System.out.println("\n=== Benefits of Colocation ===");
            System.out.println("- Reduced network hops for joins");
            System.out.println("- Better performance for related data access");
            System.out.println("- Efficient co-located processing");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Native Persistence (15 minutes)

### Exercise 3: Configure Native Persistence

Create `Lab05Persistence.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

public class Lab05Persistence {

    public static void main(String[] args) {
        System.out.println("=== Native Persistence Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("persistent-node");

        // Configure persistence
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Persistent data region
        DataRegionConfiguration persistentRegion = new DataRegionConfiguration();
        persistentRegion.setName("Persistent_Region");
        persistentRegion.setPersistenceEnabled(true);
        persistentRegion.setInitialSize(100L * 1024 * 1024);  // 100 MB
        persistentRegion.setMaxSize(500L * 1024 * 1024);      // 500 MB

        storageCfg.setDefaultDataRegionConfiguration(persistentRegion);

        // Configure WAL
        storageCfg.setWalMode(org.apache.ignite.configuration.WALMode.LOG_ONLY);
        storageCfg.setWalSegmentSize(64 * 1024 * 1024);  // 64 MB

        // Storage paths
        storageCfg.setStoragePath("./ignite-data");
        storageCfg.setWalPath("./ignite-wal");
        storageCfg.setWalArchivePath("./ignite-wal-archive");

        cfg.setDataStorageConfiguration(storageCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate cluster (required for persistent clusters)
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                System.out.println("Activating cluster...");
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            System.out.println("Cluster state: " + ignite.cluster().state());

            // Create persistent cache
            CacheConfiguration<Integer, String> cacheCfg =
                new CacheConfiguration<>("persistentCache");
            cacheCfg.setDataRegionName("Persistent_Region");

            IgniteCache<Integer, String> cache =
                ignite.getOrCreateCache(cacheCfg);

            // Check if data already exists (from previous run)
            if (cache.size() > 0) {
                System.out.println("\nData recovered from disk!");
                System.out.println("Cache size: " + cache.size());
                System.out.println("Sample data: " + cache.get(1));
            } else {
                System.out.println("\nNo existing data. Creating new data...");

                // Add data
                for (int i = 1; i <= 100; i++) {
                    cache.put(i, "Persistent-Value-" + i);
                }

                System.out.println("Added 100 entries to persistent cache");
                System.out.println("\nRestart the application to see data recovery!");
            }

            System.out.println("\n=== Persistence Features ===");
            System.out.println("- Data survives node restarts");
            System.out.println("- Write-Ahead Logging (WAL) for durability");
            System.out.println("- Checkpointing for crash recovery");
            System.out.println("- Native disk storage (faster than database)");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 3: Cache Store Integration (15 minutes)

### Exercise 4: Implement Read-Through/Write-Through Cache Store

Create `Lab05CacheStore.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteBiInClosure;

import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import java.util.HashMap;
import java.util.Map;

public class Lab05CacheStore {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Store Integration Lab ===\n");

            // Create cache with store
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("storeCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setReadThrough(true);
            cfg.setWriteThrough(true);
            cfg.setCacheStoreFactory(() -> new SimpleDatabaseStore());

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            System.out.println("=== Write-Through Test ===");
            cache.put(1, "Value 1");  // Writes to cache AND database
            cache.put(2, "Value 2");
            cache.put(3, "Value 3");
            System.out.println("Data written to cache and database");

            // Clear cache (but data remains in database)
            cache.clear();
            System.out.println("\nCache cleared (data still in database)");

            System.out.println("\n=== Read-Through Test ===");
            String value1 = cache.get(1);  // Reads from database if not in cache
            System.out.println("Read value: " + value1);
            System.out.println("Data loaded from database to cache");

            System.out.println("\n=== Cache Store Pattern ===");
            System.out.println("Read-Through: Load from DB on cache miss");
            System.out.println("Write-Through: Write to DB immediately");
            System.out.println("Write-Behind: Batch write to DB asynchronously");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simple in-memory database simulation
    private static class SimpleDatabaseStore extends CacheStoreAdapter<Integer, String> {
        private static final Map<Integer, String> database = new HashMap<>();

        @Override
        public String load(Integer key) throws CacheLoaderException {
            System.out.println("  [DB] Loading key: " + key);
            return database.get(key);
        }

        @Override
        public void write(Cache.Entry<? extends Integer, ? extends String> entry)
                throws CacheWriterException {
            System.out.println("  [DB] Writing: " + entry.getKey() +
                " = " + entry.getValue());
            database.put(entry.getKey(), entry.getValue());
        }

        @Override
        public void delete(Object key) throws CacheWriterException {
            System.out.println("  [DB] Deleting key: " + key);
            database.remove(key);
        }
    }
}
```

### Exercise 5: Write-Behind Cache Store

Create `Lab05WriteBehind.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab05WriteBehind {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Write-Behind Cache Store Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("writeBehindCache");

            cfg.setReadThrough(true);
            cfg.setWriteThrough(true);
            cfg.setCacheStoreFactory(() -> new CountingCacheStore());

            // Configure write-behind
            cfg.setWriteBehindEnabled(true);
            cfg.setWriteBehindFlushSize(10);        // Flush after 10 entries
            cfg.setWriteBehindFlushFrequency(5000); // Or every 5 seconds
            cfg.setWriteBehindBatchSize(5);         // Batch 5 entries per write

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            System.out.println("Writing 20 entries to cache...");
            long startTime = System.currentTimeMillis();

            for (int i = 1; i <= 20; i++) {
                cache.put(i, "Value-" + i);
            }

            long cacheTime = System.currentTimeMillis() - startTime;
            System.out.println("Cache writes completed in: " + cacheTime + " ms");

            System.out.println("\nWaiting for write-behind to flush to database...");
            Thread.sleep(6000);  // Wait for flush

            System.out.println("\n=== Write-Behind Benefits ===");
            System.out.println("- Non-blocking cache writes");
            System.out.println("- Batch database updates");
            System.out.println("- Better performance for write-heavy workloads");
            System.out.println("- Configurable flush frequency and size");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class CountingCacheStore extends CacheStoreAdapter<Integer, String> {
        private static final ConcurrentHashMap<Integer, String> db = new ConcurrentHashMap<>();
        private static final AtomicInteger writeCount = new AtomicInteger(0);

        @Override
        public String load(Integer key) throws CacheLoaderException {
            return db.get(key);
        }

        @Override
        public void write(Cache.Entry<? extends Integer, ? extends String> entry)
                throws CacheWriterException {
            int count = writeCount.incrementAndGet();
            System.out.println("  [DB Write #" + count + "] " +
                entry.getKey() + " = " + entry.getValue());
            db.put(entry.getKey(), entry.getValue());

            // Simulate slow database write
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void delete(Object key) throws CacheWriterException {
            db.remove(key);
        }
    }
}
```

## Verification Steps

### Checklist
- [ ] Affinity keys implemented correctly
- [ ] Related data colocated on same nodes
- [ ] Native persistence configured and working
- [ ] Data survives node restart
- [ ] Read-through cache store implemented
- [ ] Write-through cache store working
- [ ] Write-behind demonstrates batching

### Test Data Colocation

```bash
# Start two nodes and verify orders are colocated with customers
# The affinity verification in Exercise 2 will show this
```

### Test Persistence

```bash
# Run Lab05Persistence twice
# First run: Creates data
# Second run: Recovers data from disk
```

## Lab Questions

1. What is an affinity key and why is it important?
2. What is the difference between write-through and write-behind?
3. When should you use native persistence vs cache stores?
4. What is the Write-Ahead Log (WAL) and why is it needed?

## Answers

1. **Affinity key** determines which node stores the data. Using @AffinityKeyMapped ensures related data (like Customer and Orders) are stored on the same node, reducing network overhead for joins and co-located processing.

2. **Write-through**: Synchronous write to database (slower but immediate consistency). **Write-behind**: Asynchronous batched writes (faster but eventual consistency).

3. **Native persistence**: When you want Ignite as primary data store, need full ACID compliance, or want fastest performance. **Cache stores**: When integrating with existing database, need to keep external DB synchronized, or using Ignite as cache layer.

4. **WAL (Write-Ahead Log)** records all changes before applying them, ensuring durability and crash recovery. If a node crashes, changes can be replayed from WAL.

## Common Issues

**Issue: Data not colocated**
- Verify @AffinityKeyMapped annotation
- Check that affinity key field matches
- Ensure consistent cache configuration

**Issue: Persistence not working**
- Must activate cluster for persistent regions
- Check file permissions on storage paths
- Verify persistence enabled in data region config

**Issue: Write-behind not flushing**
- Check flush frequency and size configuration
- Ensure cache store is properly configured
- Look for exceptions in logs

## Next Steps

In Lab 6, you will:
- Execute SQL queries on Ignite caches
- Create and optimize indexes
- Use JDBC driver
- Understand distributed joins

## Completion

You have completed Lab 5 when you can:
- Implement proper data models with affinity keys
- Configure and use native persistence
- Implement all cache store patterns
- Understand trade-offs between different approaches
- Demonstrate data colocation benefits
