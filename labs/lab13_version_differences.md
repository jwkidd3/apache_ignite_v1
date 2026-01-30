# Lab 13: Apache Ignite Version Differences (2.16 vs 3.x)

## Duration: 90-120 minutes

## Objectives
- Understand the architectural differences between Apache Ignite 2.16 and 3.x
- Compare configuration approaches (Spring XML/Java vs HOCON/CLI)
- Explore API differences (Cache API vs Table API)
- Compare discovery, cluster lifecycle, and networking
- Understand compute grid differences and limitations
- Explore data colocation, distribution zones, and storage engines
- Compare serialization, security, and monitoring approaches
- Evaluate migration considerations and decision criteria

## Prerequisites
- Completed Labs 1-12 (familiarity with Ignite 2.x)
- Java 11 or higher (Ignite 3.x requires Java 11+)
- Docker installed (for running Ignite 3.x)
- Maven installed
- Internet connection for downloading dependencies

## Presentation

- **Presentation:** `presentations/module-13-version-differences.html`

---

## Part 1: Setting Up Both Environments (15 minutes)

### Step 1: Ignite 2.16 Project Setup

You already have Ignite 2.16 set up from previous labs. Let's create a dedicated comparison project.

Create a new directory `version-comparison/ignite2`:

```bash
mkdir -p version-comparison/ignite2
cd version-comparison/ignite2
```

Create `pom.xml` for Ignite 2.16:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.ignite</groupId>
    <artifactId>ignite2-comparison</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <ignite.version>2.16.0</ignite.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-core</artifactId>
            <version>${ignite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-spring</artifactId>
            <version>${ignite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-indexing</artifactId>
            <version>${ignite.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 2: Ignite 3.x Project Setup

Create a new directory `version-comparison/ignite3`:

```bash
mkdir -p version-comparison/ignite3
cd version-comparison/ignite3
```

Create `pom.xml` for Ignite 3.x client:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.ignite</groupId>
    <artifactId>ignite3-comparison</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <ignite3.version>3.0.0</ignite3.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.ignite</groupId>
            <artifactId>ignite-client</artifactId>
            <version>${ignite3.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 3: Start Ignite 3.x Using Docker

The easiest way to run Ignite 3.x for testing is using Docker:

```bash
# Pull the Ignite 3 image
docker pull apacheignite/ignite:3.0.0

# Start a single-node Ignite 3 cluster
docker run -d \
  --name ignite3-node \
  -p 10300:10300 \
  -p 10800:10800 \
  apacheignite/ignite:3.0.0

# Wait for startup (about 30 seconds)
sleep 30

# Initialize the cluster
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster init \
  --name=myCluster \
  --meta-storage-node=defaultNode
```

**Expected Output:**
```
Cluster was initialized successfully
```

Verify the cluster is running:
```bash
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster status
```

---

## Part 2: Discovery and Cluster Lifecycle (15 minutes)

### Exercise 1: Ignite 2.16 Discovery Configuration

Create `ignite2/src/main/java/com/example/ignite/Ignite2Discovery.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;
import java.util.Collection;

public class Ignite2Discovery {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Ignite 2.16 Discovery & Lifecycle Demo ===\n");

        // --- Discovery Configuration ---
        System.out.println("--- Discovery Configuration ---\n");

        // TcpDiscoverySpi with VM IP finder (static list)
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10); // Try ports 47500-47509

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);

        System.out.println("Discovery SPI: TcpDiscoverySpi");
        System.out.println("  IP Finder: TcpDiscoveryVmIpFinder (static list)");
        System.out.println("  Local Port: 47500");
        System.out.println("  Port Range: 10 (47500-47509)");
        System.out.println("  Topology: Ring-based");
        System.out.println();

        System.out.println("Other IP Finder options in 2.x:");
        System.out.println("  - TcpDiscoveryMulticastIpFinder (multicast, default)");
        System.out.println("  - TcpDiscoveryZookeeperIpFinder (ZooKeeper)");
        System.out.println("  - TcpDiscoveryS3IpFinder (AWS S3)");
        System.out.println("  - TcpDiscoveryGoogleStorageIpFinder (GCE)");
        System.out.println("  - TcpDiscoverySharedFsIpFinder (shared filesystem)");
        System.out.println();

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-discovery-demo");
        cfg.setDiscoverySpi(discoverySpi);

        // --- Cluster Lifecycle ---
        System.out.println("--- Cluster Lifecycle ---\n");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Node started! Cluster formed automatically on discovery.\n");

            // Cluster state management
            ClusterState state = ignite.cluster().state();
            System.out.println("Current cluster state: " + state);
            System.out.println("Is active: " + state.active());

            // Can activate/deactivate
            ignite.cluster().state(ClusterState.ACTIVE);
            System.out.println("After activation: " + ignite.cluster().state());

            // Topology information
            System.out.println("\nTopology:");
            Collection<ClusterNode> nodes = ignite.cluster().nodes();
            System.out.println("  Total nodes: " + nodes.size());
            System.out.println("  Topology version: " + ignite.cluster().topologyVersion());
            for (ClusterNode node : nodes) {
                System.out.println("  Node: " + node.consistentId());
                System.out.println("    ID: " + node.id());
                System.out.println("    Is client: " + node.isClient());
                System.out.println("    Addresses: " + node.addresses());
                System.out.println("    Host names: " + node.hostNames());
            }

            // Baseline topology (relevant for persistence)
            System.out.println("\nBaseline topology: " +
                ignite.cluster().currentBaselineTopology());

            System.out.println("\n--- Key Points ---");
            System.out.println("1. Ring topology: messages propagate O(n)");
            System.out.println("2. Discovery port 47500, communication port 47100");
            System.out.println("3. Cluster forms automatically when nodes find each other");
            System.out.println("4. Activation/deactivation controls data operations");
            System.out.println("5. Baseline topology must be managed manually (persistence)");

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        }
    }
}
```

### Exercise 2: Ignite 3.x Discovery and Lifecycle (via CLI)

Ignite 3.x discovery is configured via HOCON, and the cluster lifecycle is managed externally.

```bash
# --- Ignite 3.x Discovery ---
# Nodes use built-in node finder (SWIM gossip protocol)
# Configuration is in HOCON format:

echo 'Ignite 3.x Discovery Configuration:'
echo '  Protocol: SWIM gossip (Scalable Weakly-consistent Infection-style Membership)'
echo '  Port: 3344 (shared for discovery + communication)'
echo '  Client Port: 10800'
echo '  REST Port: 10300'
echo '  No ring topology - gossip-based, O(log n) convergence'
echo ''

# --- Cluster Lifecycle ---
# Step 1: Nodes start but are NOT part of a cluster yet
echo 'Step 1: Nodes start idle (not clustered)'
docker exec ignite3-node /opt/ignite/bin/ignite3 node status

# Step 2: Explicit cluster initialization (one-time)
echo 'Step 2: Cluster must be explicitly initialized'
docker exec ignite3-node /opt/ignite/bin/ignite3 cluster status

# Step 3: View configuration
echo 'Step 3: View cluster configuration'
docker exec ignite3-node /opt/ignite/bin/ignite3 cluster config show --selector=network

# Step 4: Dynamic configuration update (no restart!)
echo 'Step 4: Dynamic configuration update'
docker exec ignite3-node /opt/ignite/bin/ignite3 cluster config show --selector=storage

# --- Key Differences ---
echo ''
echo '--- Key Differences ---'
echo '1. SWIM protocol: faster failure detection than ring'
echo '2. Single port (3344) for inter-node communication'
echo '3. Cluster requires explicit initialization (cluster init)'
echo '4. No activation/deactivation - always active after init'
echo '5. No baseline topology - RAFT handles membership'
echo '6. Configuration changes are dynamic (many settings)'
```

---

## Part 3: Configuration Comparison (15 minutes)

### Exercise 3: Ignite 2.16 Spring XML Configuration

Create `ignite2/src/main/resources/ignite-config.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean id="ignite.cfg" class="org.apache.ignite.configuration.IgniteConfiguration">
        <!-- Instance name -->
        <property name="igniteInstanceName" value="ignite2-comparison-node"/>

        <!-- Enable peer class loading -->
        <property name="peerClassLoadingEnabled" value="true"/>

        <!-- Discovery configuration -->
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

        <!-- Data storage with persistence -->
        <property name="dataStorageConfiguration">
            <bean class="org.apache.ignite.configuration.DataStorageConfiguration">
                <property name="defaultDataRegionConfiguration">
                    <bean class="org.apache.ignite.configuration.DataRegionConfiguration">
                        <property name="maxSize" value="#{256L * 1024 * 1024}"/>
                        <property name="persistenceEnabled" value="false"/>
                    </bean>
                </property>
            </bean>
        </property>

        <!-- Cache configuration -->
        <property name="cacheConfiguration">
            <list>
                <bean class="org.apache.ignite.configuration.CacheConfiguration">
                    <property name="name" value="persons"/>
                    <property name="cacheMode" value="PARTITIONED"/>
                    <property name="backups" value="1"/>
                    <property name="indexedTypes">
                        <list>
                            <value>java.lang.Integer</value>
                            <value>com.example.ignite.model.Person</value>
                        </list>
                    </property>
                </bean>
            </list>
        </property>
    </bean>
</beans>
```

### Exercise 4: Ignite 2.16 Programmatic Configuration

Create `ignite2/src/main/java/com/example/ignite/Ignite2Config.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

public class Ignite2Config {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Configuration Demo ===\n");

        // Programmatic configuration
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-programmatic-node");
        cfg.setPeerClassLoadingEnabled(true);

        // Discovery configuration
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Data storage configuration
        DataStorageConfiguration dsc = new DataStorageConfiguration();
        DataRegionConfiguration drc = new DataRegionConfiguration();
        drc.setMaxSize(256L * 1024 * 1024); // 256 MB
        drc.setPersistenceEnabled(false);
        dsc.setDefaultDataRegionConfiguration(drc);
        cfg.setDataStorageConfiguration(dsc);

        // Cache configuration
        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("myCache");
        cacheCfg.setBackups(1);
        cfg.setCacheConfiguration(cacheCfg);

        System.out.println("Configuration created:");
        System.out.println("  - Instance Name: " + cfg.getIgniteInstanceName());
        System.out.println("  - Peer Class Loading: " + cfg.isPeerClassLoadingEnabled());
        System.out.println("  - Cache Configs: " + cfg.getCacheConfiguration().length);
        System.out.println("  - Data Region Max Size: 256 MB");

        // Start Ignite
        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\nIgnite 2.16 node started!");
            System.out.println("  - Node ID: " + ignite.cluster().localNode().id());
            System.out.println("  - Cluster size: " + ignite.cluster().nodes().size());

            // Configuration is STATIC - cannot be changed at runtime
            System.out.println("\nNote: Configuration changes require node restart in 2.x");

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 5: Ignite 3.x HOCON Configuration and Dynamic Updates

Create `ignite3/config/ignite-config.conf` (HOCON format):

```hocon
# Ignite 3.x HOCON Configuration
ignite {
    # Network configuration
    network {
        port: 3344
        nodeFinder {
            netClusterNodes: ["localhost:3344"]
        }
    }

    # Cluster configuration
    cluster {
        name: "comparison-cluster"
    }

    # Storage configuration - pluggable engines!
    storage {
        profiles: [
            {
                name: "default"
                engine: "aipersist"  # Options: aimem, aipersist, rocksdb
            }
            {
                name: "in-memory-only"
                engine: "aimem"
            }
        ]
    }

    # REST API configuration
    rest {
        port: 10300
    }

    # Client connector
    clientConnector {
        port: 10800
    }
}
```

Explore dynamic configuration via CLI:

```bash
# View current configuration
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster config show

# View specific section
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster config show --selector=storage

# View node-specific configuration
docker exec -it ignite3-node /opt/ignite/bin/ignite3 node config show

# Update configuration dynamically (no restart!)
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster config update \
  "gc.lowWatermark=0.3"
```

**Key Differences:**
1. **Format**: HOCON is more readable than XML
2. **Pluggable Storage**: Choose storage engine per profile
3. **Dynamic Config**: Many settings can be changed at runtime via CLI
4. **No Peer Class Loading**: 3.x uses deployment units instead
5. **No Spring XML**: Configuration is separate from application framework

---

## Part 4: API Comparison (20 minutes)

### Exercise 6: Ignite 2.16 Cache API

Create `ignite2/src/main/java/com/example/ignite/model/Person.java`:

```java
package com.example.ignite.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;

public class Person implements Serializable {
    @QuerySqlField(index = true)
    private int id;

    @QuerySqlField
    private String firstName;

    @QuerySqlField(index = true)
    private String lastName;

    @QuerySqlField
    private int age;

    public Person() {}

    public Person(int id, String firstName, String lastName, int age) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    @Override
    public String toString() {
        return String.format("Person[id=%d, name=%s %s, age=%d]",
            id, firstName, lastName, age);
    }
}
```

Create `ignite2/src/main/java/com/example/ignite/Ignite2CacheAPI.java`:

```java
package com.example.ignite;

import com.example.ignite.model.Person;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Ignite2CacheAPI {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Cache API Demo ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-cache-demo");
        cfg.setPeerClassLoadingEnabled(true);

        // Cache configuration with indexed types
        CacheConfiguration<Integer, Person> personCacheCfg =
            new CacheConfiguration<>("PersonCache");
        personCacheCfg.setIndexedTypes(Integer.class, Person.class);
        cfg.setCacheConfiguration(personCacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            // Get cache - this is the primary API in 2.x
            IgniteCache<Integer, Person> cache = ignite.cache("PersonCache");

            System.out.println("--- CRUD Operations (Cache API) ---\n");

            // CREATE
            System.out.println("1. Creating persons...");
            cache.put(1, new Person(1, "John", "Doe", 30));
            cache.put(2, new Person(2, "Jane", "Smith", 28));
            cache.put(3, new Person(3, "Bob", "Johnson", 35));
            System.out.println("   Created 3 persons\n");

            // BATCH CREATE
            System.out.println("2. Batch insert...");
            Map<Integer, Person> batch = new HashMap<>();
            batch.put(4, new Person(4, "Alice", "Williams", 25));
            batch.put(5, new Person(5, "Charlie", "Brown", 40));
            cache.putAll(batch);
            System.out.println("   Batch inserted 2 persons\n");

            // READ
            System.out.println("3. Reading person with ID 1...");
            Person person = cache.get(1);
            System.out.println("   " + person + "\n");

            // BATCH READ
            System.out.println("4. Batch read (IDs 1, 2, 3)...");
            Map<Integer, Person> batchResult = cache.getAll(Set.of(1, 2, 3));
            batchResult.forEach((k, v) -> System.out.println("   " + v));
            System.out.println();

            // UPDATE - full object replacement
            System.out.println("5. Updating person's age (full object replacement)...");
            person.setAge(31);
            cache.put(1, person);
            System.out.println("   Updated: " + cache.get(1) + "\n");

            // DELETE
            System.out.println("6. Deleting person with ID 3...");
            cache.remove(3);
            System.out.println("   Person 3 deleted: " + (cache.get(3) == null) + "\n");

            // BINARY OBJECT ACCESS (schema-less)
            System.out.println("--- BinaryObject API (Schema-less Access) ---\n");
            IgniteCache<Integer, BinaryObject> binaryCache = cache.withKeepBinary();
            BinaryObject bo = binaryCache.get(1);
            System.out.println("7. BinaryObject access (no deserialization):");
            System.out.println("   firstName: " + bo.field("firstName"));
            System.out.println("   lastName: " + bo.field("lastName"));
            System.out.println("   age: " + bo.<Integer>field("age"));
            System.out.println();

            // SQL Query (layered on top of cache)
            System.out.println("--- SQL Query (on Cache) ---\n");
            SqlFieldsQuery query = new SqlFieldsQuery(
                "SELECT id, firstName, lastName, age FROM Person WHERE age > ?");
            query.setArgs(25);

            System.out.println("8. Query: Persons older than 25");
            try (QueryCursor<List<?>> cursor = cache.query(query)) {
                for (List<?> row : cursor) {
                    System.out.printf("   ID: %d, Name: %s %s, Age: %d%n",
                        row.get(0), row.get(1), row.get(2), row.get(3));
                }
            }

            // ASYNC OPERATIONS
            System.out.println("\n--- Async Operations ---\n");
            System.out.println("9. Async get (returns IgniteFuture):");
            cache.getAsync(1).listen(f ->
                System.out.println("   Async result: " + f.get()));

            Thread.sleep(500); // Wait for async

            System.out.println("\n--- Key Points for Ignite 2.x ---");
            System.out.println("1. Cache API is primary - data accessed via cache.get()/put()");
            System.out.println("2. Schema defined via @QuerySqlField annotations");
            System.out.println("3. SQL queries use cache.query() method");
            System.out.println("4. Update requires full object replacement");
            System.out.println("5. BinaryObject for schema-less access (no deserialization)");
            System.out.println("6. Async returns IgniteFuture (custom type)");

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 7: Ignite 3.x Table API

Create `ignite3/src/main/java/com/example/ignite/Ignite3TableAPI.java`:

```java
package com.example.ignite;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

import java.util.concurrent.CompletableFuture;

public class Ignite3TableAPI {

    public static void main(String[] args) {
        System.out.println("=== Ignite 3.x Table API Demo ===\n");

        // Connect to Ignite 3 cluster (unified client - no thick/thin distinction)
        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {

            System.out.println("Connected to Ignite 3.x cluster!\n");

            // Step 1: Create table using SQL DDL (schema-first approach)
            System.out.println("--- Schema Definition (SQL DDL) ---\n");
            System.out.println("1. Creating table via SQL DDL...");

            client.sql().execute(null,
                "CREATE TABLE IF NOT EXISTS Person (" +
                "    id INT PRIMARY KEY," +
                "    firstName VARCHAR(100)," +
                "    lastName VARCHAR(100)," +
                "    age INT" +
                ")");
            System.out.println("   Table 'Person' created\n");

            // Create index
            System.out.println("2. Creating index...");
            try {
                client.sql().execute(null,
                    "CREATE INDEX IF NOT EXISTS idx_person_lastname ON Person (lastName)");
                System.out.println("   Index created\n");
            } catch (Exception e) {
                System.out.println("   Index already exists or skipped\n");
            }

            // Get table reference
            Table personTable = client.tables().table("Person");

            // Step 2: Using RecordView (Tuple-based)
            System.out.println("--- RecordView API (Tuple-based) ---\n");
            RecordView<Tuple> recordView = personTable.recordView();

            System.out.println("3. Inserting records using RecordView...");
            recordView.upsert(null, Tuple.create()
                .set("id", 1)
                .set("firstName", "John")
                .set("lastName", "Doe")
                .set("age", 30));

            recordView.upsert(null, Tuple.create()
                .set("id", 2)
                .set("firstName", "Jane")
                .set("lastName", "Smith")
                .set("age", 28));
            System.out.println("   Inserted 2 records\n");

            // Read using RecordView
            System.out.println("4. Reading record with ID 1...");
            Tuple key = Tuple.create().set("id", 1);
            Tuple record = recordView.get(null, key);
            if (record != null) {
                System.out.printf("   Person: %s %s, age %d%n",
                    record.stringValue("firstName"),
                    record.stringValue("lastName"),
                    record.intValue("age"));
            }
            System.out.println();

            // Step 3: Using KeyValueView (similar to 2.x cache)
            System.out.println("--- KeyValueView API (Cache-like) ---\n");
            KeyValueView<Tuple, Tuple> kvView = personTable.keyValueView();

            System.out.println("5. Using KeyValueView (familiar to 2.x users)...");
            Tuple keyTuple = Tuple.create().set("id", 3);
            Tuple valueTuple = Tuple.create()
                .set("firstName", "Bob")
                .set("lastName", "Johnson")
                .set("age", 35);
            kvView.put(null, keyTuple, valueTuple);
            System.out.println("   Inserted person with ID 3\n");

            // Step 4: SQL Query (first-class citizen in 3.x)
            System.out.println("--- SQL Query (First-Class API) ---\n");
            System.out.println("6. Query: Persons older than 25");

            try (ResultSet<SqlRow> rs = client.sql().execute(null,
                    "SELECT id, firstName, lastName, age FROM Person WHERE age > ?", 25)) {
                while (rs.hasNext()) {
                    SqlRow row = rs.next();
                    System.out.printf("   ID: %d, Name: %s %s, Age: %d%n",
                        row.intValue("id"),
                        row.stringValue("firstName"),
                        row.stringValue("lastName"),
                        row.intValue("age"));
                }
            }

            // Step 5: Async operations (standard CompletableFuture)
            System.out.println("\n--- Async Operations (CompletableFuture) ---\n");
            System.out.println("7. Async get (returns CompletableFuture):");
            CompletableFuture<Tuple> future = recordView.getAsync(null, key);
            future.thenAccept(t -> {
                if (t != null) {
                    System.out.println("   Async result: " + t.stringValue("firstName") +
                        " " + t.stringValue("lastName"));
                }
            }).join();

            // Step 6: Dynamic schema evolution
            System.out.println("\n--- Dynamic Schema Evolution ---\n");
            System.out.println("8. Adding column dynamically (no restart!)...");
            try {
                client.sql().execute(null,
                    "ALTER TABLE Person ADD COLUMN email VARCHAR(200)");
                System.out.println("   Column 'email' added to Person table");
            } catch (Exception e) {
                System.out.println("   Column may already exist: " + e.getMessage());
            }

            System.out.println("\n--- Key Points for Ignite 3.x ---");
            System.out.println("1. Schema defined via SQL DDL (CREATE TABLE)");
            System.out.println("2. Multiple views of same data: RecordView, KeyValueView, SQL");
            System.out.println("3. SQL is first-class citizen, not layered on cache");
            System.out.println("4. Unified client model - same features everywhere");
            System.out.println("5. Tuple-based API for flexible data access");
            System.out.println("6. Async returns standard CompletableFuture");
            System.out.println("7. Dynamic schema evolution via ALTER TABLE");

            // Cleanup
            System.out.println("\n9. Cleaning up...");
            client.sql().execute(null, "DROP TABLE IF EXISTS Person");
            System.out.println("   Table dropped\n");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println("\nMake sure Ignite 3 is running:");
            System.out.println("  docker run -d --name ignite3-node -p 10800:10800 apacheignite/ignite:3.0.0");
        }
    }
}
```

### Exercise 8: Run Both Demos

**Run Ignite 2.16 demo:**
```bash
cd version-comparison/ignite2
mvn compile exec:java -Dexec.mainClass="com.example.ignite.Ignite2CacheAPI"
```

**Run Ignite 3.x demo (ensure Docker container is running):**
```bash
cd version-comparison/ignite3
mvn compile exec:java -Dexec.mainClass="com.example.ignite.Ignite3TableAPI"
```

---

## Part 5: Transaction Comparison (10 minutes)

### Exercise 9: Ignite 2.16 Transactions

Create `ignite2/src/main/java/com/example/ignite/Ignite2Transactions.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

public class Ignite2Transactions {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Transaction Demo ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-tx-demo");

        CacheConfiguration<Integer, Integer> cacheCfg =
            new CacheConfiguration<>("AccountCache");
        cacheCfg.setAtomicityMode(org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL);
        cfg.setCacheConfiguration(cacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCache<Integer, Integer> accounts = ignite.cache("AccountCache");

            // Initialize accounts
            accounts.put(1, 1000);  // Account 1: $1000
            accounts.put(2, 500);   // Account 2: $500

            System.out.println("Initial balances:");
            System.out.println("  Account 1: $" + accounts.get(1));
            System.out.println("  Account 2: $" + accounts.get(2));

            // Transaction in 2.x - explicit concurrency and isolation
            System.out.println("\nPerforming transfer of $200...\n");

            // PESSIMISTIC + REPEATABLE_READ (most common in 2.x)
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = accounts.get(1);
                int balance2 = accounts.get(2);

                // Transfer $200
                accounts.put(1, balance1 - 200);
                accounts.put(2, balance2 + 200);

                tx.commit();
                System.out.println("Transaction committed!");
            }

            System.out.println("\nFinal balances:");
            System.out.println("  Account 1: $" + accounts.get(1));
            System.out.println("  Account 2: $" + accounts.get(2));

            System.out.println("\n--- Ignite 2.x Transaction Notes ---");
            System.out.println("1. Concurrency: PESSIMISTIC or OPTIMISTIC");
            System.out.println("2. Isolation: READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE");
            System.out.println("3. KV transactions are robust");
            System.out.println("4. SQL transactions limited (cannot easily mix SQL and KV)");
            System.out.println("5. Must choose concurrency/isolation explicitly");
            System.out.println("6. Deadlock detection available but not automatic resolution");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Exercise 10: Ignite 3.x Transactions

Create `ignite3/src/main/java/com/example/ignite/Ignite3Transactions.java`:

```java
package com.example.ignite;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;

public class Ignite3Transactions {

    public static void main(String[] args) {
        System.out.println("=== Ignite 3.x Transaction Demo ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("localhost:10800")
                .build()) {

            // Create accounts table
            client.sql().execute(null,
                "CREATE TABLE IF NOT EXISTS Accounts (" +
                "    id INT PRIMARY KEY," +
                "    balance INT" +
                ")");

            // Initialize accounts via SQL
            client.sql().execute(null,
                "INSERT INTO Accounts (id, balance) VALUES (1, 1000) " +
                "ON CONFLICT DO UPDATE SET balance = 1000");
            client.sql().execute(null,
                "INSERT INTO Accounts (id, balance) VALUES (2, 500) " +
                "ON CONFLICT DO UPDATE SET balance = 500");

            System.out.println("Initial balances:");
            printBalances(client);

            // Transaction in 3.x - supports BOTH SQL and KV in same transaction!
            System.out.println("\nPerforming transfer of $200...\n");

            Transaction tx = client.transactions().begin();

            try {
                // SQL operation
                client.sql().execute(tx,
                    "UPDATE Accounts SET balance = balance - 200 WHERE id = 1");

                // KV operation in SAME transaction
                Table accountsTable = client.tables().table("Accounts");
                KeyValueView<Tuple, Tuple> kv = accountsTable.keyValueView();

                Tuple key = Tuple.create().set("id", 2);
                Tuple currentValue = kv.get(tx, key);
                int newBalance = currentValue.intValue("balance") + 200;

                kv.put(tx, key, Tuple.create().set("balance", newBalance));

                // Both SQL and KV committed atomically
                tx.commit();
                System.out.println("Transaction committed!");

            } catch (Exception e) {
                tx.rollback();
                System.out.println("Transaction rolled back: " + e.getMessage());
            }

            System.out.println("\nFinal balances:");
            printBalances(client);

            System.out.println("\n--- Ignite 3.x Transaction Notes ---");
            System.out.println("1. Strictly serializable isolation by default");
            System.out.println("2. No need to choose concurrency/isolation mode");
            System.out.println("3. Full SQL transaction support!");
            System.out.println("4. Can mix SQL and KV operations in same transaction");
            System.out.println("5. RAFT-based consensus ensures consistency");
            System.out.println("6. Natural for RDBMS migration");

            // Cleanup
            client.sql().execute(null, "DROP TABLE IF EXISTS Accounts");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printBalances(IgniteClient client) {
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT id, balance FROM Accounts ORDER BY id")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("  Account %d: $%d%n",
                    row.intValue("id"), row.intValue("balance"));
            }
        }
    }
}
```

---

## Part 6: Compute Grid Comparison (10 minutes)

### Exercise 11: Ignite 2.16 Compute Grid

Create `ignite2/src/main/java/com/example/ignite/Ignite2Compute.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;

import java.util.*;

public class Ignite2Compute {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Ignite 2.16 Compute Grid Demo ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-compute-demo");
        cfg.setPeerClassLoadingEnabled(true);

        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("dataCache");
        cfg.setCacheConfiguration(cacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCompute compute = ignite.compute();

            // 1. Broadcast - run on all nodes
            System.out.println("1. Broadcast (run on all nodes):");
            compute.broadcast(() ->
                System.out.println("   Hello from " + Ignition.localIgnite().name()));
            System.out.println();

            // 2. IgniteCallable - run on one node with return value
            System.out.println("2. IgniteCallable (single node, returns value):");
            String result = compute.call(() -> {
                return "Computed on " + Ignition.localIgnite().name();
            });
            System.out.println("   Result: " + result);
            System.out.println();

            // 3. Affinity-aware compute
            System.out.println("3. Affinity-aware compute:");
            IgniteCache<Integer, String> cache = ignite.cache("dataCache");
            cache.put(42, "important-data");
            compute.affinityRun("dataCache", 42, () -> {
                System.out.println("   Running on node that owns key 42");
                System.out.println("   Node: " + Ignition.localIgnite().name());
            });
            System.out.println();

            // 4. MapReduce with ComputeTaskAdapter
            System.out.println("4. MapReduce (ComputeTaskAdapter):");
            int sum = compute.execute(new SumTask(), Arrays.asList(1,2,3,4,5,6,7,8,9,10));
            System.out.println("   Sum of 1..10 = " + sum);
            System.out.println();

            // 5. ExecutorService
            System.out.println("5. Java ExecutorService over grid:");
            java.util.concurrent.ExecutorService exec = ignite.executorService();
            java.util.concurrent.Future<String> future =
                exec.submit(() -> "Executed via ExecutorService on " +
                    Ignition.localIgnite().name());
            System.out.println("   Result: " + future.get());
            System.out.println();

            System.out.println("--- Ignite 2.x Compute Features ---");
            System.out.println("- Broadcast, Call, Run, Apply");
            System.out.println("- ComputeTask / ComputeTaskAdapter (full MapReduce)");
            System.out.println("- Affinity-aware compute (affinityRun/Call)");
            System.out.println("- Service Grid (cluster singletons, node singletons)");
            System.out.println("- Peer Class Loading (deploy code dynamically)");
            System.out.println("- Failover SPI (automatic job retry)");
            System.out.println("- Load Balancing SPI (round-robin, weighted)");
            System.out.println("- Standard Java ExecutorService");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MapReduce task: sum a list of integers
    static class SumTask extends ComputeTaskAdapter<List<Integer>, Integer> {
        @Override
        public Map<? extends ComputeJob, ClusterNode> map(
                List<ClusterNode> nodes, List<Integer> arg) {
            Map<ComputeJob, ClusterNode> jobs = new HashMap<>();
            for (Integer num : arg) {
                // Distribute each number to a node
                ClusterNode node = nodes.get(num % nodes.size());
                jobs.put(new ComputeJob() {
                    @Override
                    public Integer execute() {
                        return num * num; // Square each number
                    }
                    @Override
                    public void cancel() {}
                }, node);
            }
            return jobs;
        }

        @Override
        public Integer reduce(List<ComputeJobResult> results) {
            return results.stream()
                .mapToInt(r -> (Integer) r.getData())
                .sum();
        }
    }
}
```

### Exercise 12: Ignite 3.x Compute (Basic)

Discuss the current state of compute in Ignite 3.x:

```java
// Ignite 3.x compute - simplified API (still evolving)

// Basic job execution on specific nodes
// Set<ClusterNode> nodes = client.clusterNodes();
// client.compute().execute(nodes, MyJob.class, "arg");

// Colocated compute - runs on node owning the key
// client.compute().executeColocated("tableName",
//     Tuple.create().set("id", 42), MyJob.class, "arg");
```

**What's NOT available in Ignite 3.x yet:**
- Full MapReduce (ComputeTask/ComputeTaskAdapter)
- Service Grid (deploy cluster singletons)
- Peer Class Loading
- Failover SPI (automatic job retry)
- Load Balancing SPI
- Java ExecutorService over grid

**Discussion Questions:**
1. Why would the absence of full MapReduce matter for your use case?
2. Could you use SQL aggregations instead of compute for some workloads?
3. When would colocated compute be sufficient vs. full MapReduce?

---

## Part 7: Data Colocation and Distribution Zones (10 minutes)

### Exercise 13: Ignite 2.16 Affinity Keys

```java
// In Ignite 2.x, colocation is via @AffinityKeyMapped
public class OrderKey implements Serializable {
    @AffinityKeyMapped
    private int customerId;  // Colocate with customer
    private int orderId;
}

// Orders for same customer are on same node
CacheConfiguration<OrderKey, Order> orderCfg = new CacheConfiguration<>("orders");
orderCfg.setBackups(1);

// Affinity function controls partitioning
RendezvousAffinityFunction af = new RendezvousAffinityFunction();
af.setPartitions(1024);
orderCfg.setAffinity(af);

// Run compute on the node owning the data
ignite.compute().affinityRun("orders", customerId, () -> {
    // Local data access - fast!
});
```

### Exercise 14: Ignite 3.x Distribution Zones

```bash
# Create zones with different storage and replication
docker exec -it ignite3-node /opt/ignite/bin/ignite3 sql \
  "CREATE ZONE IF NOT EXISTS hot_zone WITH replicas=3, partitions=256, storage_profiles='default'"

docker exec -it ignite3-node /opt/ignite/bin/ignite3 sql \
  "CREATE ZONE IF NOT EXISTS cold_zone WITH replicas=1, partitions=64, storage_profiles='default'"

# Create tables in specific zones
docker exec -it ignite3-node /opt/ignite/bin/ignite3 sql \
  "CREATE TABLE IF NOT EXISTS sessions (id INT PRIMARY KEY, data VARCHAR) WITH PRIMARY_ZONE='hot_zone'"

docker exec -it ignite3-node /opt/ignite/bin/ignite3 sql \
  "CREATE TABLE IF NOT EXISTS audit_log (id INT PRIMARY KEY, event VARCHAR) WITH PRIMARY_ZONE='cold_zone'"

# Colocation via COLOCATE BY
docker exec -it ignite3-node /opt/ignite/bin/ignite3 sql \
  "CREATE TABLE IF NOT EXISTS customers (id INT PRIMARY KEY, name VARCHAR) WITH PRIMARY_ZONE='hot_zone'"

docker exec -it ignite3-node /opt/ignite/bin/ignite3 sql \
  "CREATE TABLE IF NOT EXISTS orders (
    orderId INT,
    customerId INT,
    amount DECIMAL,
    PRIMARY KEY (orderId, customerId)
  ) COLOCATE BY (customerId) WITH PRIMARY_ZONE='hot_zone'"
```

**Key Differences:**
| Aspect | Ignite 2.16 | Ignite 3.x |
|--------|-------------|------------|
| Colocation | @AffinityKeyMapped annotation | COLOCATE BY clause in SQL |
| Placement Control | Affinity function per cache | Distribution Zones |
| Storage per Table | Same engine for all | Different per zone profile |
| Replication Factor | Backups per cache | Replicas per zone |

---

## Part 8: Monitoring Comparison (5 minutes)

### Exercise 15: Ignite 2.16 Metrics

```java
// In your Ignite 2.x application:
ClusterMetrics metrics = ignite.cluster().metrics();
System.out.println("Total CPUs: " + metrics.getTotalCpus());
System.out.println("Heap used: " + metrics.getHeapMemoryUsed());
System.out.println("Total nodes: " + metrics.getTotalNodes());

// Cache metrics
CacheMetrics cm = cache.metrics();
System.out.println("Cache hits: " + cm.getCacheHits());
System.out.println("Cache misses: " + cm.getCacheMisses());
System.out.println("Cache size: " + cm.getCacheSize());

// JMX is the primary monitoring interface
// Access via JConsole, VisualVM, or monitoring tools
```

### Exercise 16: Ignite 3.x Monitoring via REST and CLI

```bash
# Cluster status
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster status

# Node status
docker exec -it ignite3-node /opt/ignite/bin/ignite3 node status

# REST API for metrics (built-in)
curl http://localhost:10300/management/v1/cluster/state

# View cluster topology
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster topology physical
```

**Key Differences:**
- 2.x: JMX-based, `cluster().metrics()`, requires JMX agents for remote monitoring
- 3.x: REST API built-in, CLI commands, OpenMetrics/Prometheus-compatible format

---

## Part 9: Side-by-Side Comparison Summary (5 minutes)

### Exercise 17: Complete Comparison Chart

Review and discuss the comprehensive differences:

| Aspect | Ignite 2.16 | Ignite 3.x |
|--------|-------------|------------|
| **Philosophy** | Cache-first | Schema-first |
| **Java Version** | Java 8+ | Java 11+ |
| **Configuration** | Spring XML / Java code | HOCON / CLI |
| **Config Changes** | Requires restart | Dynamic (many settings) |
| **Discovery** | Ring topology (TcpDiscoverySpi) | SWIM gossip protocol |
| **Discovery Port** | 47500 | 3344 |
| **Cluster Lifecycle** | Auto-forms on discovery | Explicit `cluster init` |
| **Activation** | ACTIVE / INACTIVE states | Always active after init |
| **Baseline Topology** | Manual management | Automatic (RAFT) |
| **Primary API** | Cache API | Table API |
| **Schema Definition** | Java annotations | SQL DDL |
| **Schema Evolution** | Limited (add fields) | Full DDL (add/drop/alter) |
| **SQL Support** | Layered on cache (H2/Calcite) | First-class (Calcite only) |
| **SQL Transactions** | Limited | Full support |
| **Transaction Model** | PESSIMISTIC/OPTIMISTIC | Strictly serializable |
| **Client Model** | Thick vs Thin | Unified |
| **Consensus** | Custom protocol | RAFT |
| **Storage** | Single engine | Pluggable (aimem/aipersist/RocksDB) |
| **Data Colocation** | @AffinityKeyMapped | COLOCATE BY in DDL |
| **Data Placement** | Affinity function per cache | Distribution Zones |
| **Compute Grid** | Full (MapReduce, services) | Basic (evolving) |
| **Serialization** | BinaryMarshaller | Schema-based internal |
| **Security** | Plugin-based (limited OSS) | Built-in auth/authz |
| **Monitoring** | JMX primary | REST API + JMX |
| **Async API** | IgniteFuture (custom) | CompletableFuture (standard) |
| **Continuous Queries** | Yes | Not yet |
| **Near Cache** | Yes | Not yet |
| **Service Grid** | Yes | Not yet |
| **Spring Data** | Yes | Not yet |
| **Hibernate L2** | Yes | Not yet |
| **Kafka Connector** | Yes | Not yet |

---

## Verification Steps

### Checklist
- [ ] Created separate project directories for 2.16 and 3.x
- [ ] Successfully started Ignite 2.16 node
- [ ] Successfully started Ignite 3.x via Docker
- [ ] Explored discovery configuration differences (ring vs SWIM)
- [ ] Compared cluster lifecycle (auto-form vs explicit init)
- [ ] Ran programmatic configuration demo (2.16)
- [ ] Explored HOCON configuration and dynamic updates (3.x)
- [ ] Ran Cache API demo (2.16) with BinaryObject access
- [ ] Ran Table API demo (3.x) with RecordView and KeyValueView
- [ ] Compared transaction models (PESSIMISTIC vs strictly serializable)
- [ ] Explored compute grid differences (full vs basic)
- [ ] Compared data colocation (affinity keys vs distribution zones)
- [ ] Explored monitoring differences (JMX vs REST)
- [ ] Understood schema evolution capabilities

### Common Issues and Solutions

**Issue 1: Ignite 3 Docker container not starting**
```bash
# Check container logs
docker logs ignite3-node

# Restart container
docker rm -f ignite3-node
docker run -d --name ignite3-node -p 10300:10300 -p 10800:10800 apacheignite/ignite:3.0.0
```

**Issue 2: Cannot connect to Ignite 3**
```bash
# Verify cluster is initialized
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster status

# Re-initialize if needed
docker exec -it ignite3-node /opt/ignite/bin/ignite3 cluster init --name=myCluster --meta-storage-node=defaultNode
```

**Issue 3: Port conflicts**
```
# Use different ports
docker run -d --name ignite3-node -p 10301:10300 -p 10801:10800 apacheignite/ignite:3.0.0
# Update client connection: .addresses("localhost:10801")
```

**Issue 4: Java version issues**
```bash
# Ignite 3.x requires Java 11+
java -version
# If Java 8, upgrade or use SDKMAN:
sdk install java 11.0.21-tem
sdk use java 11.0.21-tem
```

---

## Lab Questions

1. What is the fundamental philosophical difference between Ignite 2.x (cache-first) and 3.x (schema-first)? How does this affect application design?

2. Why did Apache Ignite 3.x adopt the RAFT consensus algorithm instead of the 2.x custom replication protocol? What guarantees does RAFT provide?

3. What are the three storage engine options in Ignite 3.x, and when would you use each? Why can't Ignite 2.x offer per-table engine selection?

4. Explain the difference between Ignite 2.x discovery (ring topology) and 3.x discovery (SWIM gossip). Which is more scalable and why?

5. Why does Ignite 3.x not have thick vs. thin clients? What advantage does the unified client model provide?

6. What are Distribution Zones in Ignite 3.x? How do they compare to Ignite 2.x cache-level backup configuration?

7. Why is Ignite 2.x's Compute Grid (MapReduce, Service Grid) not yet available in 3.x? What alternatives exist?

8. Compare how transactions work in 2.x (PESSIMISTIC/OPTIMISTIC with isolation levels) vs. 3.x (strictly serializable). Which is simpler for developers?

9. Can you migrate directly from Ignite 2.x to 3.x? What are the steps required?

10. Given a new project that needs SQL-heavy workloads with strong consistency, which version would you choose and why? What if the project also needs MapReduce?

---

## Answers

1. **Cache-first (2.x):** Data is defined by Java classes and accessed via Cache API (put/get). SQL is an optional layer on top. Applications think in terms of caches and keys. **Schema-first (3.x):** Data structure is defined via SQL DDL before any data is stored. Applications can then access data via Table API, KeyValueView, or SQL equally. This makes 3.x more natural for teams coming from RDBMS backgrounds and enables language-agnostic schema management.

2. **RAFT** provides formally proven consistency guarantees: leader election, log replication, and safety properties. It ensures linearizable reads and writes, automatic leader election on failure, and built-in split-brain protection. The 2.x custom protocol lacked these formal guarantees and was harder to reason about under network partitions. RAFT is also an industry-standard algorithm used by etcd, CockroachDB, and TiKV.

3. **aimem:** Pure in-memory, volatile storage - lowest latency, no durability. Use for caching, sessions, temp data that fits in RAM. **aipersist:** Memory-first with disk persistence - balanced latency and durability. Use for general OLTP workloads. **RocksDB:** LSM-tree disk-based - handles datasets larger than RAM, write-optimized. Use for write-heavy workloads or large datasets. Ignite 2.x uses a single B+ tree page-based engine for all caches, so you cannot choose per-table.

4. **Ring topology (2.x):** Each node knows its "next" neighbor. Messages travel around the ring sequentially. Failure detected by neighbor. Message propagation is O(n) - slow with many nodes. Ring must be rebuilt on topology changes. **SWIM gossip (3.x):** Randomized probing - each node periodically probes random peers. Failure detection converges in O(log n) time. More resilient to cascading failures. SWIM is more scalable because information spreads exponentially rather than linearly.

5. In 2.x, **thick clients** join the cluster (full topology awareness, compute, peer class loading) but add cluster overhead. **Thin clients** are lightweight but lack compute, continuous queries, and initially transactions. Developers had to choose and accept trade-offs. In 3.x, the **unified client** provides all features via a single protocol. Clients never join the cluster (no overhead), and all languages get identical capabilities. This simplifies architecture and reduces operational complexity.

6. **Distribution Zones** are Ignite 3.x's way of controlling data placement at a higher level. A zone defines replicas count, partition count, and which storage profile to use. Multiple tables can share a zone. This replaces Ignite 2.x's per-cache backup count and affinity function configuration. Zones also support tiered storage (hot/warm/cold) by assigning different zones to tables with different access patterns, and the COLOCATE BY clause replaces the @AffinityKeyMapped annotation.

7. Ignite 3.x is a ground-up rewrite prioritizing core data management (storage, SQL, transactions, RAFT) first. Compute features are being added incrementally. Basic job execution and colocated compute exist, but full MapReduce (ComputeTask), Service Grid, peer class loading, and failover SPI are not yet available. **Alternatives:** Use SQL aggregations (SUM, AVG, GROUP BY) for data processing; use application-level parallelism with colocated compute for key-based processing; or stay on Ignite 2.x if compute is critical.

8. **Ignite 2.x:** Developers must choose between PESSIMISTIC/OPTIMISTIC concurrency and READ_COMMITTED/REPEATABLE_READ/SERIALIZABLE isolation. Wrong choices can lead to deadlocks or inconsistencies. SQL and KV cannot easily mix in one transaction. **Ignite 3.x:** Always strictly serializable, no concurrency/isolation choice needed. SQL and KV operations can mix in the same transaction. This is much simpler and matches RDBMS behavior, reducing bugs from incorrect transaction configuration.

9. **No direct migration path.** Steps: (1) Set up a new Ignite 3.x cluster alongside 2.x; (2) Recreate schema using SQL DDL; (3) Migrate data via export/import or dual-write; (4) Update all application code (Cache API -> Table API, Ignition.start -> IgniteClient.builder, SqlFieldsQuery -> client.sql()); (5) Replace integrations (Spring XML -> HOCON, check availability of Spring Data, Hibernate, Kafka connectors); (6) Test thoroughly (different behavior under failures, different transaction semantics); (7) Cut over when ready. Ignite 3.1+ provides migration tools (DDL generator, data export/import).

10. For **SQL-heavy with strong consistency:** Ignite 3.x is the clear choice - SQL is first-class, transactions are strictly serializable by default, RAFT guarantees consistency, and schema is managed via DDL. However, if **MapReduce is also needed**, you face a trade-off: (a) Use 3.x and replace MapReduce with SQL aggregations or colocated compute where possible, or (b) Use 2.x if MapReduce is critical and SQL-first is nice-to-have. A third option: Run both - use 3.x for the SQL workload and 2.x for compute, with data synchronization between them.

---

## Cleanup

```bash
# Stop Ignite 3 Docker container
docker stop ignite3-node
docker rm ignite3-node

# Clean up project directories (optional)
rm -rf version-comparison/
```

---

## Next Steps

After completing this lab, you should:
- Understand when to choose Ignite 2.x vs 3.x for new projects
- Be able to evaluate migration effort for existing 2.x deployments
- Know the key API, configuration, and architecture differences
- Understand the compute grid and integration ecosystem gaps in 3.x
- Be prepared to make informed version selection decisions

## Additional Resources

- [Apache Ignite 3.0 Documentation](https://ignite.apache.org/docs/ignite3/latest/)
- [What's New in Apache Ignite 3.0](https://ignite.apache.org/blog/whats-new-in-apache-ignite-3-0.html)
- [Ignite 2.16 Release Notes](https://ignite.apache.org/releases/2.16.0/release_notes.html)
- [Getting to Know Apache Ignite 3](https://ignite.apache.org/blog/getting-to-know-apache-ignite-3.html)
- [RAFT Consensus Algorithm](https://raft.github.io/)
- [SWIM Protocol Paper](https://www.cs.cornell.edu/projects/Quicksilver/public_pdfs/SWIM.pdf)

## Completion

Once you can successfully:
- Set up and run both Ignite 2.16 and 3.x
- Explain discovery, lifecycle, and configuration differences
- Use both Cache API (2.x) and Table API (3.x)
- Understand transaction model and compute grid differences
- Compare data colocation, security, and monitoring approaches
- Make informed decisions about which version to use

You have completed Lab 13: Version Differences!
