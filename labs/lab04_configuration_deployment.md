# Lab 4: Configuration and Deployment

## Duration: 60 minutes

## Objectives
- Learn XML vs programmatic configuration
- Integrate Apache Ignite with Spring Boot
- Configure cache parameters effectively
- Set up basic monitoring and logging
- Understand configuration best practices

## Prerequisites
- Completed Lab 1-3
- Spring Boot knowledge (helpful)
- Maven or Gradle configured

## Part 1: XML Configuration (15 minutes)

### Exercise 1: Create XML Configuration File

Create `ignite-config.xml` in your resources folder:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean class="org.apache.ignite.configuration.IgniteConfiguration">
        <!-- Node name -->
        <property name="igniteInstanceName" value="xml-configured-node"/>

        <!-- Enable peer class loading -->
        <property name="peerClassLoadingEnabled" value="true"/>

        <!-- Configure discovery -->
        <property name="discoverySpi">
            <bean class="org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi">
                <property name="ipFinder">
                    <bean class="org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder">
                        <property name="addresses">
                            <list>
                                <value>127.0.0.1:47500..47509</value>
                            </list>
                        </property>
                    </bean>
                </property>
            </bean>
        </property>

        <!-- Cache configurations -->
        <property name="cacheConfiguration">
            <list>
                <!-- Partitioned cache -->
                <bean class="org.apache.ignite.configuration.CacheConfiguration">
                    <property name="name" value="xmlPartitionedCache"/>
                    <property name="cacheMode" value="PARTITIONED"/>
                    <property name="backups" value="1"/>
                    <property name="atomicityMode" value="TRANSACTIONAL"/>
                </bean>

                <!-- Replicated cache -->
                <bean class="org.apache.ignite.configuration.CacheConfiguration">
                    <property name="name" value="xmlReplicatedCache"/>
                    <property name="cacheMode" value="REPLICATED"/>
                    <property name="atomicityMode" value="ATOMIC"/>
                </bean>
            </list>
        </property>

        <!-- Data storage configuration -->
        <property name="dataStorageConfiguration">
            <bean class="org.apache.ignite.configuration.DataStorageConfiguration">
                <property name="defaultDataRegionConfiguration">
                    <bean class="org.apache.ignite.configuration.DataRegionConfiguration">
                        <property name="name" value="Default_Region"/>
                        <property name="initialSize" value="#{100L * 1024 * 1024}"/>
                        <property name="maxSize" value="#{200L * 1024 * 1024}"/>
                        <property name="persistenceEnabled" value="false"/>
                    </bean>
                </property>
            </bean>
        </property>
    </bean>
</beans>
```

### Exercise 2: Load Configuration from XML

Create `Lab04XmlConfig.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;

public class Lab04XmlConfig {

    public static void main(String[] args) {
        System.out.println("=== XML Configuration Lab ===\n");

        // Load configuration from XML file
        try (Ignite ignite = Ignition.start("ignite-config.xml")) {
            System.out.println("Node started with XML configuration");
            System.out.println("Node name: " + ignite.name());

            // Access pre-configured caches
            IgniteCache<Integer, String> partitionedCache =
                ignite.cache("xmlPartitionedCache");
            IgniteCache<Integer, String> replicatedCache =
                ignite.cache("xmlReplicatedCache");

            System.out.println("\n=== Available Caches ===");
            ignite.cacheNames().forEach(name ->
                System.out.println("  - " + name));

            // Test the caches
            partitionedCache.put(1, "XML Configured Value");
            System.out.println("\nTest value: " +
                partitionedCache.get(1));

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Programmatic Configuration (15 minutes)

### Exercise 3: Advanced Programmatic Configuration

Create `Lab04ProgrammaticConfig.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.*;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Lab04ProgrammaticConfig {

    public static void main(String[] args) {
        System.out.println("=== Programmatic Configuration Lab ===\n");

        IgniteConfiguration cfg = createIgniteConfiguration();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Node started with programmatic configuration");
            displayConfiguration(ignite);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createIgniteConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Basic settings
        cfg.setIgniteInstanceName("programmatic-node");
        cfg.setClientMode(false);
        cfg.setPeerClassLoadingEnabled(true);

        // Discovery configuration
        cfg.setDiscoverySpi(createDiscoverySpi());

        // Data storage configuration
        cfg.setDataStorageConfiguration(createDataStorageConfig());

        // Cache configurations
        cfg.setCacheConfiguration(
            createPartitionedCache(),
            createReplicatedCache(),
            createTransactionalCache()
        );

        // Thread pool configuration
        cfg.setPublicThreadPoolSize(8);
        cfg.setSystemThreadPoolSize(8);

        // Network configuration
        cfg.setNetworkTimeout(5000);
        cfg.setFailureDetectionTimeout(10000);

        return cfg;
    }

    private static TcpDiscoverySpi createDiscoverySpi() {
        TcpDiscoverySpi spi = new TcpDiscoverySpi();

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));

        spi.setIpFinder(ipFinder);
        spi.setLocalPort(47500);
        spi.setLocalPortRange(10);

        return spi;
    }

    private static DataStorageConfiguration createDataStorageConfig() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Default data region
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("Default_Region");
        defaultRegion.setInitialSize(100L * 1024 * 1024);  // 100 MB
        defaultRegion.setMaxSize(500L * 1024 * 1024);      // 500 MB
        defaultRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        defaultRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);

        // Additional data region for persistent data
        DataRegionConfiguration persistentRegion = new DataRegionConfiguration();
        persistentRegion.setName("Persistent_Region");
        persistentRegion.setInitialSize(50L * 1024 * 1024);
        persistentRegion.setMaxSize(200L * 1024 * 1024);
        persistentRegion.setPersistenceEnabled(true);

        storageCfg.setDataRegionConfigurations(persistentRegion);

        return storageCfg;
    }

    private static CacheConfiguration<Integer, String> createPartitionedCache() {
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("programmaticPartitioned");

        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg.setBackups(1);
        cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cacheCfg.setWriteSynchronizationMode(
            CacheWriteSynchronizationMode.PRIMARY_SYNC);
        cacheCfg.setStatisticsEnabled(true);

        return cacheCfg;
    }

    private static CacheConfiguration<Integer, String> createReplicatedCache() {
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("programmaticReplicated");

        cacheCfg.setCacheMode(CacheMode.REPLICATED);
        cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cacheCfg.setStatisticsEnabled(true);

        return cacheCfg;
    }

    private static CacheConfiguration<Integer, String> createTransactionalCache() {
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("transactionalCache");

        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg.setBackups(1);
        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cacheCfg.setStatisticsEnabled(true);

        return cacheCfg;
    }

    private static void displayConfiguration(Ignite ignite) {
        System.out.println("\n=== Node Configuration ===");
        System.out.println("Instance name: " + ignite.name());
        System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

        System.out.println("\n=== Configured Caches ===");
        ignite.cacheNames().forEach(name -> {
            System.out.println("  - " + name);
        });

        System.out.println("\n=== Data Regions ===");
        ignite.dataRegionMetrics().forEach(metrics -> {
            System.out.println("  Region: " + metrics.getName());
            System.out.println("    Total allocated: " +
                metrics.getTotalAllocatedSize() / (1024 * 1024) + " MB");
        });
    }
}
```

## Part 3: Spring Boot Integration (20 minutes)

### Exercise 4: Create Spring Boot Application

**Add Spring Boot dependencies to `pom.xml`:**

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <version>2.7.14</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>2.7.14</version>
    </dependency>

    <!-- Ignite Spring Boot Starter -->
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-spring-boot-autoconfigure-ext</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- Ignite Dependencies -->
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-core</artifactId>
        <version>2.16.0</version>
    </dependency>

    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-spring</artifactId>
        <version>2.16.0</version>
    </dependency>
</dependencies>
```

**Create `application.yml`:**

```yaml
spring:
  application:
    name: ignite-springboot-app

ignite:
  instance-name: springboot-ignite-node
  peer-class-loading-enabled: true

  discovery:
    spi:
      local-port: 47500
      local-port-range: 10
      addresses:
        - 127.0.0.1:47500..47509

  caches:
    - name: springbootCache
      cache-mode: PARTITIONED
      backups: 1
      atomicity-mode: TRANSACTIONAL
```

**Create Spring Boot Application:**

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Lab04SpringBootApp implements CommandLineRunner {

    @Autowired
    private Ignite ignite;

    public static void main(String[] args) {
        SpringApplication.run(Lab04SpringBootApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n=== Spring Boot Ignite Integration ===");
        System.out.println("Ignite node: " + ignite.name());
        System.out.println("Cluster size: " + ignite.cluster().nodes().size());

        // Create cache through Spring-managed Ignite instance
        IgniteCache<Integer, String> cache =
            ignite.getOrCreateCache("springbootCache");

        // Test operations
        cache.put(1, "Spring Boot Value");
        System.out.println("Cached value: " + cache.get(1));

        System.out.println("\nApplication running. Press Ctrl+C to stop.");
    }
}
```

**Create Configuration Bean:**

```java
package com.example.ignite.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IgniteConfig {

    @Bean
    public Ignite igniteInstance() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("springboot-node");
        cfg.setPeerClassLoadingEnabled(true);

        // Cache configuration
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("springCache");
        cacheCfg.setBackups(1);

        cfg.setCacheConfiguration(cacheCfg);

        return Ignition.start(cfg);
    }
}
```

**Create REST Controller:**

```java
package com.example.ignite.controller;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private Ignite ignite;

    private static final String CACHE_NAME = "springCache";

    @PostMapping("/{key}")
    public Map<String, String> put(@PathVariable String key,
                                     @RequestBody String value) {
        IgniteCache<String, String> cache = ignite.cache(CACHE_NAME);
        cache.put(key, value);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("key", key);
        response.put("value", value);
        return response;
    }

    @GetMapping("/{key}")
    public Map<String, String> get(@PathVariable String key) {
        IgniteCache<String, String> cache = ignite.cache(CACHE_NAME);
        String value = cache.get(key);

        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        response.put("value", value);
        return response;
    }

    @GetMapping("/size")
    public Map<String, Integer> size() {
        IgniteCache<String, String> cache = ignite.cache(CACHE_NAME);
        Map<String, Integer> response = new HashMap<>();
        response.put("size", cache.size());
        return response;
    }
}
```

## Part 4: Monitoring and Logging (10 minutes)

### Exercise 5: Configure Logging

Create `log4j2.xml` in resources:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>

        <File name="IgniteLog" fileName="logs/ignite.log">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%t] %-5level %logger{36} - %msg%n"/>
        </File>
    </Appenders>

    <Loggers>
        <Logger name="org.apache.ignite" level="INFO" additivity="false">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="IgniteLog"/>
        </Logger>

        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### Exercise 6: Enable Metrics

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMetrics;

public class Lab04Monitoring {

    public static void main(String[] args) throws Exception {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Monitoring and Metrics Lab ===\n");

            // Create cache with metrics enabled
            IgniteCache<Integer, String> cache =
                ignite.getOrCreateCache("monitoredCache");

            // Perform some operations
            for (int i = 0; i < 100; i++) {
                cache.put(i, "Value-" + i);
            }

            for (int i = 0; i < 50; i++) {
                cache.get(i);
            }

            // Display cache metrics
            CacheMetrics metrics = cache.metrics();

            System.out.println("=== Cache Metrics ===");
            System.out.println("Cache gets: " + metrics.getCacheGets());
            System.out.println("Cache puts: " + metrics.getCachePuts());
            System.out.println("Cache hits: " + metrics.getCacheHits());
            System.out.println("Cache misses: " + metrics.getCacheMisses());
            System.out.println("Hit percentage: " +
                String.format("%.2f%%", metrics.getCacheHitPercentage()));
            System.out.println("Average get time: " +
                String.format("%.3f ms", metrics.getAverageGetTime()));
            System.out.println("Average put time: " +
                String.format("%.3f ms", metrics.getAveragePutTime()));

            // Cluster metrics
            System.out.println("\n=== Cluster Metrics ===");
            System.out.println("Total CPUs: " +
                ignite.cluster().metrics().getTotalCpus());
            System.out.println("Current CPU load: " +
                String.format("%.2f%%",
                    ignite.cluster().metrics().getCurrentCpuLoad() * 100));
            System.out.println("Heap memory used: " +
                ignite.cluster().metrics().getHeapMemoryUsed() / (1024 * 1024) + " MB");
            System.out.println("Heap memory max: " +
                ignite.cluster().metrics().getHeapMemoryMaximum() / (1024 * 1024) + " MB");

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        }
    }
}
```

## Verification Steps

### Checklist
- [ ] XML configuration file created and loaded successfully
- [ ] Programmatic configuration works
- [ ] Spring Boot application starts with Ignite
- [ ] REST endpoints respond correctly
- [ ] Logging configured and working
- [ ] Metrics displayed correctly

### Test Spring Boot REST API

```bash
# Put a value
curl -X POST http://localhost:8080/api/cache/mykey \
  -H "Content-Type: text/plain" \
  -d "myvalue"

# Get a value
curl http://localhost:8080/api/cache/mykey

# Get cache size
curl http://localhost:8080/api/cache/size
```

## Lab Questions

1. What are the advantages of XML configuration over programmatic configuration?
2. How does Spring Boot simplify Ignite integration?
3. What metrics are most important for monitoring cache performance?
4. When should you enable statistics on a cache?

## Answers

1. **XML advantages**: Externalized configuration, no code changes needed for config updates, easier for ops teams. **Programmatic advantages**: Type safety, IDE support, conditional logic possible.

2. Spring Boot provides **auto-configuration**, dependency injection, simplified setup, and integration with Spring ecosystem (REST, transactions, etc.).

3. Key metrics: **Hit ratio** (cache effectiveness), **average get/put times** (performance), **cache size** (memory usage), **eviction rate** (capacity planning).

4. Enable statistics when you need to:
   - Monitor performance
   - Debug cache behavior
   - Optimize configuration
   - Note: Small overhead, disable in production if not needed

## Next Steps

In Lab 5, you will:
- Implement data modeling best practices
- Use affinity keys for data colocation
- Configure native persistence
- Implement cache store patterns

## Completion

You have completed Lab 4 when you can:
- Configure Ignite using both XML and code
- Integrate Ignite with Spring Boot
- Create REST APIs with cache operations
- Monitor cache and cluster metrics
- Configure logging appropriately
