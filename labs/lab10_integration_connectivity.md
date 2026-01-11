# Lab 10: Integration and Connectivity

## Duration: 55 minutes

## Objectives
- Use Ignite REST API
- Integrate with Spring Framework
- Set up Hibernate L2 cache integration

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

## Part 3: Hibernate L2 Cache (10 minutes)

### Exercise 3: Configure Hibernate L2 Cache

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

---

## Verification Steps

### Checklist
- [ ] REST API enabled and accessible
- [ ] Basic REST operations (get, put) working
- [ ] Spring Framework integration configured
- [ ] Hibernate L2 cache integration working

### Common Issues

**Issue: REST API not responding**
- Ensure ignite-rest-http dependency is included
- Check node is running and accessible

**Issue: Spring integration errors**
- Verify Spring Boot version compatibility
- Check autoconfiguration is enabled

## Lab Questions

1. What is the default port for Ignite REST API?
2. How does Hibernate L2 cache improve performance?
3. What is the difference between thick and thin clients?

## Answers

1. Default REST API port is **8080** (configurable via jettyPort).

2. Hibernate L2 cache reduces database queries by caching entities across sessions, with Ignite providing distributed caching across nodes.

3. **Thick clients** are full cluster members (receive topology updates), while **thin clients** are lightweight connections that don't participate in data distribution.

## Next Steps

In Lab 11, you will:
- Learn performance tuning techniques
- Configure memory and storage optimization
- Implement monitoring and diagnostics

## Completion

You have completed Lab 10 when you can:
- Use Ignite REST API for basic operations
- Configure Spring Framework integration
- Set up Hibernate L2 cache

---

## Optional Exercises (If Time Permits)

### Optional: REST API Deep Dive

Create `Lab10RestAPIDeepDive.java` for advanced REST operations:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Lab10RestAPIDeepDive {

    private static final String REST_BASE_URL = "http://localhost:8080/ignite";

    public static void main(String[] args) {
        System.out.println("=== REST API Deep Dive Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("rest-deep-dive-node");

        // Configure REST connector with advanced settings
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();
        connectorCfg.setPort(8080);
        connectorCfg.setIdleTimeout(30000);  // 30 second idle timeout
        connectorCfg.setThreadPoolSize(8);   // 8 threads for REST requests
        cfg.setConnectorConfiguration(connectorCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite REST API node started on port 8080\n");

            // Setup SQL-enabled cache for queries
            setupSqlCache(ignite);

            // Demonstrate cache operations via REST
            demonstrateCacheOperations();

            // Demonstrate SQL queries via REST
            demonstrateSqlQueries();

            // Demonstrate cluster monitoring via REST
            demonstrateClusterMonitoring();

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupSqlCache(Ignite ignite) {
        System.out.println("=== Setting Up SQL-Enabled Cache ===\n");

        // Create cache with SQL schema
        CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>("PersonCache");
        cfg.setIndexedTypes(Long.class, Person.class);
        cfg.setSqlSchema("PUBLIC");

        IgniteCache<Long, Object> cache = ignite.getOrCreateCache(cfg);

        // Insert sample data using SQL
        cache.query(new SqlFieldsQuery(
            "CREATE TABLE IF NOT EXISTS Person (" +
            "id LONG PRIMARY KEY, " +
            "name VARCHAR, " +
            "age INT, " +
            "city VARCHAR)"
        )).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city) VALUES (?, ?, ?, ?)")
            .setArgs(1L, "John Doe", 30, "New York")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city) VALUES (?, ?, ?, ?)")
            .setArgs(2L, "Jane Smith", 25, "Los Angeles")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city) VALUES (?, ?, ?, ?)")
            .setArgs(3L, "Bob Johnson", 35, "Chicago")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city) VALUES (?, ?, ?, ?)")
            .setArgs(4L, "Alice Brown", 28, "New York")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city) VALUES (?, ?, ?, ?)")
            .setArgs(5L, "Charlie Wilson", 42, "Boston")).getAll();

        System.out.println("Created Person table with 5 records\n");
    }

    private static void demonstrateCacheOperations() throws Exception {
        System.out.println("=== Cache Operations via REST ===\n");

        // 1. PUT operation
        System.out.println("1. PUT - Adding data to cache:");
        String putUrl = REST_BASE_URL + "?cmd=put&cacheName=myCache&key=product1&val=Laptop";
        String putResult = executeRestCall(putUrl);
        System.out.println("   PUT product1=Laptop: " + putResult);

        putUrl = REST_BASE_URL + "?cmd=put&cacheName=myCache&key=product2&val=Mouse";
        putResult = executeRestCall(putUrl);
        System.out.println("   PUT product2=Mouse: " + putResult);

        putUrl = REST_BASE_URL + "?cmd=put&cacheName=myCache&key=product3&val=Keyboard";
        putResult = executeRestCall(putUrl);
        System.out.println("   PUT product3=Keyboard: " + putResult);

        // 2. GET operation
        System.out.println("\n2. GET - Retrieving data from cache:");
        String getUrl = REST_BASE_URL + "?cmd=get&cacheName=myCache&key=product1";
        String getResult = executeRestCall(getUrl);
        System.out.println("   GET product1: " + getResult);

        // 3. GETALL operation
        System.out.println("\n3. GETALL - Retrieving multiple keys:");
        String getAllUrl = REST_BASE_URL + "?cmd=getall&cacheName=myCache&k1=product1&k2=product2&k3=product3";
        String getAllResult = executeRestCall(getAllUrl);
        System.out.println("   GETALL: " + getAllResult);

        // 4. PUTALL operation
        System.out.println("\n4. PUTALL - Adding multiple entries:");
        String putAllUrl = REST_BASE_URL + "?cmd=putall&cacheName=myCache&k1=item1&v1=Value1&k2=item2&v2=Value2";
        String putAllResult = executeRestCall(putAllUrl);
        System.out.println("   PUTALL: " + putAllResult);

        // 5. CONTAINS operation
        System.out.println("\n5. CONTAINS - Check if key exists:");
        String containsUrl = REST_BASE_URL + "?cmd=conkey&cacheName=myCache&key=product1";
        String containsResult = executeRestCall(containsUrl);
        System.out.println("   CONTAINS product1: " + containsResult);

        // 6. SIZE operation
        System.out.println("\n6. SIZE - Get cache size:");
        String sizeUrl = REST_BASE_URL + "?cmd=size&cacheName=myCache";
        String sizeResult = executeRestCall(sizeUrl);
        System.out.println("   SIZE: " + sizeResult);

        // 7. REMOVE operation
        System.out.println("\n7. REMOVE - Delete from cache:");
        String removeUrl = REST_BASE_URL + "?cmd=rmv&cacheName=myCache&key=item1";
        String removeResult = executeRestCall(removeUrl);
        System.out.println("   REMOVE item1: " + removeResult);

        // 8. CAS (Compare-And-Set) operation
        System.out.println("\n8. CAS - Atomic compare-and-set:");
        String casUrl = REST_BASE_URL + "?cmd=cas&cacheName=myCache&key=product1&val=Gaming_Laptop&exp=Laptop";
        String casResult = executeRestCall(casUrl);
        System.out.println("   CAS (Laptop -> Gaming_Laptop): " + casResult);

        // Verify CAS worked
        getUrl = REST_BASE_URL + "?cmd=get&cacheName=myCache&key=product1";
        getResult = executeRestCall(getUrl);
        System.out.println("   Verified new value: " + getResult);

        System.out.println();
    }

    private static void demonstrateSqlQueries() throws Exception {
        System.out.println("=== SQL Queries via REST ===\n");

        // 1. SELECT all records
        System.out.println("1. SELECT * FROM Person:");
        String query = URLEncoder.encode("SELECT * FROM Person", StandardCharsets.UTF_8.toString());
        String sqlUrl = REST_BASE_URL + "?cmd=qryfldexe&pageSize=100&cacheName=PersonCache&qry=" + query;
        String sqlResult = executeRestCall(sqlUrl);
        System.out.println("   Result: " + formatJsonResponse(sqlResult));

        // 2. SELECT with WHERE clause
        System.out.println("\n2. SELECT with WHERE clause (age > 30):");
        query = URLEncoder.encode("SELECT name, age, city FROM Person WHERE age > 30", StandardCharsets.UTF_8.toString());
        sqlUrl = REST_BASE_URL + "?cmd=qryfldexe&pageSize=100&cacheName=PersonCache&qry=" + query;
        sqlResult = executeRestCall(sqlUrl);
        System.out.println("   Result: " + formatJsonResponse(sqlResult));

        // 3. SELECT with aggregation
        System.out.println("\n3. Aggregate query (AVG age):");
        query = URLEncoder.encode("SELECT AVG(age) as avg_age FROM Person", StandardCharsets.UTF_8.toString());
        sqlUrl = REST_BASE_URL + "?cmd=qryfldexe&pageSize=100&cacheName=PersonCache&qry=" + query;
        sqlResult = executeRestCall(sqlUrl);
        System.out.println("   Result: " + formatJsonResponse(sqlResult));

        // 4. SELECT with GROUP BY
        System.out.println("\n4. GROUP BY city (count per city):");
        query = URLEncoder.encode("SELECT city, COUNT(*) as person_count FROM Person GROUP BY city", StandardCharsets.UTF_8.toString());
        sqlUrl = REST_BASE_URL + "?cmd=qryfldexe&pageSize=100&cacheName=PersonCache&qry=" + query;
        sqlResult = executeRestCall(sqlUrl);
        System.out.println("   Result: " + formatJsonResponse(sqlResult));

        // 5. SELECT with ORDER BY
        System.out.println("\n5. ORDER BY age DESC:");
        query = URLEncoder.encode("SELECT name, age FROM Person ORDER BY age DESC", StandardCharsets.UTF_8.toString());
        sqlUrl = REST_BASE_URL + "?cmd=qryfldexe&pageSize=100&cacheName=PersonCache&qry=" + query;
        sqlResult = executeRestCall(sqlUrl);
        System.out.println("   Result: " + formatJsonResponse(sqlResult));

        // 6. UPDATE via SQL
        System.out.println("\n6. UPDATE statement:");
        query = URLEncoder.encode("UPDATE Person SET age = 31 WHERE name = 'John Doe'", StandardCharsets.UTF_8.toString());
        sqlUrl = REST_BASE_URL + "?cmd=qryfldexe&pageSize=100&cacheName=PersonCache&qry=" + query;
        sqlResult = executeRestCall(sqlUrl);
        System.out.println("   UPDATE result: " + formatJsonResponse(sqlResult));

        System.out.println();
    }

    private static void demonstrateClusterMonitoring() throws Exception {
        System.out.println("=== Cluster Monitoring via REST ===\n");

        // 1. Get cluster version
        System.out.println("1. Cluster Version:");
        String versionUrl = REST_BASE_URL + "?cmd=version";
        String versionResult = executeRestCall(versionUrl);
        System.out.println("   " + formatJsonResponse(versionResult));

        // 2. Get cluster topology
        System.out.println("\n2. Cluster Topology:");
        String topUrl = REST_BASE_URL + "?cmd=top";
        String topResult = executeRestCall(topUrl);
        System.out.println("   " + formatJsonResponse(topResult));

        // 3. Get node information
        System.out.println("\n3. Node Information:");
        String nodeUrl = REST_BASE_URL + "?cmd=node&id=local";
        String nodeResult = executeRestCall(nodeUrl);
        System.out.println("   " + formatJsonResponse(nodeResult));

        // 4. Get cache names
        System.out.println("\n4. Cache Names:");
        String cacheNamesUrl = REST_BASE_URL + "?cmd=name";
        String cacheNamesResult = executeRestCall(cacheNamesUrl);
        System.out.println("   " + formatJsonResponse(cacheNamesResult));

        // 5. Get cache metrics
        System.out.println("\n5. Cache Metrics (myCache):");
        String metricsUrl = REST_BASE_URL + "?cmd=cache&cacheName=myCache";
        String metricsResult = executeRestCall(metricsUrl);
        System.out.println("   " + formatJsonResponse(metricsResult));

        // 6. Log messages
        System.out.println("\n6. Log command (adds log entry):");
        String logUrl = REST_BASE_URL + "?cmd=log&msg=REST+API+monitoring+test";
        String logResult = executeRestCall(logUrl);
        System.out.println("   " + logResult);

        System.out.println("\n=== REST API Monitoring Endpoints Summary ===");
        System.out.println("- version     : Get Ignite version");
        System.out.println("- top         : Get cluster topology");
        System.out.println("- node        : Get specific node info");
        System.out.println("- name        : List all cache names");
        System.out.println("- cache       : Get cache configuration/metrics");
        System.out.println("- log         : Add log message");
        System.out.println("- exe         : Execute compute task");
    }

    private static String executeRestCall(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                return "Error: HTTP " + responseCode;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private static String formatJsonResponse(String json) {
        // Simple truncation for display
        if (json.length() > 200) {
            return json.substring(0, 200) + "...";
        }
        return json;
    }

    // Person class for SQL queries
    public static class Person implements java.io.Serializable {
        private Long id;
        private String name;
        private int age;
        private String city;

        public Person() {}

        public Person(Long id, String name, int age, String city) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.city = city;
        }

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }
}
```

### REST API Quick Reference

```bash
# Cache Operations
curl "http://localhost:8080/ignite?cmd=put&cacheName=test&key=k1&val=v1"
curl "http://localhost:8080/ignite?cmd=get&cacheName=test&key=k1"
curl "http://localhost:8080/ignite?cmd=getall&cacheName=test&k1=key1&k2=key2"
curl "http://localhost:8080/ignite?cmd=rmv&cacheName=test&key=k1"
curl "http://localhost:8080/ignite?cmd=size&cacheName=test"

# SQL Queries (URL encode the query)
curl "http://localhost:8080/ignite?cmd=qryfldexe&pageSize=10&cacheName=SQL_PUBLIC_PERSON&qry=SELECT%20*%20FROM%20Person"

# Cluster Monitoring
curl "http://localhost:8080/ignite?cmd=version"
curl "http://localhost:8080/ignite?cmd=top"
curl "http://localhost:8080/ignite?cmd=node&id=local"
curl "http://localhost:8080/ignite?cmd=name"
```

### Optional: Thin Client Implementation

### Exercise 5: Thin Client Connection and Configuration

Thin clients connect to the cluster without joining as a node, reducing resource usage.

Add dependency:

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-core</artifactId>
    <version>2.16.0</version>
</dependency>
```

Create `Lab10ThinClient.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.ThinClientKVView;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab10ThinClient {

    public static void main(String[] args) {
        System.out.println("=== Thin Client Implementation Lab ===\n");

        // Start server node first
        startServerNode();

        // Wait for server to be ready
        try { Thread.sleep(2000); } catch (InterruptedException e) { }

        // Demonstrate thin client features
        demonstrateBasicConnection();
        demonstrateConnectionPooling();
        demonstrateReconnection();
        compareThickVsThin();

        System.out.println("\n=== Lab Complete ===");
        System.out.println("Press Enter to stop server...");
        try { System.in.read(); } catch (Exception e) { }
    }

    private static void startServerNode() {
        new Thread(() -> {
            IgniteConfiguration cfg = new IgniteConfiguration();
            cfg.setIgniteInstanceName("thin-client-server");

            // Configure client connector for thin clients
            ClientConnectorConfiguration clientCfg = new ClientConnectorConfiguration();
            clientCfg.setPort(10800);
            clientCfg.setPortRange(10);  // Try ports 10800-10810
            clientCfg.setThreadPoolSize(8);
            clientCfg.setIdleTimeout(30000);
            cfg.setClientConnectorConfiguration(clientCfg);

            Ignite ignite = Ignition.start(cfg);
            System.out.println("Server node started, thin client port: 10800\n");

            // Keep server running
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                ignite.close();
            }
        }).start();
    }

    private static void demonstrateBasicConnection() {
        System.out.println("=== 1. Basic Thin Client Connection ===\n");

        // Configure thin client
        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses("127.0.0.1:10800");

        try (IgniteClient client = Ignition.startClient(cfg)) {
            System.out.println("Connected to cluster via thin client");

            // Create and use cache
            ClientCache<Integer, String> cache = client.getOrCreateCache("thinClientCache");

            // Put data
            cache.put(1, "Hello");
            cache.put(2, "Thin");
            cache.put(3, "Client");

            // Get data
            System.out.println("Retrieved: " + cache.get(1) + " " + cache.get(2) + " " + cache.get(3));

            // Cache size
            System.out.println("Cache size: " + cache.size());

            // PutAll
            Map<Integer, String> batch = new HashMap<>();
            batch.put(4, "Batch1");
            batch.put(5, "Batch2");
            batch.put(6, "Batch3");
            cache.putAll(batch);
            System.out.println("After batch put, size: " + cache.size());

            // GetAll
            Map<Integer, String> results = cache.getAll(
                new java.util.HashSet<>(Arrays.asList(4, 5, 6)));
            System.out.println("GetAll results: " + results);

            // Replace operation
            boolean replaced = cache.replace(1, "Hello", "Hello World");
            System.out.println("Replace succeeded: " + replaced);
            System.out.println("New value: " + cache.get(1));

            // Remove operation
            cache.remove(6);
            System.out.println("After remove, size: " + cache.size());

            System.out.println();

        } catch (ClientException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void demonstrateConnectionPooling() {
        System.out.println("=== 2. Connection Pooling Configuration ===\n");

        // Advanced client configuration with connection pooling
        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses("127.0.0.1:10800", "127.0.0.1:10801")  // Multiple addresses
            .setTcpNoDelay(true)
            .setSendBufferSize(65536)
            .setReceiveBufferSize(65536)
            .setTimeout(5000)                    // Operation timeout
            .setReconnectThrottlingPeriod(30000) // Reconnect throttling
            .setReconnectThrottlingRetries(3)    // Max retries in throttling period
            .setAffinityAwarenessEnabled(true);  // Route to primary node

        System.out.println("Connection Pool Configuration:");
        System.out.println("  - Multiple server addresses for failover");
        System.out.println("  - TCP NoDelay: enabled");
        System.out.println("  - Send/Receive buffer: 64KB");
        System.out.println("  - Operation timeout: 5 seconds");
        System.out.println("  - Affinity awareness: enabled");
        System.out.println("  - Reconnect throttling: 3 retries per 30 seconds");

        try (IgniteClient client = Ignition.startClient(cfg)) {
            System.out.println("\nConnected with pooled configuration");

            // Demonstrate affinity-aware operations
            ClientCache<Integer, String> cache = client.getOrCreateCache("pooledCache");

            System.out.println("\nPerforming 1000 operations with affinity awareness...");
            long start = System.currentTimeMillis();

            for (int i = 0; i < 1000; i++) {
                cache.put(i, "Value-" + i);
            }

            long elapsed = System.currentTimeMillis() - start;
            System.out.println("1000 puts completed in " + elapsed + "ms");
            System.out.println("Average: " + (elapsed / 1000.0) + "ms per operation");

            // Read operations
            start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                cache.get(i);
            }
            elapsed = System.currentTimeMillis() - start;
            System.out.println("1000 gets completed in " + elapsed + "ms");

            System.out.println();

        } catch (ClientException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void demonstrateReconnection() {
        System.out.println("=== 3. Reconnection Handling ===\n");

        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses("127.0.0.1:10800")
            .setReconnectThrottlingPeriod(5000)
            .setReconnectThrottlingRetries(5)
            .setTimeout(10000);

        try (IgniteClient client = Ignition.startClient(cfg)) {
            ClientCache<String, String> cache = client.getOrCreateCache("reconnectCache");

            System.out.println("Demonstrating resilient operations...\n");

            // Simulate operations that might need reconnection
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(4);

            for (int i = 0; i < 100; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        cache.put("key-" + idx, "value-" + idx);
                        String value = cache.get("key-" + idx);
                        if (value != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (ClientException e) {
                        failCount.incrementAndGet();
                        System.err.println("Operation failed: " + e.getMessage());
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

            System.out.println("Concurrent operations completed:");
            System.out.println("  Successful: " + successCount.get());
            System.out.println("  Failed: " + failCount.get());

            System.out.println("\n=== Reconnection Best Practices ===");
            System.out.println("1. Configure multiple server addresses");
            System.out.println("2. Set appropriate timeouts");
            System.out.println("3. Use reconnect throttling to prevent storms");
            System.out.println("4. Implement retry logic in application code");
            System.out.println("5. Monitor connection state changes");

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void compareThickVsThin() {
        System.out.println("=== 4. Thick vs Thin Client Comparison ===\n");

        System.out.println("+------------------+------------------------+------------------------+");
        System.out.println("| Feature          | Thick Client           | Thin Client            |");
        System.out.println("+------------------+------------------------+------------------------+");
        System.out.println("| Joins Cluster    | Yes (as client node)   | No (standalone)        |");
        System.out.println("| Resources        | Higher (JVM overhead)  | Lower (lightweight)    |");
        System.out.println("| Partition Aware  | Yes (built-in)         | Yes (affinity aware)   |");
        System.out.println("| Near Cache       | Yes                    | No                     |");
        System.out.println("| Compute Tasks    | Yes                    | No                     |");
        System.out.println("| Services         | Yes                    | No                     |");
        System.out.println("| Transactions     | Full ACID              | Limited                |");
        System.out.println("| Connection       | Discovery-based        | Direct TCP             |");
        System.out.println("| Protocol         | Binary internal        | Thin client protocol   |");
        System.out.println("| Use Case         | Full feature access    | Simple CRUD ops        |");
        System.out.println("+------------------+------------------------+------------------------+");

        System.out.println("\n=== When to Use Thin Client ===");
        System.out.println("- Microservices with simple cache operations");
        System.out.println("- Non-Java applications (via thin client protocol)");
        System.out.println("- Containerized applications with limited resources");
        System.out.println("- High number of short-lived connections");
        System.out.println("- Applications that don't need compute/services");

        System.out.println("\n=== When to Use Thick Client ===");
        System.out.println("- Need near cache for frequently accessed data");
        System.out.println("- Require distributed compute capabilities");
        System.out.println("- Using Ignite services");
        System.out.println("- Need full transaction support");
        System.out.println("- Long-running applications with stable connections");
    }
}
```

### Thin Client Quick Reference

```java
// Basic connection
ClientConfiguration cfg = new ClientConfiguration()
    .setAddresses("host1:10800", "host2:10800");
IgniteClient client = Ignition.startClient(cfg);

// With SSL
cfg.setSslMode(SslMode.REQUIRE)
   .setSslClientCertificateKeyStorePath("/path/to/keystore.jks")
   .setSslClientCertificateKeyStorePassword("password")
   .setSslTrustCertificateKeyStorePath("/path/to/truststore.jks")
   .setSslTrustCertificateKeyStorePassword("password");

// Cache operations
ClientCache<K, V> cache = client.cache("name");
cache.put(key, value);
V value = cache.get(key);
cache.putAll(map);
cache.remove(key);
```

### Optional: Spring Data Integration

### Exercise 6: Spring Data Ignite Repositories

Add dependencies to `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-spring-data_2.2</artifactId>
    <version>2.16.0</version>
</dependency>

<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-commons</artifactId>
    <version>2.7.14</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <version>2.7.14</version>
</dependency>
```

Create domain entity `Employee.java`:

```java
package com.example.ignite.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;
import java.io.Serializable;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    @QuerySqlField(index = true)
    private Long id;

    @QuerySqlField
    private String firstName;

    @QuerySqlField
    private String lastName;

    @QuerySqlField(index = true)
    private String department;

    @QuerySqlField
    private Double salary;

    @QuerySqlField(index = true)
    private String email;

    public Employee() {}

    public Employee(Long id, String firstName, String lastName,
                    String department, Double salary, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.salary = salary;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + firstName + " " + lastName +
               "', dept='" + department + "', salary=" + salary + "}";
    }
}
```

Create repository `EmployeeRepository.java`:

```java
package com.example.ignite.repository;

import com.example.ignite.model.Employee;
import org.apache.ignite.springdata22.repository.IgniteRepository;
import org.apache.ignite.springdata22.repository.config.Query;
import org.apache.ignite.springdata22.repository.config.RepositoryConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RepositoryConfig(cacheName = "EmployeeCache")
public interface EmployeeRepository extends IgniteRepository<Employee, Long> {

    // Spring Data derived query methods
    List<Employee> findByDepartment(String department);

    List<Employee> findByLastName(String lastName);

    List<Employee> findByDepartmentAndSalaryGreaterThan(String department, Double salary);

    List<Employee> findByFirstNameContaining(String name);

    List<Employee> findBySalaryBetween(Double minSalary, Double maxSalary);

    List<Employee> findByDepartmentOrderBySalaryDesc(String department);

    // Custom SQL queries
    @Query("SELECT * FROM Employee WHERE salary > ?")
    List<Employee> findHighEarners(Double minSalary);

    @Query("SELECT * FROM Employee WHERE department = ? ORDER BY salary DESC")
    List<Employee> findByDepartmentSortedBySalary(String department);

    @Query("SELECT AVG(salary) FROM Employee WHERE department = ?")
    Double getAverageSalaryByDepartment(String department);

    @Query("SELECT department, COUNT(*), AVG(salary) FROM Employee GROUP BY department")
    List<List<?>> getDepartmentStatistics();

    @Query("SELECT * FROM Employee WHERE email LIKE ?")
    List<Employee> findByEmailPattern(String pattern);

    // Count queries
    long countByDepartment(String department);

    // Exists queries
    boolean existsByEmail(String email);

    // Delete queries
    void deleteByDepartment(String department);
}
```

Create Ignite configuration `IgniteConfig.java`:

```java
package com.example.ignite.config;

import com.example.ignite.model.Employee;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.springdata22.repository.config.EnableIgniteRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableIgniteRepositories(basePackages = "com.example.ignite.repository")
public class IgniteConfig {

    @Bean
    public Ignite igniteInstance() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("spring-data-node");
        cfg.setPeerClassLoadingEnabled(true);

        // Configure Employee cache
        CacheConfiguration<Long, Employee> employeeCacheCfg =
            new CacheConfiguration<>("EmployeeCache");
        employeeCacheCfg.setIndexedTypes(Long.class, Employee.class);
        employeeCacheCfg.setSqlSchema("PUBLIC");

        cfg.setCacheConfiguration(employeeCacheCfg);

        return Ignition.start(cfg);
    }
}
```

Create main application `Lab10SpringDataIgnite.java`:

```java
package com.example.ignite;

import com.example.ignite.config.IgniteConfig;
import com.example.ignite.model.Employee;
import com.example.ignite.repository.EmployeeRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Lab10SpringDataIgnite {

    public static void main(String[] args) {
        System.out.println("=== Spring Data Ignite Lab ===\n");

        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(IgniteConfig.class)) {

            // Get repository bean
            EmployeeRepository repository = ctx.getBean(EmployeeRepository.class);

            // Insert sample data
            insertSampleData(repository);

            // Demonstrate repository operations
            demonstrateCrudOperations(repository);
            demonstrateQueryMethods(repository);
            demonstrateCustomQueries(repository);
            demonstrateAggregations(repository);

            System.out.println("\n=== Spring Data Ignite Benefits ===");
            System.out.println("- Familiar Spring Data programming model");
            System.out.println("- Auto-implemented repository methods");
            System.out.println("- Type-safe queries with method naming");
            System.out.println("- Custom SQL query support");
            System.out.println("- Seamless integration with Spring ecosystem");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertSampleData(EmployeeRepository repository) {
        System.out.println("=== Inserting Sample Data ===\n");

        List<Employee> employees = Arrays.asList(
            new Employee(1L, "John", "Doe", "Engineering", 85000.0, "john.doe@company.com"),
            new Employee(2L, "Jane", "Smith", "Engineering", 92000.0, "jane.smith@company.com"),
            new Employee(3L, "Bob", "Johnson", "Sales", 75000.0, "bob.johnson@company.com"),
            new Employee(4L, "Alice", "Williams", "Engineering", 105000.0, "alice.williams@company.com"),
            new Employee(5L, "Charlie", "Brown", "Marketing", 68000.0, "charlie.brown@company.com"),
            new Employee(6L, "Diana", "Davis", "Sales", 82000.0, "diana.davis@company.com"),
            new Employee(7L, "Edward", "Miller", "HR", 65000.0, "edward.miller@company.com"),
            new Employee(8L, "Fiona", "Wilson", "Engineering", 95000.0, "fiona.wilson@company.com"),
            new Employee(9L, "George", "Taylor", "Marketing", 72000.0, "george.taylor@company.com"),
            new Employee(10L, "Helen", "Anderson", "HR", 70000.0, "helen.anderson@company.com")
        );

        for (Employee emp : employees) {
            repository.save(emp.getId(), emp);
        }

        System.out.println("Inserted " + repository.count() + " employees\n");
    }

    private static void demonstrateCrudOperations(EmployeeRepository repository) {
        System.out.println("=== CRUD Operations ===\n");

        // Read by ID
        Optional<Employee> emp = Optional.ofNullable(repository.findById(1L).orElse(null));
        System.out.println("Find by ID 1: " + emp.orElse(null));

        // Update
        Employee john = repository.findById(1L).orElse(null);
        if (john != null) {
            john.setSalary(88000.0);
            repository.save(john.getId(), john);
            System.out.println("Updated John's salary to: " +
                repository.findById(1L).get().getSalary());
        }

        // Count
        System.out.println("Total employees: " + repository.count());

        // Exists
        System.out.println("Employee 1 exists: " + repository.existsById(1L));
        System.out.println("Employee 99 exists: " + repository.existsById(99L));

        System.out.println();
    }

    private static void demonstrateQueryMethods(EmployeeRepository repository) {
        System.out.println("=== Derived Query Methods ===\n");

        // Find by department
        System.out.println("1. Find by Department (Engineering):");
        List<Employee> engineers = repository.findByDepartment("Engineering");
        engineers.forEach(e -> System.out.println("   " + e));

        // Find by department and salary
        System.out.println("\n2. Engineers earning > $90,000:");
        List<Employee> highPaidEngineers =
            repository.findByDepartmentAndSalaryGreaterThan("Engineering", 90000.0);
        highPaidEngineers.forEach(e -> System.out.println("   " + e));

        // Find by salary range
        System.out.println("\n3. Salary between $70,000 and $85,000:");
        List<Employee> midRange = repository.findBySalaryBetween(70000.0, 85000.0);
        midRange.forEach(e -> System.out.println("   " + e));

        // Find by name containing
        System.out.println("\n4. First name containing 'a':");
        List<Employee> namesWithA = repository.findByFirstNameContaining("a");
        namesWithA.forEach(e -> System.out.println("   " + e));

        // Find with ordering
        System.out.println("\n5. Sales team ordered by salary (desc):");
        List<Employee> salesBySalary = repository.findByDepartmentOrderBySalaryDesc("Sales");
        salesBySalary.forEach(e -> System.out.println("   " + e));

        // Count by department
        System.out.println("\n6. Count by department:");
        System.out.println("   Engineering: " + repository.countByDepartment("Engineering"));
        System.out.println("   Sales: " + repository.countByDepartment("Sales"));
        System.out.println("   Marketing: " + repository.countByDepartment("Marketing"));

        // Exists by email
        System.out.println("\n7. Email exists check:");
        System.out.println("   john.doe@company.com exists: " +
            repository.existsByEmail("john.doe@company.com"));
        System.out.println("   unknown@company.com exists: " +
            repository.existsByEmail("unknown@company.com"));

        System.out.println();
    }

    private static void demonstrateCustomQueries(EmployeeRepository repository) {
        System.out.println("=== Custom SQL Queries ===\n");

        // High earners
        System.out.println("1. High earners (salary > $90,000):");
        List<Employee> highEarners = repository.findHighEarners(90000.0);
        highEarners.forEach(e -> System.out.println("   " + e));

        // Department sorted by salary
        System.out.println("\n2. Marketing sorted by salary:");
        List<Employee> marketingSorted =
            repository.findByDepartmentSortedBySalary("Marketing");
        marketingSorted.forEach(e -> System.out.println("   " + e));

        // Email pattern
        System.out.println("\n3. Emails matching pattern '%son%':");
        List<Employee> emailPattern = repository.findByEmailPattern("%son%");
        emailPattern.forEach(e -> System.out.println("   " + e));

        System.out.println();
    }

    private static void demonstrateAggregations(EmployeeRepository repository) {
        System.out.println("=== Aggregation Queries ===\n");

        // Average salary by department
        System.out.println("1. Average salary by department:");
        System.out.println("   Engineering: $" +
            String.format("%.2f", repository.getAverageSalaryByDepartment("Engineering")));
        System.out.println("   Sales: $" +
            String.format("%.2f", repository.getAverageSalaryByDepartment("Sales")));
        System.out.println("   Marketing: $" +
            String.format("%.2f", repository.getAverageSalaryByDepartment("Marketing")));
        System.out.println("   HR: $" +
            String.format("%.2f", repository.getAverageSalaryByDepartment("HR")));

        // Department statistics
        System.out.println("\n2. Department Statistics:");
        List<List<?>> stats = repository.getDepartmentStatistics();
        System.out.println("   Department | Count | Avg Salary");
        System.out.println("   -----------|-------|------------");
        for (List<?> row : stats) {
            System.out.printf("   %-10s | %5d | $%.2f%n",
                row.get(0), ((Number)row.get(1)).intValue(), ((Number)row.get(2)).doubleValue());
        }

        System.out.println();
    }
}
```

### Spring Data Ignite Quick Reference

```java
// Repository interface pattern
@RepositoryConfig(cacheName = "MyCache")
public interface MyRepository extends IgniteRepository<Entity, Long> {

    // Derived queries (auto-implemented)
    List<Entity> findByField(String value);
    List<Entity> findByFieldAndOtherField(String v1, String v2);
    List<Entity> findByFieldOrderByOtherFieldDesc(String value);
    long countByField(String value);
    boolean existsByField(String value);

    // Custom SQL
    @Query("SELECT * FROM Entity WHERE field = ?")
    List<Entity> customQuery(String value);
}
```

### Optional: Kafka Integration

### Exercise 7: Kafka Data Streaming with Backpressure

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

Create `Lab10KafkaIntegration.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.stream.StreamSingleTupleExtractor;
import org.apache.ignite.stream.kafka.KafkaStreamer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Lab10KafkaIntegration {

    private static final String KAFKA_BOOTSTRAP = "localhost:9092";
    private static final String TOPIC = "ignite-events";

    public static void main(String[] args) {
        System.out.println("=== Kafka Integration Lab ===\n");

        System.out.println("Prerequisites:");
        System.out.println("1. Kafka running on localhost:9092");
        System.out.println("2. Topic 'ignite-events' created\n");

        System.out.println("To create topic:");
        System.out.println("  kafka-topics.sh --create --topic ignite-events \\");
        System.out.println("    --bootstrap-server localhost:9092 \\");
        System.out.println("    --partitions 4 --replication-factor 1\n");

        // Start Ignite
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("kafka-integration-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite node started\n");

            // Create cache
            CacheConfiguration<String, Event> cacheCfg = new CacheConfiguration<>("eventCache");
            IgniteCache<String, Event> cache = ignite.getOrCreateCache(cacheCfg);

            // Demonstrate different integration patterns
            demonstrateKafkaStreamer(ignite);
            demonstrateManualConsumer(ignite, cache);
            demonstrateBackpressureHandling(ignite);
            demonstrateProducerIntegration(cache);

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateKafkaStreamer(Ignite ignite) {
        System.out.println("=== 1. Kafka Streamer Configuration ===\n");

        // Kafka consumer properties
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ignite-streamer-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        // Display configuration
        System.out.println("Kafka Consumer Configuration:");
        System.out.println("  Bootstrap servers: " + KAFKA_BOOTSTRAP);
        System.out.println("  Group ID: ignite-streamer-group");
        System.out.println("  Auto offset reset: earliest");
        System.out.println("  Auto commit: enabled (1000ms)");

        System.out.println("\nKafkaStreamer Setup Code:");
        System.out.println("```java");
        System.out.println("KafkaStreamer<String, String, String> streamer = new KafkaStreamer<>();");
        System.out.println("streamer.setIgnite(ignite);");
        System.out.println("streamer.setStreamer(ignite.dataStreamer(\"cacheName\"));");
        System.out.println("streamer.setTopic(\"topic-name\");");
        System.out.println("streamer.setThreads(4);");
        System.out.println("streamer.setConsumerConfig(kafkaProps);");
        System.out.println("streamer.setSingleTupleExtractor(msg -> {");
        System.out.println("    // Parse message and return key-value pair");
        System.out.println("    return new AbstractMap.SimpleEntry<>(key, value);");
        System.out.println("});");
        System.out.println("streamer.start();");
        System.out.println("```\n");
    }

    private static void demonstrateManualConsumer(Ignite ignite, IgniteCache<String, Event> cache) {
        System.out.println("=== 2. Manual Kafka Consumer with DataStreamer ===\n");

        System.out.println("Pattern: Poll Kafka -> Transform -> Stream to Ignite\n");

        // Show the implementation pattern
        System.out.println("```java");
        System.out.println("try (IgniteDataStreamer<String, Event> streamer = ");
        System.out.println("         ignite.dataStreamer(\"eventCache\")) {");
        System.out.println("");
        System.out.println("    // Configure streamer for optimal performance");
        System.out.println("    streamer.perNodeBufferSize(10000);");
        System.out.println("    streamer.perNodeParallelOperations(8);");
        System.out.println("    streamer.autoFlushFrequency(1000); // 1 second");
        System.out.println("    streamer.allowOverwrite(true);");
        System.out.println("");
        System.out.println("    KafkaConsumer<String, String> consumer = ...");
        System.out.println("    consumer.subscribe(Collections.singletonList(TOPIC));");
        System.out.println("");
        System.out.println("    while (running) {");
        System.out.println("        ConsumerRecords<String, String> records = ");
        System.out.println("            consumer.poll(Duration.ofMillis(100));");
        System.out.println("");
        System.out.println("        for (ConsumerRecord<String, String> record : records) {");
        System.out.println("            Event event = parseEvent(record.value());");
        System.out.println("            streamer.addData(event.getId(), event);");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```\n");

        // Create actual working example (without Kafka running)
        System.out.println("Simulating Kafka consumer with DataStreamer...");

        try (IgniteDataStreamer<String, Event> streamer = ignite.dataStreamer("eventCache")) {
            streamer.perNodeBufferSize(1000);
            streamer.perNodeParallelOperations(4);
            streamer.autoFlushFrequency(500);
            streamer.allowOverwrite(true);

            // Simulate consuming messages
            long start = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                Event event = new Event(
                    "evt-" + i,
                    "EventType" + (i % 5),
                    System.currentTimeMillis(),
                    "Payload data " + i
                );
                streamer.addData(event.getId(), event);
            }
            streamer.flush();
            long elapsed = System.currentTimeMillis() - start;

            System.out.println("Streamed 10,000 events in " + elapsed + "ms");
            System.out.println("Rate: " + (10000 * 1000 / elapsed) + " events/second");
            System.out.println("Cache size: " + cache.size() + "\n");
        }
    }

    private static void demonstrateBackpressureHandling(Ignite ignite) {
        System.out.println("=== 3. Backpressure Handling ===\n");

        System.out.println("Backpressure Strategies:\n");

        System.out.println("1. Buffer-based backpressure:");
        System.out.println("   - Monitor streamer buffer utilization");
        System.out.println("   - Pause Kafka consumer when buffer full");
        System.out.println("   - Resume when buffer drains below threshold\n");

        System.out.println("2. Rate limiting:");
        System.out.println("   - Control consumption rate programmatically");
        System.out.println("   - Use max.poll.records for batch size control");
        System.out.println("   - Implement token bucket algorithm\n");

        System.out.println("3. Partition-based control:");
        System.out.println("   - Pause specific partitions when overwhelmed");
        System.out.println("   - Resume when processing catches up\n");

        // Implementation example
        System.out.println("Implementation Example:");
        System.out.println("```java");
        System.out.println("public class BackpressureHandler {");
        System.out.println("    private final AtomicLong pendingCount = new AtomicLong(0);");
        System.out.println("    private final long maxPending = 100000;");
        System.out.println("");
        System.out.println("    public void consumeWithBackpressure(");
        System.out.println("            KafkaConsumer<String, String> consumer,");
        System.out.println("            IgniteDataStreamer<String, Event> streamer) {");
        System.out.println("");
        System.out.println("        while (running) {");
        System.out.println("            // Check backpressure");
        System.out.println("            if (pendingCount.get() > maxPending) {");
        System.out.println("                consumer.pause(consumer.assignment());");
        System.out.println("                Thread.sleep(100);");
        System.out.println("                continue;");
        System.out.println("            }");
        System.out.println("");
        System.out.println("            // Resume if paused");
        System.out.println("            consumer.resume(consumer.assignment());");
        System.out.println("");
        System.out.println("            ConsumerRecords<String, String> records = ");
        System.out.println("                consumer.poll(Duration.ofMillis(100));");
        System.out.println("");
        System.out.println("            pendingCount.addAndGet(records.count());");
        System.out.println("");
        System.out.println("            for (ConsumerRecord<String, String> rec : records) {");
        System.out.println("                streamer.addData(key, value);");
        System.out.println("            }");
        System.out.println("");
        System.out.println("            // Callback on completion");
        System.out.println("            streamer.receiver((cache, entries) -> {");
        System.out.println("                cache.putAll(entries);");
        System.out.println("                pendingCount.addAndGet(-entries.size());");
        System.out.println("            });");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```\n");

        // Demonstrate with simulation
        System.out.println("Simulating backpressure scenario...\n");

        AtomicLong pendingCount = new AtomicLong(0);
        long maxPending = 5000;

        try (IgniteDataStreamer<String, Event> streamer = ignite.dataStreamer("eventCache")) {
            streamer.perNodeBufferSize(500);
            streamer.autoFlushFrequency(100);

            long start = System.currentTimeMillis();
            int totalRecords = 0;
            int pauseCount = 0;

            // Simulate high-volume streaming with backpressure
            for (int batch = 0; batch < 20; batch++) {
                // Simulate check for backpressure
                if (pendingCount.get() > maxPending) {
                    pauseCount++;
                    System.out.println("  Backpressure triggered at batch " + batch +
                        " (pending: " + pendingCount.get() + ")");
                    Thread.sleep(50);  // Simulate pause
                    pendingCount.set(0);  // Simulate drain
                }

                for (int i = 0; i < 1000; i++) {
                    Event event = new Event(
                        "bp-" + batch + "-" + i,
                        "BackpressureTest",
                        System.currentTimeMillis(),
                        "Data"
                    );
                    streamer.addData(event.getId(), event);
                    pendingCount.incrementAndGet();
                    totalRecords++;
                }

                // Simulate partial drain
                pendingCount.addAndGet(-500);
            }

            streamer.flush();
            long elapsed = System.currentTimeMillis() - start;

            System.out.println("\nBackpressure simulation results:");
            System.out.println("  Total records: " + totalRecords);
            System.out.println("  Pause events: " + pauseCount);
            System.out.println("  Time: " + elapsed + "ms");
            System.out.println("  Effective rate: " + (totalRecords * 1000 / elapsed) + " events/second\n");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void demonstrateProducerIntegration(IgniteCache<String, Event> cache) {
        System.out.println("=== 4. Producing to Kafka from Ignite ===\n");

        System.out.println("Use case: Stream cache changes to Kafka for downstream consumers\n");

        System.out.println("Implementation with Continuous Query:");
        System.out.println("```java");
        System.out.println("// Kafka producer setup");
        System.out.println("Properties props = new Properties();");
        System.out.println("props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, \"localhost:9092\");");
        System.out.println("props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,");
        System.out.println("    StringSerializer.class.getName());");
        System.out.println("props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,");
        System.out.println("    StringSerializer.class.getName());");
        System.out.println("props.put(ProducerConfig.ACKS_CONFIG, \"all\");");
        System.out.println("props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);");
        System.out.println("props.put(ProducerConfig.LINGER_MS_CONFIG, 5);");
        System.out.println("");
        System.out.println("KafkaProducer<String, String> producer = new KafkaProducer<>(props);");
        System.out.println("");
        System.out.println("// Set up continuous query to capture changes");
        System.out.println("ContinuousQuery<String, Event> query = new ContinuousQuery<>();");
        System.out.println("query.setLocalListener(events -> {");
        System.out.println("    for (CacheEntryEvent<String, Event> e : events) {");
        System.out.println("        String json = toJson(e.getValue());");
        System.out.println("        producer.send(new ProducerRecord<>(");
        System.out.println("            \"ignite-changes\", e.getKey(), json));");
        System.out.println("    }");
        System.out.println("});");
        System.out.println("cache.query(query);");
        System.out.println("```\n");

        System.out.println("=== Kafka Integration Best Practices ===");
        System.out.println("1. Use DataStreamer for bulk ingestion (10x faster than put)");
        System.out.println("2. Configure appropriate buffer sizes based on memory");
        System.out.println("3. Implement backpressure to prevent OOM");
        System.out.println("4. Use partition-aware consuming for locality");
        System.out.println("5. Monitor lag and throughput metrics");
        System.out.println("6. Handle Kafka rebalances gracefully");
        System.out.println("7. Commit offsets after Ignite write is confirmed");
        System.out.println();
    }

    // Event class
    public static class Event implements java.io.Serializable {
        private String id;
        private String type;
        private long timestamp;
        private String payload;

        public Event() {}

        public Event(String id, String type, long timestamp, String payload) {
            this.id = id;
            this.type = type;
            this.timestamp = timestamp;
            this.payload = payload;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
```

### Kafka Integration Quick Reference

```java
// Kafka Streamer setup
KafkaStreamer<String, String, MyValue> streamer = new KafkaStreamer<>();
streamer.setIgnite(ignite);
streamer.setStreamer(ignite.dataStreamer("cache"));
streamer.setTopic("topic");
streamer.setThreads(4);
streamer.setConsumerConfig(kafkaProps);
streamer.setSingleTupleExtractor(msg -> new SimpleEntry<>(key, value));
streamer.start();

// DataStreamer configuration for high throughput
IgniteDataStreamer<K, V> streamer = ignite.dataStreamer("cache");
streamer.perNodeBufferSize(10000);
streamer.perNodeParallelOperations(8);
streamer.autoFlushFrequency(1000);
streamer.allowOverwrite(true);
```

### Optional: Challenge Exercises

### Challenge 1: Build REST-based Cache Admin Tool

Create a command-line tool that manages caches via REST API.

```java
package com.example.ignite.challenge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class RestCacheAdminTool {

    private static final String BASE_URL = "http://localhost:8080/ignite";

    public static void main(String[] args) {
        System.out.println("=== REST Cache Admin Tool ===\n");
        System.out.println("Commands: list, create <name>, get <cache> <key>,");
        System.out.println("          put <cache> <key> <value>, delete <cache> <key>,");
        System.out.println("          size <cache>, clear <cache>, sql <query>, exit\n");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("admin> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 4);
            String cmd = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "list":
                        listCaches();
                        break;

                    case "create":
                        if (parts.length < 2) {
                            System.out.println("Usage: create <cacheName>");
                        } else {
                            createCache(parts[1]);
                        }
                        break;

                    case "get":
                        if (parts.length < 3) {
                            System.out.println("Usage: get <cache> <key>");
                        } else {
                            getValue(parts[1], parts[2]);
                        }
                        break;

                    case "put":
                        if (parts.length < 4) {
                            System.out.println("Usage: put <cache> <key> <value>");
                        } else {
                            putValue(parts[1], parts[2], parts[3]);
                        }
                        break;

                    case "delete":
                        if (parts.length < 3) {
                            System.out.println("Usage: delete <cache> <key>");
                        } else {
                            deleteValue(parts[1], parts[2]);
                        }
                        break;

                    case "size":
                        if (parts.length < 2) {
                            System.out.println("Usage: size <cache>");
                        } else {
                            getCacheSize(parts[1]);
                        }
                        break;

                    case "clear":
                        if (parts.length < 2) {
                            System.out.println("Usage: clear <cache>");
                        } else {
                            clearCache(parts[1]);
                        }
                        break;

                    case "sql":
                        if (parts.length < 2) {
                            System.out.println("Usage: sql <query>");
                        } else {
                            String query = line.substring(4).trim();
                            executeSql(query);
                        }
                        break;

                    case "help":
                        printHelp();
                        break;

                    case "exit":
                    case "quit":
                        System.out.println("Goodbye!");
                        return;

                    default:
                        System.out.println("Unknown command: " + cmd);
                        System.out.println("Type 'help' for available commands");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static void listCaches() throws Exception {
        String result = restCall(BASE_URL + "?cmd=name");
        System.out.println("Caches: " + result);
    }

    private static void createCache(String name) throws Exception {
        // Create cache by putting a dummy value and removing it
        restCall(BASE_URL + "?cmd=getorcreate&cacheName=" + encode(name));
        System.out.println("Cache '" + name + "' created");
    }

    private static void getValue(String cache, String key) throws Exception {
        String url = BASE_URL + "?cmd=get&cacheName=" + encode(cache) + "&key=" + encode(key);
        String result = restCall(url);
        System.out.println("Value: " + result);
    }

    private static void putValue(String cache, String key, String value) throws Exception {
        String url = BASE_URL + "?cmd=put&cacheName=" + encode(cache) +
            "&key=" + encode(key) + "&val=" + encode(value);
        restCall(url);
        System.out.println("OK");
    }

    private static void deleteValue(String cache, String key) throws Exception {
        String url = BASE_URL + "?cmd=rmv&cacheName=" + encode(cache) + "&key=" + encode(key);
        restCall(url);
        System.out.println("Deleted");
    }

    private static void getCacheSize(String cache) throws Exception {
        String url = BASE_URL + "?cmd=size&cacheName=" + encode(cache);
        String result = restCall(url);
        System.out.println("Size: " + result);
    }

    private static void clearCache(String cache) throws Exception {
        // Note: REST API doesn't have direct clear, would need iteration
        System.out.println("Clear not directly supported via REST");
        System.out.println("Use SQL: DELETE FROM TableName");
    }

    private static void executeSql(String query) throws Exception {
        String url = BASE_URL + "?cmd=qryfldexe&pageSize=100&cacheName=SQL_PUBLIC&qry=" +
            encode(query);
        String result = restCall(url);
        System.out.println("Result: " + result);
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  list                    - List all caches");
        System.out.println("  create <name>           - Create a new cache");
        System.out.println("  get <cache> <key>       - Get value from cache");
        System.out.println("  put <cache> <key> <val> - Put value to cache");
        System.out.println("  delete <cache> <key>    - Delete key from cache");
        System.out.println("  size <cache>            - Get cache size");
        System.out.println("  sql <query>             - Execute SQL query");
        System.out.println("  help                    - Show this help");
        System.out.println("  exit                    - Exit tool");
    }

    private static String restCall(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int code = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(code == 200 ? conn.getInputStream() : conn.getErrorStream()));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    private static String encode(String s) throws Exception {
        return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
    }
}
```

### Challenge 2: Event Streaming to External System

Implement a continuous query that streams cache changes to an external system.

```java
package com.example.ignite.challenge;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.configuration.IgniteConfiguration;

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class EventStreamingChallenge {

    // Queue for async event processing
    private static final BlockingQueue<CacheEvent> eventQueue = new LinkedBlockingQueue<>(10000);
    private static volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Event Streaming to External System ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("event-streaming-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCache<String, String> cache = ignite.getOrCreateCache("streamedCache");

            // Start event processor threads
            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.submit(() -> processEvents("http://localhost:8081/events"));
            executor.submit(() -> processEvents("http://localhost:8082/events"));

            // Set up continuous query
            ContinuousQuery<String, String> query = new ContinuousQuery<>();

            query.setLocalListener(events -> {
                for (CacheEntryEvent<? extends String, ? extends String> event : events) {
                    CacheEvent cacheEvent = new CacheEvent(
                        event.getKey(),
                        event.getValue(),
                        event.getOldValue(),
                        event.getEventType().toString(),
                        System.currentTimeMillis()
                    );

                    // Non-blocking offer with backpressure
                    if (!eventQueue.offer(cacheEvent)) {
                        System.err.println("Event queue full, dropping event: " + event.getKey());
                    }
                }
            });

            try (QueryCursor<Cache.Entry<String, String>> cursor = cache.query(query)) {
                System.out.println("Continuous query started, streaming events...\n");

                // Generate some cache changes
                System.out.println("Generating cache changes...");
                for (int i = 0; i < 100; i++) {
                    cache.put("key-" + i, "value-" + i);
                    Thread.sleep(10);
                }

                // Update some entries
                for (int i = 0; i < 50; i++) {
                    cache.put("key-" + i, "updated-value-" + i);
                }

                // Remove some entries
                for (int i = 0; i < 25; i++) {
                    cache.remove("key-" + i);
                }

                System.out.println("\nGenerated 175 events (100 creates, 50 updates, 25 removes)");
                System.out.println("Queue size: " + eventQueue.size());

                // Wait for processing
                Thread.sleep(2000);

                running = false;
                executor.shutdown();

                System.out.println("\n=== Streaming Features ===");
                System.out.println("- Async event processing with queue");
                System.out.println("- Backpressure handling");
                System.out.println("- Multiple downstream targets");
                System.out.println("- Event batching possible");
                System.out.println("- Retry logic for failures");
            }
        }
    }

    private static void processEvents(String targetUrl) {
        System.out.println("Event processor started for: " + targetUrl);

        while (running || !eventQueue.isEmpty()) {
            try {
                CacheEvent event = eventQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (event != null) {
                    sendEvent(targetUrl, event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error sending event: " + e.getMessage());
            }
        }

        System.out.println("Event processor stopped for: " + targetUrl);
    }

    private static void sendEvent(String targetUrl, CacheEvent event) {
        // Simulate sending to external system
        // In production, use actual HTTP client
        System.out.printf("  -> %s: %s [%s] key=%s%n",
            targetUrl.substring(targetUrl.lastIndexOf(':') + 1, targetUrl.lastIndexOf('/')),
            event.eventType, event.key,
            event.newValue != null ? event.newValue.substring(0, Math.min(20, event.newValue.length())) : "null");

        // Actual HTTP call would look like:
        // HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
        // conn.setRequestMethod("POST");
        // conn.setDoOutput(true);
        // conn.getOutputStream().write(toJson(event).getBytes());
        // conn.getResponseCode();
    }

    static class CacheEvent {
        String key;
        String newValue;
        String oldValue;
        String eventType;
        long timestamp;

        CacheEvent(String key, String newValue, String oldValue, String eventType, long timestamp) {
            this.key = key;
            this.newValue = newValue;
            this.oldValue = oldValue;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }
    }
}
```

### Challenge 3: Multi-Protocol Gateway

Create a gateway that provides unified access via REST, thin client, and JDBC.

```java
package com.example.ignite.challenge;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.*;

import java.sql.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MultiProtocolGateway {

    private final Ignite ignite;
    private final String jdbcUrl;
    private final String thinClientAddress;

    public MultiProtocolGateway(Ignite ignite) {
        this.ignite = ignite;
        this.jdbcUrl = "jdbc:ignite:thin://127.0.0.1";
        this.thinClientAddress = "127.0.0.1:10800";
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Multi-Protocol Gateway ===\n");

        // Configure Ignite with all protocols
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("gateway-node");

        // REST API
        ConnectorConfiguration restCfg = new ConnectorConfiguration();
        restCfg.setPort(8080);
        cfg.setConnectorConfiguration(restCfg);

        // Thin Client
        ClientConnectorConfiguration thinCfg = new ClientConnectorConfiguration();
        thinCfg.setPort(10800);
        cfg.setClientConnectorConfiguration(thinCfg);

        // SQL (JDBC) - uses same thin client port

        try (Ignite ignite = Ignition.start(cfg)) {
            MultiProtocolGateway gateway = new MultiProtocolGateway(ignite);

            // Setup test data
            gateway.setupTestData();

            // Demonstrate each protocol
            System.out.println("=== Protocol Demonstrations ===\n");

            // 1. Native API
            System.out.println("1. Native Ignite API:");
            gateway.demonstrateNativeApi();

            // 2. Thin Client
            System.out.println("\n2. Thin Client Protocol:");
            gateway.demonstrateThinClient();

            // 3. JDBC
            System.out.println("\n3. JDBC Protocol:");
            gateway.demonstrateJdbc();

            // 4. REST (show commands)
            System.out.println("\n4. REST API (curl commands):");
            gateway.showRestCommands();

            // Unified access example
            System.out.println("\n=== Unified Gateway Access ===\n");
            gateway.demonstrateUnifiedAccess();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }

    private void setupTestData() {
        System.out.println("Setting up test data...\n");

        // Create SQL table
        IgniteCache<Object, Object> cache = ignite.getOrCreateCache("SQL_PUBLIC_PRODUCTS");
        cache.query(new SqlFieldsQuery(
            "CREATE TABLE IF NOT EXISTS Products (" +
            "id INT PRIMARY KEY, " +
            "name VARCHAR, " +
            "price DECIMAL, " +
            "category VARCHAR)"
        )).getAll();

        // Insert data
        for (int i = 1; i <= 10; i++) {
            cache.query(new SqlFieldsQuery(
                "MERGE INTO Products (id, name, price, category) VALUES (?, ?, ?, ?)")
                .setArgs(i, "Product-" + i, 10.0 * i, "Category-" + (i % 3))
            ).getAll();
        }

        System.out.println("Created Products table with 10 records\n");
    }

    private void demonstrateNativeApi() {
        IgniteCache<Object, Object> cache = ignite.cache("SQL_PUBLIC_PRODUCTS");

        List<List<?>> results = cache.query(new SqlFieldsQuery(
            "SELECT name, price FROM Products WHERE price > 50"
        )).getAll();

        System.out.println("  Products with price > 50:");
        for (List<?> row : results) {
            System.out.println("    " + row.get(0) + ": $" + row.get(1));
        }
    }

    private void demonstrateThinClient() {
        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses(thinClientAddress);

        try (IgniteClient client = Ignition.startClient(cfg)) {
            // Execute SQL via thin client
            List<List<?>> results = client.query(
                new SqlFieldsQuery("SELECT COUNT(*), AVG(price) FROM Products")
            ).getAll();

            List<?> row = results.get(0);
            System.out.println("  Product count: " + row.get(0));
            System.out.println("  Average price: $" + row.get(1));
        } catch (Exception e) {
            System.out.println("  Thin client error: " + e.getMessage());
        }
    }

    private void demonstrateJdbc() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                "SELECT category, COUNT(*) as cnt, SUM(price) as total " +
                "FROM Products GROUP BY category ORDER BY total DESC"
            );

            System.out.println("  Category | Count | Total");
            System.out.println("  ---------|-------|------");
            while (rs.next()) {
                System.out.printf("  %-8s | %5d | $%.2f%n",
                    rs.getString(1), rs.getInt(2), rs.getDouble(3));
            }

        } catch (SQLException e) {
            System.out.println("  JDBC error: " + e.getMessage());
        }
    }

    private void showRestCommands() {
        System.out.println("  # Get all products");
        System.out.println("  curl \"http://localhost:8080/ignite?cmd=qryfldexe&pageSize=100" +
            "&cacheName=SQL_PUBLIC_PRODUCTS&qry=SELECT%20*%20FROM%20Products\"");
        System.out.println();
        System.out.println("  # Insert product");
        System.out.println("  curl \"http://localhost:8080/ignite?cmd=qryfldexe" +
            "&cacheName=SQL_PUBLIC_PRODUCTS&qry=INSERT%20INTO%20Products%20VALUES(11,'New',99.99,'Cat1')\"");
    }

    private void demonstrateUnifiedAccess() {
        System.out.println("Gateway provides unified access:");
        System.out.println("  - Native API: Full feature set, best performance");
        System.out.println("  - Thin Client: Lightweight, language-agnostic");
        System.out.println("  - JDBC: Standard SQL access, BI tool compatible");
        System.out.println("  - REST: HTTP access, no client library needed");
        System.out.println();
        System.out.println("Protocol selection guide:");
        System.out.println("  +------------+------------------+------------------+");
        System.out.println("  | Need       | Recommended      | Alternative      |");
        System.out.println("  +------------+------------------+------------------+");
        System.out.println("  | Java app   | Native API       | Thin Client      |");
        System.out.println("  | .NET app   | .NET Thin Client | REST             |");
        System.out.println("  | Python     | Thin Client      | REST             |");
        System.out.println("  | BI tools   | JDBC             | REST             |");
        System.out.println("  | Web app    | REST             | Thin Client      |");
        System.out.println("  | Microsvcs  | Thin Client      | REST             |");
        System.out.println("  +------------+------------------+------------------+");
    }
}
```

