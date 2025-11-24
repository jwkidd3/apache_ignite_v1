# Lab 10: Integration and Connectivity

## Duration: 60 minutes

## Objectives
- Use Ignite REST API and web console
- Integrate with Spring Framework
- Configure Kafka connector for streaming data
- Set up Hibernate L2 cache integration
- Understand client vs server node configurations

## Prerequisites
- Completed Labs 1-9
- Spring Framework knowledge (helpful)
- Basic understanding of Kafka and Hibernate

## Part 1: REST API Usage (15 minutes)

### Exercise 1: Enable and Use REST API

Add dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-rest-http</artifactId>
    <version>2.16.0</version>
</dependency>
```

Create `Lab10RestAPI.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

public class Lab10RestAPI {

    public static void main(String[] args) {
        System.out.println("=== REST API Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("rest-node");

        // Enable REST connector
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();
        connectorCfg.setPort(8080);
        cfg.setConnectorConfiguration(connectorCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite node started with REST API enabled");
            System.out.println("REST endpoint: http://localhost:8080/ignite\n");

            // Create sample cache
            CacheConfiguration<String, String> cacheCfg =
                new CacheConfiguration<>("restCache");
            IgniteCache<String, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Add sample data
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("user:john", "{\"name\":\"John\",\"age\":30}");

            System.out.println("Sample data added to cache\n");

            System.out.println("=== REST API Endpoints ===\n");

            System.out.println("1. Get Version:");
            System.out.println("   curl http://localhost:8080/ignite?cmd=version\n");

            System.out.println("2. Get Cache Value:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=get&cacheName=restCache&key=key1\"\n");

            System.out.println("3. Put Cache Value:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=put&cacheName=restCache&key=key3&val=value3\"\n");

            System.out.println("4. Get All Keys:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=getall&cacheName=restCache&k1=key1&k2=key2\"\n");

            System.out.println("5. Execute SQL Query:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=qryfldexe&pageSize=10&cacheName=restCache&qry=SELECT+*+FROM+String\"\n");

            System.out.println("6. Get Cluster Topology:");
            System.out.println("   curl http://localhost:8080/ignite?cmd=top\n");

            System.out.println("7. Cache Size:");
            System.out.println("   curl \"http://localhost:8080/ignite?cmd=size&cacheName=restCache\"\n");

            System.out.println("=== Testing REST API ===");
            System.out.println("Try the curl commands above in another terminal\n");

            System.out.println("=== REST API Use Cases ===");
            System.out.println("- Multi-language client access");
            System.out.println("- Web applications");
            System.out.println("- Monitoring and management");
            System.out.println("- Quick prototyping");
            System.out.println("- Integration with non-Java systems");

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Spring Framework Integration (20 minutes)

### Exercise 2: Spring Boot Integration

Already covered in Lab 4, here's an advanced example.

Add dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>2.7.14</version>
</dependency>
```

Create `Lab10SpringService.java`:

```java
package com.example.ignite.service;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class Lab10SpringService {

    @Autowired
    private Ignite ignite;

    // Spring cache annotations with Ignite
    @Cacheable(value = "userCache", key = "#userId")
    public String getUserName(int userId) {
        System.out.println("Fetching user " + userId + " from database...");
        // Simulate database call
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "User-" + userId;
    }

    public void putToCache(String cacheName, Object key, Object value) {
        IgniteCache<Object, Object> cache = ignite.cache(cacheName);
        if (cache == null) {
            cache = ignite.getOrCreateCache(cacheName);
        }
        cache.put(key, value);
    }

    public Object getFromCache(String cacheName, Object key) {
        IgniteCache<Object, Object> cache = ignite.cache(cacheName);
        return cache != null ? cache.get(key) : null;
    }

    public int getCacheSize(String cacheName) {
        IgniteCache<Object, Object> cache = ignite.cache(cacheName);
        return cache != null ? cache.size() : 0;
    }
}
```

Create `Lab10SpringController.java`:

```java
package com.example.ignite.controller;

import com.example.ignite.service.Lab10SpringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/spring")
public class Lab10SpringController {

    @Autowired
    private Lab10SpringService service;

    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable int id) {
        long start = System.currentTimeMillis();
        String userName = service.getUserName(id);
        long time = System.currentTimeMillis() - start;

        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("userName", userName);
        response.put("responseTime", time + "ms");
        response.put("cached", time < 100);  // If < 100ms, likely from cache

        return response;
    }

    @PostMapping("/cache/{cacheName}")
    public Map<String, String> putCache(
            @PathVariable String cacheName,
            @RequestParam String key,
            @RequestParam String value) {

        service.putToCache(cacheName, key, value);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("cacheName", cacheName);
        response.put("key", key);

        return response;
    }

    @GetMapping("/cache/{cacheName}/{key}")
    public Map<String, Object> getCache(
            @PathVariable String cacheName,
            @PathVariable String key) {

        Object value = service.getFromCache(cacheName, key);

        Map<String, Object> response = new HashMap<>();
        response.put("cacheName", cacheName);
        response.put("key", key);
        response.put("value", value);

        return response;
    }
}
```

## Part 3: Kafka Connector (15 minutes)

### Exercise 3: Stream Data from Kafka

Add dependencies:

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-kafka</artifactId>
    <version>2.16.0</version>
</dependency>

<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.4.0</version>
</dependency>
```

Create `Lab10KafkaStreamer.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.stream.StreamSingleTupleExtractor;
import org.apache.ignite.stream.kafka.KafkaStreamer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Lab10KafkaStreamer {

    public static void main(String[] args) {
        System.out.println("=== Kafka Connector Lab ===\n");

        try (Ignite ignite = Ignition.start()) {
            // Create cache
            CacheConfiguration<String, String> cacheCfg =
                new CacheConfiguration<>("kafkaCache");
            IgniteCache<String, String> cache = ignite.getOrCreateCache(cacheCfg);

            System.out.println("Cache created: kafkaCache\n");

            // Configure Kafka consumer properties
            Properties kafkaProps = new Properties();
            kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ignite-group");
            kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
            kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
            kafkaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            // Create Kafka streamer
            try (KafkaStreamer<String, String, String> kafkaStreamer =
                     new KafkaStreamer<>()) {

                // Set Ignite instance
                kafkaStreamer.setIgnite(ignite);

                // Set streamer configuration
                kafkaStreamer.setStreamer(ignite.dataStreamer("kafkaCache"));

                // Set topic
                kafkaStreamer.setTopic("ignite-topic");

                // Set thread count
                kafkaStreamer.setThreads(4);

                // Set Kafka consumer properties
                kafkaStreamer.setConsumerConfig(kafkaProps);

                // Set tuple extractor
                kafkaStreamer.setSingleTupleExtractor(new StreamSingleTupleExtractor<String, String, String>() {
                    @Override
                    public Map.Entry<String, String> extract(String msg) {
                        // Parse message and create cache entry
                        // Format: key:value
                        String[] parts = msg.split(":", 2);
                        if (parts.length == 2) {
                            return new HashMap.SimpleEntry<>(parts[0], parts[1]);
                        }
                        return null;
                    }
                });

                System.out.println("Starting Kafka streamer...");
                System.out.println("Listening to topic: ignite-topic");
                System.out.println("Kafka broker: localhost:9092\n");

                kafkaStreamer.start();

                System.out.println("=== To Test ===");
                System.out.println("1. Start Kafka: bin/kafka-server-start.sh config/server.properties");
                System.out.println("2. Create topic: bin/kafka-topics.sh --create --topic ignite-topic --bootstrap-server localhost:9092");
                System.out.println("3. Send messages: bin/kafka-console-producer.sh --topic ignite-topic --bootstrap-server localhost:9092");
                System.out.println("   Example messages:");
                System.out.println("   user1:John Doe");
                System.out.println("   user2:Jane Smith");
                System.out.println("\n4. Check cache:");

                // Monitor cache for updates
                Thread monitorThread = new Thread(() -> {
                    int lastSize = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            int currentSize = cache.size();
                            if (currentSize != lastSize) {
                                System.out.println("\n[UPDATE] Cache size: " + currentSize);
                                System.out.println("Latest entries:");
                                cache.forEach(entry ->
                                    System.out.println("  " + entry.getKey() + " = " + entry.getValue()));
                                lastSize = currentSize;
                            }
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });

                monitorThread.start();

                System.out.println("\nStreamer running. Press Enter to stop...");
                System.in.read();

                monitorThread.interrupt();
                kafkaStreamer.stop();

                System.out.println("\nFinal cache size: " + cache.size());
            }

            System.out.println("\n=== Kafka Integration Benefits ===");
            System.out.println("- Real-time data ingestion");
            System.out.println("- Streaming analytics");
            System.out.println("- Event-driven architecture");
            System.out.println("- Scalable data pipeline");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 4: Hibernate L2 Cache (10 minutes)

### Exercise 4: Configure Hibernate L2 Cache

Add dependencies:

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-hibernate_5.3</artifactId>
    <version>2.16.0</version>
</dependency>

<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-core</artifactId>
    <version>5.6.15.Final</version>
</dependency>
```

Create `hibernate.cfg.xml`:

```xml
<?xml version='1.0' encoding='utf-8'?>
<!DOCTYPE hibernate-configuration PUBLIC
    "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
    "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">

<hibernate-configuration>
    <session-factory>
        <!-- Database connection settings -->
        <property name="connection.driver_class">org.h2.Driver</property>
        <property name="connection.url">jdbc:h2:mem:testdb</property>
        <property name="connection.username">sa</property>
        <property name="connection.password"></property>

        <!-- SQL dialect -->
        <property name="dialect">org.hibernate.dialect.H2Dialect</property>

        <!-- Echo SQL to stdout -->
        <property name="show_sql">true</property>

        <!-- Drop and re-create the database schema on startup -->
        <property name="hbm2ddl.auto">create</property>

        <!-- Enable Ignite L2 cache -->
        <property name="cache.use_second_level_cache">true</property>
        <property name="cache.use_query_cache">true</property>
        <property name="cache.region.factory_class">
            org.apache.ignite.cache.hibernate.HibernateRegionFactory
        </property>

        <!-- Ignite configuration file -->
        <property name="org.apache.ignite.hibernate.ignite_instance_name">hibernate-grid</property>
        <property name="org.apache.ignite.hibernate.default_access_type">READ_WRITE</property>
    </session-factory>
</hibernate-configuration>
```

Create entity:

```java
package com.example.ignite.entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table(name = "products")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;

    // Constructors, getters, setters

    public Product() {}

    public Product(String name, Double price) {
        this.name = name;
        this.price = price;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', price=" + price + "}";
    }
}
```

Create `Lab10HibernateL2Cache.java`:

```java
package com.example.ignite;

import com.example.ignite.entity.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

public class Lab10HibernateL2Cache {

    public static void main(String[] args) {
        System.out.println("=== Hibernate L2 Cache Lab ===\n");

        // Build session factory
        SessionFactory sessionFactory = new Configuration()
            .configure("hibernate.cfg.xml")
            .addAnnotatedClass(Product.class)
            .buildSessionFactory();

        // Enable statistics
        sessionFactory.getStatistics().setStatisticsEnabled(true);

        try {
            // Insert data
            System.out.println("=== Inserting Products ===");
            Session session = sessionFactory.openSession();
            session.beginTransaction();

            Product p1 = new Product("Laptop", 1200.0);
            Product p2 = new Product("Mouse", 25.0);
            Product p3 = new Product("Keyboard", 75.0);

            session.save(p1);
            session.save(p2);
            session.save(p3);

            session.getTransaction().commit();
            session.close();

            System.out.println("Products inserted\n");

            // First query - hits database
            System.out.println("=== First Query (Database) ===");
            session = sessionFactory.openSession();
            Product product1 = session.get(Product.class, 1L);
            System.out.println("Retrieved: " + product1);
            session.close();

            // Second query - should hit L2 cache
            System.out.println("\n=== Second Query (L2 Cache) ===");
            session = sessionFactory.openSession();
            Product product2 = session.get(Product.class, 1L);
            System.out.println("Retrieved: " + product2);
            session.close();

            // Print statistics
            Statistics stats = sessionFactory.getStatistics();
            System.out.println("\n=== Cache Statistics ===");
            System.out.println("Entity fetch count: " + stats.getEntityFetchCount());
            System.out.println("Second level cache hit count: " +
                stats.getSecondLevelCacheHitCount());
            System.out.println("Second level cache miss count: " +
                stats.getSecondLevelCacheMissCount());
            System.out.println("Second level cache put count: " +
                stats.getSecondLevelCachePutCount());

            System.out.println("\n=== Hibernate L2 Cache Benefits ===");
            System.out.println("- Reduced database load");
            System.out.println("- Better application performance");
            System.out.println("- Distributed caching");
            System.out.println("- Cluster-wide cache");
            System.out.println("- Automatic cache synchronization");

        } finally {
            sessionFactory.close();
        }
    }
}
```

## Verification Steps

### Checklist
- [ ] REST API responds to requests
- [ ] Spring integration working correctly
- [ ] Kafka streamer receives and caches messages
- [ ] Hibernate L2 cache reduces database queries
- [ ] Client nodes can connect to cluster

### Test REST API

```bash
# Get version
curl http://localhost:8080/ignite?cmd=version

# Put value
curl "http://localhost:8080/ignite?cmd=put&cacheName=testCache&key=test&val=hello"

# Get value
curl "http://localhost:8080/ignite?cmd=get&cacheName=testCache&key=test"
```

## Lab Questions

1. What are the advantages of using REST API for Ignite?
2. How does Hibernate L2 cache improve application performance?
3. What is the benefit of Kafka integration with Ignite?
4. When should you use client nodes vs server nodes?

## Answers

1. **REST API advantages**:
   - Language-agnostic access
   - Easy integration with web applications
   - Simple HTTP-based protocol
   - No Ignite client library needed
   - Good for monitoring and management

2. **Hibernate L2 cache** benefits:
   - Reduces database queries (second query from cache)
   - Distributed across cluster nodes
   - Automatic synchronization
   - Transparent to application code
   - Better scalability

3. **Kafka + Ignite** benefits:
   - Real-time streaming data ingestion
   - Event-driven architecture
   - Scalable data pipeline
   - Persistent queue + fast cache
   - Analytics on streaming data

4. **Client vs Server nodes**:
   - **Server nodes**: Store data, participate in compute
   - **Client nodes**: Don't store data, lighter weight, good for application servers
   - Use clients when you don't want application servers storing data

## Common Issues

**Issue: REST API not responding**
- Check ConnectorConfiguration enabled
- Verify port not in use
- Check firewall settings

**Issue: Hibernate L2 cache not working**
- Verify @Cacheable annotation
- Check hibernate configuration
- Enable statistics to debug

**Issue: Kafka messages not streaming**
- Ensure Kafka broker running
- Verify topic exists
- Check consumer properties

## Next Steps

In Lab 11, you will:
- Tune JVM for Ignite workloads
- Optimize memory management
- Monitor performance metrics
- Benchmark and load test
- Identify and fix performance issues

## Completion

You have completed Lab 10 when you can:
- Access Ignite via REST API
- Integrate with Spring Framework
- Stream data from Kafka
- Use Hibernate L2 cache
- Configure client and server nodes appropriately
